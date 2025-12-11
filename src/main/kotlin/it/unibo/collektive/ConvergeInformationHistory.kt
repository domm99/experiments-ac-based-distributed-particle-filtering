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
): List<*> = with(collektiveDevice){
    val leader = globalElection(localId)
    val history = convergeHistory(
        sink = leader == localId,
        startingData = position.targetsPosition().first(),
        historySize = 5,
        evaluateData = { data -> data }
    )
    env["source"] = leader == localId
    env["history"] = history
    return history
    // localFiltering(env, randomGenerator, position)
}

/**
 * Manages the evolution of shared data in an aggregate system, accumulating historical data.
 * Allows for optional evaluation of the data and limiting the size of the history.
 *
 * @param sink A boolean indicating whether the current device acts as a sink for the data.
 * @param startingData The initial data to start the accumulation process.
 * @param historySize The maximum size of the historical data to retain. If null, the history is unbounded.
 * @param evaluateData A function to transform or evaluate the data at each step. Defaults to the identity function.
 * @return A list of accumulated historical data after convergence and evaluation.
 */
fun <SharingData> Aggregate<Int>.convergeHistory(
    sink: Boolean,
    startingData: SharingData,
    historySize: Int? = null, // null means unbounded, keep all history -- careful with memory!
    evaluateData: (SharingData) -> SharingData = { it }, // default to identity
): List<SharingData> {
    return evolve(listOf(evaluateData(startingData))) { data ->
        convergeCast(
            local = data,
            sink = sink,
            accumulateData = { acc, value ->
                val evaluated = value.map { evaluateData(it) }
                when {
                    historySize == null -> (acc + evaluated)
                    else -> (acc + evaluated).takeLast(historySize)
                }
            }
        )
    }
}
