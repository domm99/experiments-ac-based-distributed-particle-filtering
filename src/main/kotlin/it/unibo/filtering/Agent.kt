package it.unibo.filtering

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.stdlib.accumulation.convergeCast
import it.unibo.collektive.stdlib.accumulation.convergeSum
import it.unibo.collektive.stdlib.consensus.globalElection
import it.unibo.collektive.stdlib.spreading.gossipMin
import it.unibo.collektive.stdlib.spreading.gradientCast
import kotlin.math.pow
import kotlin.math.sqrt


fun euclideanDistance(pos1: Euclidean2DPosition, pos2: Euclidean2DPosition): Double {
    return sqrt((pos1.x - pos2.x).pow(2.0) + Math.pow((pos1.y - pos2.y), 2.0))
}

/**
 * The entrypoint of the simulation running a gradient, considering the device with id 0 as the source.
 */
fun Aggregate<Int>.gradientEntrypoint(env: EnvironmentVariables, dev: CollektiveDevice<*>): Unit {

    val targetNodePosition: Euclidean2DPosition = dev.getTargetPosition()
    val localPosition: Euclidean2DPosition = dev.coordinates()

    val leaderId = globalElection(
        strength = -euclideanDistance(localPosition, targetNodePosition),
    )

    // TODO - (DPF) Computation of particles

    // TODO - gathering of particles from nodes into the leader
    //convergeCast()

    // TODO - (DFP) the leader aggregates the particles to create new model

    // TODO - share back the new model to the nodes
    //gradientCast()


    env["WhoIsLeader"] = leaderId
    env["isLeader"] = leaderId == localId
}



