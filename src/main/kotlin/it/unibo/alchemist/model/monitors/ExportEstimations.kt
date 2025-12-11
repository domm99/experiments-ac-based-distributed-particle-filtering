package it.unibo.alchemist.model.monitors

import it.unibo.alchemist.boundary.OutputMonitor
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import it.unibo.filtering.Point
import java.io.File
import java.util.Locale

class ExportEstimations<T> : OutputMonitor<T, Euclidean2DPosition> {

    override fun finished(environment: Environment<T?, Euclidean2DPosition>, time: Time, step: Long) {
        val filters = environment.nodes.filter { it.contains(SimpleMolecule("Filter")) }

        filters.forEach { filter ->
            val estimations = filter.getConcentration(SimpleMolecule("Estimations")) as MutableList<Point>
            val id = filter.id
            exportToCsv("data/estimations_node-$id.csv", estimations)
        }

    }

    fun exportToCsv(filename: String, history: List<Point>) {
        File(filename).printWriter().use { out ->
            // Header
            out.println("estimatedX,estimatedY")

            // Data
            history.forEach { step ->
                println("${step.x},${step.y}")
                val line = String.format(
                    Locale.US,
                    "%.4f,%.4f",
                    step.x,
                    step.y,
                )
                out.println(line)
            }
        }
    }
}
