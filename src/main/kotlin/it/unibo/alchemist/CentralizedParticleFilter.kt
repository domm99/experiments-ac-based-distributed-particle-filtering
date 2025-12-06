package it.unibo.alchemist

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.positions.Euclidean2DPosition


data class Point(val x: Double, val y: Double)

data class Particle(
    val position: Point,
    var weight: Double = 1.0
)

class CentralizedParticleFilter<T>(
    val environment: Environment<T, Euclidean2DPosition>,
    node: Node<T>,
    val sideLength: Double,
    val numberOfParticles: Int
): AbstractAction<T>(node) {

    private val random = java.util.Random()

    private var particles: MutableList<Particle> = MutableList(numberOfParticles) {
        val x = random.nextDouble() * sideLength
        val y = random.nextDouble() * sideLength
        Particle(Point(x, y), 1.0 / numberOfParticles)
    }

    override fun execute() {
        println("Inside CentralizedParticleFilter")
        val node = environment.nodes.first { it.contains(SimpleMolecule("Movable")) }
        val pos = environment.getPosition(node)
        println("Position of moving node: $pos")
    }

    override fun getContext(): Context? {
        return Context.LOCAL
    }

    override fun cloneAction(
        node: Node<T?>?,
        reaction: Reaction<T?>?
    ): Action<T?>? {
        return CentralizedParticleFilter(environment, node!! as Node<T>, sideLength, numberOfParticles)
    }

}
