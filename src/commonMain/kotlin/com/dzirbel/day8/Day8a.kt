package com.dzirbel.day8

import kotlin.io.path.Path
import kotlin.io.path.useLines

private const val ITERATIONS = 1_000
private const val N_LARGEST = 3

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
        .take(ITERATIONS)
        .map { (a, b) -> Pair(a.index, b.index) }

    val circuits = mutableListOf<MutableSet<Int>>()

    for ((a, b) in closest) {
        val circuitA: MutableSet<Int>? = circuits.find { a in it }
        val circuitB: MutableSet<Int>? = circuits.find { b in it }

        when {
            // neither box is in a circuit; create a new one for them
            circuitA == null && circuitB == null -> circuits.add(mutableSetOf(a, b))

            // both boxes already in a circuit; merge them
            circuitA != null && circuitB != null ->
                if (circuitA != circuitB) {
                    check(circuits.remove(circuitA))
                    circuitB.addAll(circuitA)
                }

            // one box is already in a circuit; the other is not
            circuitA != null -> check(circuitA.add(b))
            circuitB != null -> check(circuitB.add(a))
            else -> error("impossible")
        }
    }

    val largestCircuits = circuits.asSequence().map { it.size }.sortedDescending().take(N_LARGEST)
    println(largestCircuits.reduce(Int::times))
}
