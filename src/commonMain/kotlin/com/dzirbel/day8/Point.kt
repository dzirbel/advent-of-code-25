package com.dzirbel.day8

import kotlin.math.abs

data class Point(
    val x: Int,
    val y: Int,
    val z: Int,
) {
    fun distanceSquared(other: Point): Long {
        val dx = abs(x - other.x).toLong()
        val dy = abs(y - other.y).toLong()
        val dz = abs(z - other.z).toLong()
        return dx * dx + dy * dy + dz * dz
    }
}
