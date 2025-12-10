package it.unibo.collektive.alchemist.device.sensors.impl

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.collektive.alchemist.device.sensors.LocationSensor
import it.unibo.filtering.Point
import org.apache.commons.math3.random.RandomGenerator

class LocationSensorProperty<T : Any, P : Position<P>>(
    private val environment: Environment<T, P>,
    private val random: RandomGenerator,
    override val node: Node<T>,
    private val stdDev: Double = 0.5,
) : LocationSensor,
    NodeProperty<T> {
    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> =
        LocationSensorProperty(environment, random, node, stdDev)

    override fun coordinates(): Point {
        val position = environment.getPosition(node).coordinates
        return Point(position[0], position[1])
    }

    override fun surroundings(): List<Point> = environment.getNeighborhood(node).map { node ->
        environment.getPosition(node).coordinates.let { Point(it[0], it[1]) }
    }

    override fun targetsPosition(): List<Point> = environment.nodes
        .filter { node ->
            node.contains(SimpleMolecule("Movable"))
        }.map { target ->
            environment.getPosition(target)
        }.map { position ->
            val newX = position.coordinates[0] + (random.nextGaussian() * stdDev)
            val newY = position.coordinates[1] + (random.nextGaussian() * stdDev)
            Point(newX, newY)
        }
}
