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
    private val maxInitialSpeed = 2.0
    private val estimations: MutableList<Point> = mutableListOf()

    private var particles: MutableList<Particle> = MutableList(numberOfParticles) {
        val x = random.nextDouble(0.0, sideLength)
        val y = random.nextDouble(0.0, sideLength)
        val vx = random.nextDouble(-maxInitialSpeed, maxInitialSpeed)
        val vy = random.nextDouble(-maxInitialSpeed, maxInitialSpeed)

        Particle(x, y, vx, vy, 1.0 / numberOfParticles)
    }

    override fun execute() {
        val movingNode = environment.nodes.first { it.contains(SimpleMolecule("Movable")) }
        val truePosition = environment.getPosition(movingNode)

        val dt = 1.0
        val measurementNoise = 1.0 // TODO - check dei noise
        val sensorNoise = 0.8
        val measuredX = truePosition.x + random.nextGaussian() * sensorNoise
        val measuredY = truePosition.y + random.nextGaussian() * sensorNoise

        predict(dt)
        update(measuredX, measuredY, measurementNoise)

        val estimation = getEstimate()
        estimations.add(Point(estimation.x, estimation.y))
        node.setConcentration(SimpleMolecule("Estimations"), estimations as T)
    }

    private fun predict(dt: Double) {
        val posNoise = 0.2
        val velNoise = 0.5

        for (p in particles) {
            p.x += (p.vx * dt) + (random.nextGaussian() * posNoise)
            p.y += (p.vy * dt) + (random.nextGaussian() * posNoise)
            p.vx += random.nextGaussian() * velNoise
            p.vy += random.nextGaussian() * velNoise
        }
    }

    private fun update(measuredX: Double, measuredY: Double, observationNoise: Double) {

        for (particle in particles) {
            val distance = hypot(
                particle.x - measuredX,
                particle.y - measuredY
            )
            val likelihood = exp(-(distance * distance) / (2 * observationNoise * observationNoise))
            particle.weight *= likelihood
        }

        // Weights normalization
        val sumOfWeights = particles.sumOf { it.weight }
        if (sumOfWeights > 0) {
            for (particle in particles) {
                particle.weight /= sumOfWeights
            }
        } else {
            // If all weights are equal to zero then assign uniform weights
            particles.forEach { it.weight = 1.0 / numberOfParticles }
        }

    }

    data class StateEstimate(val x: Double, val y: Double, val vx: Double, val vy: Double)

    // TODO - check
    fun getEstimate(): StateEstimate {
        var sumX = 0.0
        var sumY = 0.0
        var sumVx = 0.0
        var sumVy = 0.0

        for (p in particles) {
            sumX += p.x * p.weight
            sumY += p.y * p.weight
            sumVx += p.vx * p.weight
            sumVy += p.vy * p.weight
        }

        return StateEstimate(sumX, sumY, sumVx, sumVy)
    }

    fun resample() {
        val newParticles = mutableListOf<Particle>()
        val cumulativeWeights = DoubleArray(numberOfParticles)

        // Calcola la somma cumulativa dei pesi
        var cumulativeSum = 0.0
        for (i in 0 until numberOfParticles) {
            cumulativeSum += particles[i].weight
            cumulativeWeights[i] = cumulativeSum
        }

        // Algoritmo di Resampling a Ruota di Fortuna
        for (i in 0 until numberOfParticles) {
            val r = random.nextDouble() // Un numero casuale tra 0.0 e 1.0

            // Trova la particella corrispondente al valore casuale r
            for (j in 0 until numberOfParticles) {
                if (r < cumulativeWeights[j]) {
                    // Clona la particella selezionata, ripristinando il suo peso
                    newParticles.add(Particle(
                        particles[j].x,
                        particles[j].y,
                        particles[j].vx,
                        particles[j].vy ,
                        1.0 / numberOfParticles
                    ))
                    break
                }
            }
        }
        particles = newParticles
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
