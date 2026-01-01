package com.dzirbel.day12

import kotlin.io.path.Path
import kotlin.io.path.useLines

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day12.txt" }

    val problems = Path(path).useLines { lines ->
        val split = lines.splitAround("")

        val shapes = split.dropLast(1)
            .mapIndexed { index, block ->
                check(block[0] == "$index:")
                val matrix = block
                    .drop(1)
                    .map { line ->
                        BooleanArray(line.length) { i ->
                            when (val char = line[i]) {
                                '#' -> true
                                '.' -> false
                                else -> error("unrecognized shape character $char")
                            }
                        }
                    }
                    .toTypedArray()
                Shape(matrix = matrix)
            }

        val problemRegex = """(\d+)x(\d+): ([\d ]+)""".toRegex()
        val problems = split.last()
            .map { line ->
                val match = requireNotNull(problemRegex.matchEntire(line)) { "invalid problem: $line" }
                val width = match.groups[1]!!.value.toInt()
                val height = match.groups[2]!!.value.toInt()
                val shapeCounts = match.groups.last()!!.value.split(' ')
                check(shapeCounts.size == shapes.size) { "expected ${shapes.size} shapes; was $shapeCounts" }
                val shapes = shapeCounts
                    .mapIndexed { index, group -> shapes[index] to group.toInt() }
                    .toMap()

                Problem(width = width, height = height, shapes = shapes)
            }

        problems
    }

    println(problems.count { it.canSolve() })
}

private class Shape(val matrix: Array<BooleanArray>) {
    // number of cells occupied in the matrix
    val occupied = matrix.sumOf { row -> row.count { cell -> cell } }
}

private class Problem(val width: Int, val height: Int, val shapes: Map<Shape, Int>) {
    fun canSolve(): Boolean {
        // 8)
        return (width * height) >= shapes.toList().sumOf { (shape, count) -> shape.occupied * count }
    }
}

private fun <T> Sequence<T>.splitAround(special: T): List<List<T>> {
    val iterator = iterator()
    return buildList {
        while (iterator.hasNext()) {
            val block = buildList {
                while (iterator.hasNext()) {
                    val item = iterator.next()
                    if (item == special) break else add(item)
                }
            }

            add(block)
        }
    }
}
