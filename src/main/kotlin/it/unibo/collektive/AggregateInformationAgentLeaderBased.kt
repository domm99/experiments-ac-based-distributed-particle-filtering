package it.unibo.collektive

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.LocationSensor
import it.unibo.collektive.stdlib.accumulation.convergeCast
import it.unibo.collektive.stdlib.consensus.boundedElection
import it.unibo.filtering.ParticleFilter
import it.unibo.filtering.ParticleMemory
import it.unibo.filtering.Point
import it.unibo.filtering.distanceTo
import kotlin.math.exp

/**
 * The entrypoint of the simulation performing local information filtering.
 */
fun Aggregate<Int>.informationFilterEntrypointLeaderBased(
    collektiveDevice: CollektiveDevice<*>,
    env: EnvironmentVariables,
    position: LocationSensor,
) = context(env, collektiveDevice.randomGenerator, position, collektiveDevice) {
    val sideLength = env["SideLength"] as Int
    val numberOfParticles = env["NumberOfParticles"] as Int
    val maxInitialSpeed = env["MaxInitialSpeed"] as Double
    val isLeader = isLeaderBasedOnLocation(sideLength).also { env["isLeader"] = it }
    env["sensing?"] = position.targetsPosition().firstOrNull()
    evolve(listOf<Point>()) { estimationsHistory ->
        sensorsExecution(isLeader, estimationsHistory, numberOfParticles, maxInitialSpeed, sideLength.toDouble())
    }.also { history ->
        env["Estimations"] = history
    }
}

context(device: CollektiveDevice<*>, position: LocationSensor)
fun Aggregate<Int>.sensorsExecution(
    isLeader: Boolean,
    estimationsHistory: List<Point>,
    numberOfParticles: Int,
    maxInitialSpeed: Double,
    sideLength: Double,
): List<Point> = evolving(ParticleMemory(ParticleFilter(numberOfParticles, maxInitialSpeed, sideLength, device.randomGenerator))) { memory ->
    val avgPoint = averageNeighborhoodPoint()
    val sampledParticles = memory.filter.resample()
    val predictedParticles = memory.filter.predictParticles(sampledParticles)//.take(max(1, ceil(numberOfParticles * 0.5).toInt())) // top 50%
    val convergedParticles = convergeCast(predictedParticles, isLeader) { list1, list2 -> list1 + list2 }
    val estimate = if (isLeader) {
        val limitParticles = convergedParticles.sortedByDescending { it.weight }.take(numberOfParticles)
        memory.filter.updateWeights(limitParticles, avgPoint)
        memory.filter.estimatePosition()
    } else {
        avgPoint
    }
    memory.yielding { if (estimate != null ) estimationsHistory + estimate else estimationsHistory}
}

context(device: CollektiveDevice<*>, position: LocationSensor)
fun Aggregate<Int>.isLeaderBasedOnLocation(bound: Int): Boolean  {
    val dist = device.environment.distanceFromNetworkCentroid(position.coordinates())
    val weight = centralityWeight(dist, bound / 2.0) // the highest, the closest to the center
    return boundedElection(strength = weight, bound = bound) == localId
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
