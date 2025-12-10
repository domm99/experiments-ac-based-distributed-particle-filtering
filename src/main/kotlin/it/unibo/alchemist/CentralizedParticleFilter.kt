package it.unibo.alchemist

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import it.unibo.filtering.ParticleFilter
import it.unibo.filtering.Point
import org.apache.commons.math3.random.RandomGenerator

class CentralizedParticleFilter<T>(
    val environment: Environment<T, Euclidean2DPosition>,
    val random: RandomGenerator,
    node: Node<T>,
    val sideLength: Double,
    val numberOfParticles: Int,
    val maxInitialSpeed: Double = 2.0,
) : AbstractAction<T>(node) {
    private val estimations: MutableList<Point> = mutableListOf()
    private val filter = ParticleFilter(numberOfParticles, maxInitialSpeed, sideLength, random)

    private fun measurePosition(stdDev: Double = 0.5): Point {
        val movingNode = environment.nodes.first { it.contains(SimpleMolecule("Movable")) }
        val truePosition = environment.getPosition(movingNode)
        val newX = truePosition.x + (random.nextGaussian() * stdDev)
        val newY = truePosition.y + (random.nextGaussian() * stdDev)
        return Point(newX, newY)
    }

    override fun execute() {
        val position = measurePosition()
        val sampledParticles = filter.resample()
        val newParticles = filter.predictParticles(sampledParticles)
        filter.updateWeights(newParticles, position)
        val estimation = filter.estimatePosition()
        estimations.add(estimation)
        node.setConcentration(SimpleMolecule("Estimations"), estimations as T)
    }

    override fun getContext(): Context = Context.LOCAL

    override fun cloneAction(node: Node<T?>?, reaction: Reaction<T?>?): Action<T?>? = CentralizedParticleFilter(
        environment,
        random,
        node!! as Node<T>,
        sideLength,
        numberOfParticles,
        maxInitialSpeed,
    )
}
