package it.unibo.collektive

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.LocationSensor
import it.unibo.collektive.stdlib.accumulation.convergeCast
import it.unibo.collektive.stdlib.consensus.globalElection

/**
 * todo.
 */
fun Aggregate<Int>.convergeHistoryEntrypoint(
    collektiveDevice: CollektiveDevice<*>,
    env: EnvironmentVariables,
    position: LocationSensor,
): List<*> = with(collektiveDevice) {
    val leader = globalElection(localId)
    val history = convergeSourceHistory(
        sink = leader == localId,
        startingData = position.targetsPosition().first(),
        historySize = 5,
    )
    env["source"] = leader == localId
    env["history"] = history
    return history
    // localFiltering(env, randomGenerator, position)
}

/**
 * Manages the evolution of shared data in an aggregate system, accumulating historical data from all nodes.
 * The [sink] accumulate all the lastest data of the system.
 * Allows limiting the size of the history.
 * @param sink A boolean indicating whether the current device acts as a sink for the data.
 * @param startingData The initial data to start the accumulation process.
 * @param historySize The maximum size of the historical data to retain. If null, the history is unbounded.
 * @return A list of accumulated historical data after convergence and evaluation.
 */
fun <SharingData> Aggregate<Int>.convergeAllHistory(
    sink: Boolean,
    startingData: SharingData,
    historySize: Int? = null, // null means unbounded, keep all history -- careful with memory!
): List<SharingData> = evolve(listOf(startingData)) { data ->
    convergeCast(
        local = data,
        sink = sink,
        accumulateData = { acc, value ->
            when {
                historySize == null -> (acc + value)
                else -> (acc + value).takeLast(historySize)
            }.also { println(it) }
        },
    )
}

/**
 * Manages the evolution of shared data in an aggregate system, accumulating historical data from all nodes.
 * The [sink] knows the entire history, while the other nodes just accumulate their own data.
 * Allows limiting the size of the history.
 * @param sink A boolean indicating whether the current device acts as a sink for the data.
 * @param startingData The initial data to start the accumulation process.
 * @param historySize The maximum size of the historical data to retain. If null, the history is unbounded.
 * @return A list of accumulated historical data after convergence and evaluation.
 */
inline fun <reified SharingData> Aggregate<Int>.convergeHistory(
    sink: Boolean,
    startingData: SharingData,
    historySize: Int? = null, // null means unbounded, keep all history -- careful with memory!
): List<NeighborhoodHistory<SharingData>> =
    evolve(listOf(NeighborhoodHistory(startingData))) { previousData ->
        val systemSnapshot = convergeCast(
            local = listOf(startingData),
            sink = sink,
            accumulateData = { acc, value ->
                acc + value
            },
        )
        (previousData + NeighborhoodHistory(systemSnapshot)).takeLast(historySize ?: Int.MAX_VALUE)
    }

/**
 * Manages the evolution of shared data in an aggregate system, accumulating historical data only at sink nodes.
 * Allows limiting the size of the history.
 * @param sink A boolean indicating whether the current device acts as a sink for the data.
 * @param startingData The initial data to start the accumulation process.
 * @param historySize The maximum size of the historical data to retain. If null, the history is unbounded.
 * @return A list of accumulated historical data after convergence and evaluation.
 */
inline fun <reified SharingData> Aggregate<Int>.convergeSourceHistory(
    sink: Boolean,
    startingData: SharingData,
    historySize: Int? = null, // null means unbounded, keep all history -- careful with memory!
): List<NeighborhoodHistory<SharingData>> =
    evolve(listOf(NeighborhoodHistory(startingData))) { previousData ->
        val systemSnapshot = convergeCast(
            local = listOf(startingData),
            sink = sink,
            accumulateData = { acc, value ->
                acc + value
            },
        )
        when {
            sink -> (previousData + NeighborhoodHistory(systemSnapshot)).takeLast(historySize ?: Int.MAX_VALUE)
            else -> previousData
        }
    }

data class NeighborhoodHistory<SharingData>(val neighborsData: List<SharingData> = emptyList()) {

    constructor(data: SharingData) : this(listOf(data))

    override fun toString(): String =
        "History of #neighborhood=${neighborsData.size} { ${neighborsData.joinToString(", ")} }"
}
