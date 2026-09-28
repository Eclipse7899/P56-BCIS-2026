package com.p56.safereach.model

data class LatLong(
    val latitude: Double,
    val longitude: Double,
)

sealed interface Shape {
    data class Circle(val center: LatLong, val radius: Double) : Shape
    data class Polygon(val points: List<LatLong>) : Shape
}
