package it.unibo.collektive

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.collektive.aggregate.Field
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.neighboring
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.LocationSensor
import it.unibo.filtering.ParticleFilter
import it.unibo.filtering.Point
import it.unibo.filtering.div
import it.unibo.filtering.plus
import org.apache.commons.math3.random.RandomGenerator

/**
 * The entrypoint of the simulation performing local information filtering.
 */
fun Aggregate<Int>.informationFilterEntrypoint(
    collektiveDevice: CollektiveDevice<*>,
    env: EnvironmentVariables,
    position: LocationSensor,
) = context(env, collektiveDevice.randomGenerator, position) {
    val estimations = env.getOrDefault("Estimations", listOf<Point>())
    localFiltering(estimations, env["NumberOfParticles"], env["MaxInitialSpeed"], env["SideLength"]).also { history ->
        env["Estimations"] = history
    }
}

/**
 * Performs local filtering using a Particle Filter to estimate the position
 * of a target based on neighborhood information.
 *
 * @param random the random generator for stochastic processes
 * @param position the location sensor providing target position and neighborhood data
 */
context(random: RandomGenerator, position: LocationSensor)
fun Aggregate<*>.localFiltering(
    estimationsHistory: List<Point>,
    numberOfParticles: Int,
    maxInitialSpeed: Double,
    sideLength: Double,
): List<Point> = evolving(ParticleFilter(numberOfParticles, maxInitialSpeed, sideLength, random)) { filter ->
    val sampledParticles = filter.resample()
    val newParticles = filter.predictParticles(sampledParticles)
    filter.updateWeights(newParticles, averageNeighborhoodPoint())
    val estimation = filter.estimatePosition()
    val history = estimationsHistory + estimation
    filter.yielding { history }
}

/**
 * Calculates the average position of the neighborhood perception of the target position, given by the location sensor.
 *
 * @param position the location sensor that provides the target position and its neighborhood information
 * @return the average position of the neighboring points
 */
context(position: LocationSensor)
fun Aggregate<*>.averageNeighborhoodPoint(): Point? {
    val targetPosition: Point? = position.targetsPosition().firstOrNull()
    val neighborsTargetPosition: Field<*, Point?> = neighboring(targetPosition)
    val neighborsNonNullPoint = neighborsTargetPosition.all.list.mapNotNull { it.value }
    return when {
        neighborsNonNullPoint.isEmpty() -> null
        else -> neighborsNonNullPoint.reduce(Point::plus) / neighborsNonNullPoint.size.toDouble()
    }
}
