package it.unibo.alchemist

import java.util.Random
import kotlin.math.sqrt
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.actions.AbstractMoveNode
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.math.max

class MoveNode<T> (
    environment: Environment<T, Euclidean2DPosition>,
    node: Node<T>,
    val xVel: Double,
    val yVel: Double,
    val sideLength: Double
) : AbstractMoveNode<T, Euclidean2DPosition>(environment, node, true) {

    private val random = Random()
    private val sigmaSquared = 0.00035
    private val sigma = sqrt(sigmaSquared)
    private var currentVx = xVel
    private var currentVy = yVel
    private val friction = 0.97

    override fun getNextPosition(): Euclidean2DPosition? {
        val currentPosition = environment.getPosition(node)
        storePosition(currentPosition)
        var newPosition = computeNextPosition(currentPosition)
        newPosition = checkBoundaries(newPosition)
        return newPosition
    }

    private fun storePosition(currentPosition: Euclidean2DPosition) {
        val node =
            environment.nodes.first { it.contains(SimpleMolecule("Movable")) }
        node
            .setConcentration(SimpleMolecule("PositionX"), currentPosition.x as T?)

        node
            .setConcentration(SimpleMolecule("PositionY"), currentPosition.y as T?)
    }

    override fun cloneAction(
        p0: Node<T?>?,
        p1: Reaction<T?>?
    ): Action<T?>? {
        return MoveNode(environment, node, xVel, yVel, sideLength)
    }

    private fun computeNextPosition(currentPosition: Euclidean2DPosition): Euclidean2DPosition {

        val ux = random.nextGaussian() * sigma
        val uy = random.nextGaussian() * sigma

        val newX = currentPosition.x + currentVx + (0.5 * ux)
        val newY = currentPosition.y + currentVy + (0.5 * uy)
        currentVx = (currentVx * friction) + ux
        currentVy = (currentVy * friction) + uy

        return Euclidean2DPosition(newX, newY)
    }

    private fun checkBoundaries(position: Euclidean2DPosition): Euclidean2DPosition {
        var newX = position.x
        var newY = position.y

        if (newX < 0) {
            newX = 0.0
            currentVx = -currentVx
        } else if (newX > sideLength) {
            newX = sideLength
            currentVx = -currentVx
        }

        if (newY < 0) {
            newY = 0.0
            currentVy = -currentVy
        } else if (newY > sideLength) {
            newY = sideLength
            currentVy = -currentVy
        }
        return Euclidean2DPosition(newX, newY)
    }

}
