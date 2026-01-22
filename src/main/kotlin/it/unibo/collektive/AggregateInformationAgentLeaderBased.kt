package it.unibo.collektive

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.collektive.aggregate.Field
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.neighboring
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.LocationSensor
import it.unibo.collektive.stdlib.consensus.boundedElection
import it.unibo.collektive.stdlib.consensus.globalElection
import it.unibo.filtering.ParticleFilter
import it.unibo.filtering.ParticleHistory
import it.unibo.filtering.ParticleMemory
import it.unibo.filtering.Point
import it.unibo.filtering.div
import it.unibo.filtering.distanceTo
import it.unibo.filtering.plus
import kotlin.math.exp
import org.apache.commons.math3.random.RandomGenerator

/**
 * The entrypoint of the simulation performing local information filtering.
 */
fun Aggregate<Int>.informationFilterEntrypointLeaderBased(
    collektiveDevice: CollektiveDevice<*>,
    env: EnvironmentVariables,
    position: LocationSensor,
) = context(env, collektiveDevice.randomGenerator, position) {
    val sideLength = env["SideLength"] as Int
    val dist = collektiveDevice.environment.distanceFromNetworkCentroid(position.coordinates())
    val weight = centralityWeight(dist, sideLength / 2.0) // the highest, the closest to the center
    env["weight"] = weight to localId
    val isLeader = boundedElection(strength = weight, bound = sideLength) == localId
    env["isLeader"] = isLeader
    val numberOfParticles = env["NumberOfParticles"] as Int
    evolve(listOf<Point>()) { estimationsHistory ->
        localFilterings(estimationsHistory, numberOfParticles, env["MaxInitialSpeed"], sideLength.toDouble())
    }.also { history ->
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
fun Aggregate<*>.localFilterings(
    estimationsHistory: List<Point>,
    numberOfParticles: Int,
    maxInitialSpeed: Double,
    sideLength: Double,
): List<Point> = evolving(ParticleMemory(ParticleFilter(numberOfParticles, maxInitialSpeed, sideLength, random))) { memory ->
    memory.particlesHistory.add(ParticleHistory(memory.filter.getAll()))
    env["Particles"] = memory.particlesHistory
    val sampledParticles = memory.filter.resample()
    val newParticles = memory.filter.predictParticles(sampledParticles)
    memory.filter.updateWeights(newParticles, aaaa())
    val estimation = memory.filter.estimatePosition()
    val history = estimationsHistory + estimation
    memory.yielding { history }
}

/**
 * Calculates the average position of the neighborhood perception of the target position, given by the location sensor.
 *
 * @param position the location sensor that provides the target position and its neighborhood information
 * @return the average position of the neighboring points
 */
context(position: LocationSensor)
fun Aggregate<*>.aaaa(): Point? {
    val targetPosition: Point? = position.targetsPosition().firstOrNull()
    val neighborsTargetPosition: Field<*, Point?> = neighboring(targetPosition)
    val neighborsNonNullPoint = neighborsTargetPosition.all.list.mapNotNull { it.value }
    return when {
        neighborsNonNullPoint.isEmpty() -> null
        else -> neighborsNonNullPoint.reduce(Point::plus) / neighborsNonNullPoint.size.toDouble()
    }
}

fun <T, P: Position<P>> Environment<T, P>.distanceFromNetworkCentroid(position: Point): Double {
    val filtersNode = this.nodes.filter { it.contains(SimpleMolecule("Filter")) }
    val sum = filtersNode.fold(0.0 to 0.0) { acc, next ->
        val nextNodePos = this.getPosition(next).coordinates // Add 10 to avoid negative positions .map { it + 10 }
        acc.first + nextNodePos[0] to acc.second + nextNodePos[1]
    }
    val filtersCount = filtersNode.size
    val center = Point(sum.first / filtersCount, sum.second / filtersCount)
    return center.distanceTo(position) // the smallest, the closest
}

fun centralityWeight(
    distanceFromCentroid: Double,
    sigma: Double,
): Double = exp(-(distanceFromCentroid * distanceFromCentroid) / (2 * sigma * sigma))
