package com.dzirbel.day8

import kotlin.io.path.Path
import kotlin.io.path.useLines

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day8.txt" }

    val points: List<IndexedValue<Point>> = Path(path).useLines { lines ->
        lines
            .map { line ->
                val (x, y, z) = line.split(',')
                Point(x = x.toInt(), y = y.toInt(), z = z.toInt())
            }
            .withIndex()
            .toList()
    }

    val closest = points
        .flatMap { pointA ->
            if (pointA.index == 0) {
                emptyList()
            } else {
                List(pointA.index - 1) { bIndex -> pointA to points[bIndex] }
            }
        }
        .asSequence()
        .sortedBy { (a, b) -> a.value.distanceSquared(b.value) }
        .map { (a, b) -> Pair(a.index, b.index) }

    val circuits = MutableList(points.size) { index -> mutableSetOf(index) }

    for ((a, b) in closest) {
        val circuitA: MutableSet<Int> = circuits.first { a in it }
        if (b in circuitA) continue

        val circuitB: MutableSet<Int> = circuits.first { b in it }

        circuits.remove(circuitA)
        circuitB.addAll(circuitA)

        if (circuits.size == 1) {
            println(points[a].value.x * points[b].value.x)
            break
        }
    }
}
