package it.unibo.alchemist

import java.util.Random
import kotlin.math.sqrt
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.actions.AbstractMoveNode
import it.unibo.alchemist.model.positions.Euclidean2DPosition

data class AgentState(
    val x: Double,
    val y: Double,
    val vx: Double,
    val vy: Double
)

class MoveNode<T> (
    environment: Environment<T, Euclidean2DPosition>,
    node: Node<T>,
    val xVel: Double,
    val yVel: Double,
) : AbstractMoveNode<T, Euclidean2DPosition>(environment, node, false) {

    private val random = Random()
    private val sigmaSquared = 0.00035
    private val sigma = sqrt(sigmaSquared)
    private var newVx = xVel
    private var newVy = yVel

    override fun getNextPosition(): Euclidean2DPosition? {
        val currentPosition = environment.getPosition(node)
        return computeNextPosition(AgentState(currentPosition.x, currentPosition.y, newVx, newVy))
    }

    override fun cloneAction(
        p0: Node<T?>?,
        p1: Reaction<T?>?
    ): Action<T?>? {
        return MoveNode(environment, node, xVel, yVel)
    }

    private fun computeNextPosition(currentState: AgentState): Euclidean2DPosition {

        val ux = random.nextGaussian() * sigma
        val uy = random.nextGaussian() * sigma

        val newX = currentState.x + currentState.vx + (0.5 * ux)
        val newY = currentState.y + currentState.vy + (0.5 * uy)
        newVx = currentState.vx + ux
        newVy = currentState.vy + uy

        return Euclidean2DPosition(newX, newY)
    }

}
