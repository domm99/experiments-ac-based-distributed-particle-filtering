package it.unibo.collektive

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.collektive.aggregate.Field
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.neighboring
import it.unibo.collektive.aggregate.values
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.LocationSensor
import it.unibo.collektive.stdlib.collapse.reduce
import it.unibo.filtering.ParticleFilter
import it.unibo.filtering.Point
import it.unibo.filtering.div
import it.unibo.filtering.plus
import kotlin.collections.mutableListOf
import org.apache.commons.math3.random.RandomGenerator

/**
 * The entrypoint of the simulation performing local information filtering.
 */
fun Aggregate<Int>.informationFilterEntrypoint(
    collektiveDevice: CollektiveDevice<*>,
    env: EnvironmentVariables,
    position: LocationSensor,
) = with(collektiveDevice) {
    localFiltering(env, randomGenerator, position)
}

/**
 * Performs local filtering using a Particle Filter to estimate the position
 * of a target based on neighborhood information.
 *
 * @param env the environment variables to store estimation history
 * @param random the random generator for stochastic processes
 * @param position the location sensor providing target position and neighborhood data
 */
fun Aggregate<*>.localFiltering(env: EnvironmentVariables, random: RandomGenerator, position: LocationSensor) {
    evolve(ParticleFilter(250, 2.0, 100.0, random)) { filter ->
        val sampledParticles = filter.resample()
        val newParticles = filter.predictParticles(sampledParticles)
        filter.updateWeights(newParticles, averageNeighborhoodPoint(position))
        val estimation = filter.estimatePosition()
        val history = env.getOrDefault("Estimations", mutableListOf<Point>())
        history.add(estimation)
        env["Estimations"] = history
        filter
    }
}

/**
 * Calculates the average position of the neighborhood perception of the target position, given by the location sensor.
 *
 * @param position the location sensor that provides the target position and its neighborhood information
 * @return the average position of the neighboring points
 */
fun Aggregate<*>.averageNeighborhoodPoint(position: LocationSensor): Point {
    val targetPosition: Point = position.targetsPosition().first()
    val neighborsTargetPosition: Field<*, Point> = neighboring(targetPosition)
    val size = neighborsTargetPosition.all.size.toDouble()
    val averagePosition: Point = neighborsTargetPosition.all.values.reduce(Point::plus) / size
    return averagePosition
}
