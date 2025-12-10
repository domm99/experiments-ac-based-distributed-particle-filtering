package it.unibo.alchemist

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.math.exp
import kotlin.math.hypot


data class Particle(
    var x: Double,
    var y: Double,
    var vx: Double,
    var vy: Double,
    var weight: Double = 1.0
)

data class Point(val x: Double, val y: Double)

class CentralizedParticleFilter<T>(
    val environment: Environment<T, Euclidean2DPosition>,
    node: Node<T>,
    val sideLength: Double,
    val numberOfParticles: Int
): AbstractAction<T>(node) {

    private val random = java.util.Random()
    private val maxInitialSpeed = 2.0 // TODO - check
    private val estimations: MutableList<Point> = mutableListOf()

    private var particles: MutableList<Particle> = MutableList(numberOfParticles) {
        val x = random.nextDouble(0.0, sideLength)
        val y = random.nextDouble(0.0, sideLength)
        val vx = random.nextDouble(-maxInitialSpeed, maxInitialSpeed)
        val vy = random.nextDouble(-maxInitialSpeed, maxInitialSpeed)

        Particle(x, y, vx, vy, 1.0 / numberOfParticles)
    }

    private fun measurePosition(stdDev: Double = 0.5): Euclidean2DPosition {
        val movingNode = environment.nodes.first { it.contains(SimpleMolecule("Movable")) }
        val truePosition = environment.getPosition(movingNode)
        val newX = truePosition.x + (random.nextGaussian() * stdDev)
        val newY = truePosition.y + (random.nextGaussian() * stdDev)
        return Euclidean2DPosition(newX, newY)
    }

    override fun execute() {

        val position = measurePosition()

        val sampledParticles = resample()
        val newParticles = predictParticles(sampledParticles)
        particles = updateWeights(newParticles, position).toMutableList()
        estimatePosition()
        node.setConcentration(SimpleMolecule("Estimations"), estimations as T)
    }

    private fun predictParticles(
        sampledParticles: List<Particle>,
        stdDev: Double = 1.0,
        dt: Double = 1.0
    ): List<Particle> {

        val newParticles = ArrayList<Particle>(sampledParticles.size)

        for (p in sampledParticles) {
            val noiseX = random.nextGaussian() * stdDev
            val noiseY = random.nextGaussian() * stdDev
            val noiseVx = random.nextGaussian() * stdDev
            val noiseVy = random.nextGaussian() * stdDev

            val newX = p.x + (p.vx * dt) + noiseX
            val newY = p.y + (p.vy * dt) + noiseY
            val newVx = p.vx + noiseVx
            val newVy = p.vy + noiseVy

            newParticles.add(Particle(newX, newY, newVx, newVy, p.weight))
        }

        return newParticles
    }

    private fun updateWeights(
        newParticles: List<Particle>,
        measurement: Euclidean2DPosition,
        measurementStdDev: Double = 0.5
        ): List<Particle> {
        var totalWeight = 0.0

        for (p in newParticles) {
            val dist = hypot(p.x - measurement.x, p.y - measurement.y)

            // P(z|x) ~ exp(-dist^2 / (2 * sigma^2))
            val likelihood = exp(-0.5 * (dist * dist) / (measurementStdDev * measurementStdDev))

            p.weight = likelihood
            totalWeight += likelihood
        }

        // Weights normalization
        if (totalWeight > 0.0) {
            for (p in newParticles) {
                p.weight /= totalWeight
            }
        } else {
            val uniformWeight = 1.0 / numberOfParticles
            for (p in newParticles) {
                p.weight = uniformWeight
            }
        }

        return newParticles
    }

    fun resample(): List<Particle> {
        val newParticles = ArrayList<Particle>(numberOfParticles)
        val totalWeight = particles.sumOf { it.weight }

        if (totalWeight == 0.0) return particles

        val cumulativeWeights = DoubleArray(numberOfParticles)
        var currentSum = 0.0

        for (i in 0 until numberOfParticles) {
            currentSum += particles[i].weight
            cumulativeWeights[i] = currentSum / totalWeight
        }

        cumulativeWeights[numberOfParticles - 1] = 1.0

        val resetWeight = 1.0 / numberOfParticles

        repeat(numberOfParticles) {
            val r = random.nextDouble()

            var selectedIndex = 0
            for (j in 0 until numberOfParticles) {
                if (r <= cumulativeWeights[j]) {
                    selectedIndex = j
                    break
                }
            }

            val p = particles[selectedIndex]
            newParticles.add(Particle(
                x = p.x,
                y = p.y,
                vx = p.vx,
                vy = p.vy,
                weight = resetWeight
            ))
        }

        return newParticles
    }

    private fun estimatePosition() {
        var x = 0.0
        var y = 0.0
        for (p in particles) {
            x += p.x * p.weight
            y += p.y * p.weight
        }
        estimations.add(Point(x, y))
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
