package it.unibo.filtering

import it.unibo.alchemist.util.RandomGenerators.nextDouble
import kotlin.math.exp
import kotlin.math.hypot
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

    fun predict(stdDev: Double = 1.0, dt: Double = 1.0) {
        particles = particles.map { p ->
            val nx = random.nextGaussian() * stdDev
            val ny = random.nextGaussian() * stdDev
            val nvx = random.nextGaussian() * stdDev
            val nvy = random.nextGaussian() * stdDev
            p.copy(
                x = p.x + p.vx * dt + nx,
                y = p.y + p.vy * dt + ny,
                vx = p.vx + nvx,
                vy = p.vy + nvy
            )
        }
    }

    fun updateWithMeasurements(measurements: List<Point>) {
        if (measurements.isEmpty()) return

        particles.forEach { p ->
            val logLikelihood = measurements.sumOf { z ->
                val d = hypot(p.x - z.x, p.y - z.y)
                -0.5 * (d * d) / (measurementStdDev * measurementStdDev)
            } / measurements.size

            p.weight *= exp(logLikelihood)
        }
        normalize()
    }

    private fun normalize() {
        val sum = particles.sumOf { it.weight }
        if (sum > 0.0) {
            particles = particles.map { it.copy(weight = it.weight / sum) }
        } else {
            val uniform = 1.0 / numberOfParticles
            particles = particles.map { it.copy(weight = uniform) }
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
