package it.unibo.alchemist.model.deployments

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Position
import org.apache.commons.math3.random.RandomGenerator

class Gaussian<P: Position<P>>(
    environment: Environment<Any, P>,
    randomGenerator: RandomGenerator,
    nodes: Int,
    val centerX: Double,
    val centerY: Double,
    val stddev: Double,
) : AbstractRandomDeployment<P>(environment, randomGenerator, nodes) {

    override fun indexToPosition(i: Int): P {
        val gaussianX = randomGenerator.nextGaussian()
        val gaussianY = randomGenerator.nextGaussian()
        val finalX = centerX + (gaussianX * stddev)
        val finalY = centerY + (gaussianY * stddev)
        return makePosition(finalX, finalY)
    }

}
