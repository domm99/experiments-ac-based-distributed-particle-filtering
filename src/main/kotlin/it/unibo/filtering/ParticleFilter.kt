package it.unibo.filtering

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.util.RandomGenerators.nextDouble
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.log10
import org.apache.commons.math3.random.RandomGenerator

data class ParticleMemory(
    val filter: ParticleFilter,
    val particlesHistory: MutableList<ParticleHistory> = mutableListOf(),
)

data class ParticleHistory(val history: List<Particle>)

/**
 * A simple Particle Filter implementation for 2D position tracking.
 * @param numberOfParticles The number of particles to use in the filter.
 * @param maxInitialSpeed The maximum initial speed of the particles.
 * @param sideLength The side length of the square area where particles are initialized.
 * @param random An instance of a random number generator.
 * @param measurementStdDev The standard deviation of the measurement noise.
 */
class ParticleFilter(
    val numberOfParticles: Int = 250,
    val maxInitialSpeed: Double = 2.0,
    sideLength: Double = 100.0,
    val random: RandomGenerator,
    val measurementStdDev: Double = 1.0,
) {

    private var particles: List<Particle> = initParticles(sideLength)

    private fun initParticles(sideLength: Double): List<Particle> = List(numberOfParticles) {
        val x = random.nextDouble(0.0, sideLength)
        val y = random.nextDouble(0.0, sideLength)
        val vx = random.nextDouble(-maxInitialSpeed, maxInitialSpeed)
        val vy = random.nextDouble(-maxInitialSpeed, maxInitialSpeed)
        Particle(x, y, vx, vy, 1.0 / numberOfParticles)
    }

    fun getAll(): List<Particle> = particles

    // todo use local delta time per mettere il delta tempo
    fun predict(stdDev: Double = 1.0, dt: Double = 1.0): List<Particle> {
       particles = particles.map { p ->
           // estimate previous position assuming stored velocity was used to get current position
           val xPrev = p.x - p.vx * dt
           val yPrev = p.y - p.vy * dt
           // velocity estimate v_t = (x_t - x_{t-1}) / dt
           val vEstX = (p.x - xPrev) / dt
           val vEstY = (p.y - yPrev) / dt
           // blend estimated velocity with stored velocity for stability
           val alpha = 0.7
           val baseVx = alpha * vEstX + (1 - alpha) * p.vx
           val baseVy = alpha * vEstY + (1 - alpha) * p.vy
           // process noise
           val predVx = baseVx + random.nextGaussian() * stdDev
           val predVy = baseVy + random.nextGaussian() * stdDev
           p.copy(
               x = p.x + predVx * dt + random.nextGaussian() * stdDev,
               y = p.y + predVy * dt + random.nextGaussian() * stdDev,
               vx = predVx,
               vy = predVy
           )
       }
        return particles
    }

    fun updateWithMeasurements(measurements: List<Point>) {
        var maxLogW = Double.NEGATIVE_INFINITY
        if (measurements.isEmpty()) return
        particles.forEach { p ->
            var newWeight = 0.0
            measurements.forEach { m ->
                val d = hypot(p.x - m.x, p.y - m.y)
                val likelihood = -0.5 * (d * d) / (measurementStdDev * measurementStdDev)
                newWeight += likelihood
            }
            p.weight = newWeight
            if (newWeight > maxLogW) maxLogW = newWeight
        }
        var totalWeight = 0.0
        particles.forEach { particle ->
            particle.weight = exp(particle.weight - maxLogW)
            totalWeight += particle.weight
        }
        // Weights normalization
        if (totalWeight > 0.0) {
            for (p in particles) {
                p.weight /= totalWeight
            }
        } else {
            val uniformWeight = 1.0 / numberOfParticles
            for (p in particles) {
                p.weight = uniformWeight
            }
        }
    }

    fun resample() {
        val cumulative = DoubleArray(numberOfParticles)
        var acc = 0.0
        for (i in particles.indices) {
            acc += particles[i].weight
            cumulative[i] = acc
        }
        val newParticles = ArrayList<Particle>(numberOfParticles)
        val step = 1.0 / numberOfParticles
        var r = random.nextDouble(0.0, step)
        var i = 0

        repeat(numberOfParticles) {
            while (r > cumulative[i]) i++
            val p = particles[i]
            newParticles.add(
                Particle(p.x, p.y, p.vx, p.vy, step)
            )
            r += step
        }
        particles = newParticles
    }

    fun estimatePosition(): Point {
        var x = 0.0
        var y = 0.0
        particles.forEach { p ->
            x += p.x * p.weight
            y += p.y * p.weight
        }
        return Point(x, y)
    }
}
