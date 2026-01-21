package it.unibo.collektive

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.collektive.aggregate.Field
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.neighboring
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.LocationSensor
import it.unibo.filtering.Particle
import it.unibo.filtering.ParticleFilter
import it.unibo.filtering.Point
import it.unibo.filtering.div
import it.unibo.filtering.plus
import kotlin.math.hypot
import kotlin.math.log10
import org.apache.commons.math3.random.RandomGenerator

const val p0 = 100
const val pathLoss = 2
const val measureStdDev = 1.0

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
context(random: RandomGenerator, position: LocationSensor, env: EnvironmentVariables)
fun Aggregate<*>.localFiltering(
    estimationsHistory: List<Point>,
    numberOfParticles: Int,
    maxInitialSpeed: Double,
    sideLength: Double,
): List<Point> = evolving(ParticleFilter(numberOfParticles, maxInitialSpeed, sideLength, random)) { filter ->
    val previous = env.getOrDefault("Particles", mutableListOf<List<Particle>>())
    env["NumberOfParticles"] = numberOfParticles
    previous.add(filter.getAll())
    env["Particles"] = previous
    val sampledParticles = filter.resample()
    val newParticles = filter.predictParticles(sampledParticles)
    val selfPosition = position.selfPosition()
    val targetPosition = position.targetsPosition().first()
    val distance = hypot(targetPosition.x - selfPosition.x, targetPosition.y - selfPosition.y)
    val measure = p0 - 10 * pathLoss * log10(distance) + random.nextGaussian() * measureStdDev

    val neighborsInfo =
        neighboring(selfPosition to measure)
            .all.list
            .mapNotNull { it.value }

    println("${neighborsInfo.size} neighbors info")

    filter.updateWeights(newParticles, neighborsInfo)
    val estimation = filter.estimatePosition()
    val history = estimationsHistory + estimation
    filter.yielding { history }
}
