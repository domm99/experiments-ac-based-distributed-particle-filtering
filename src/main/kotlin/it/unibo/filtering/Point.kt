package it.unibo.filtering

data class Point(val x: Double, val y: Double)

data class Particle(var x: Double, var y: Double, var vx: Double, var vy: Double, var weight: Double = 1.0)

operator fun Point.plus(other: Point): Point = Point(this.x + other.x, this.y + other.y)

operator fun Point.div(scalar: Double): Point = Point(this.x / scalar, this.y / scalar)
