package it.unibo.filtering

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables

/**
 * The entrypoint of the simulation running a gradient, considering the device with id 0 as the source.
 */
fun Aggregate<Int>.gradientEntrypoint(env: EnvironmentVariables): Unit {
    env["mid"] = localId
}


