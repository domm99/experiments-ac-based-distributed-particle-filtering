package it.unibo.filtering

import it.unibo.alchemist.util.RandomGenerators.nextDouble
import kotlin.math.exp
import kotlin.math.hypot
import org.apache.commons.math3.random.RandomGenerator

class ParticleFilter(
    val numberOfParticles: Int = 250,
    val maxInitialSpeed: Double = 2.0,
    sideLength: Double = 100.0,
    val random: RandomGenerator,
    val measurementStdDev: Double = 1.0,
) {

    var particles: List<Particle> = initParticles(sideLength)

    private fun initParticles(sideLength: Double): List<Particle> = List(numberOfParticles) {
        val x = random.nextDouble(0.0, sideLength)
        val y = random.nextDouble(0.0, sideLength)
        val vx = random.nextDouble(-maxInitialSpeed, maxInitialSpeed)
        val vy = random.nextDouble(-maxInitialSpeed, maxInitialSpeed)
        Particle(x, y, vx, vy, 1.0 / numberOfParticles)
    }

    fun predictParticles(sampledParticles: List<Particle>, stdDev: Double = 1.0, dt: Double = 1.0): List<Particle> {
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

    fun updateWeights(newParticles: List<Particle>, measurement: Point) {
        var totalWeight = 0.0

        newParticles.forEach { p ->
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
        particles = newParticles
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
            newParticles.add(
                Particle(
                    x = p.x,
                    y = p.y,
                    vx = p.vx,
                    vy = p.vy,
                    weight = resetWeight,
                ),
            )
        }

        return newParticles
    }

    fun estimatePosition(): Point {
        var x = 0.0
        var y = 0.0
        for (p in particles) {
            x += p.x * p.weight
            y += p.y * p.weight
        }
        return Point(x, y)
    }
}
