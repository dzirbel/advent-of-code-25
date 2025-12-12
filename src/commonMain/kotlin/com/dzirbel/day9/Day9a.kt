package com.dzirbel.day9

import kotlin.io.path.Path
import kotlin.io.path.useLines
import kotlin.math.abs

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day9.txt" }

    val points: List<Pair<Long, Long>> = Path(path).useLines { lines ->
        lines
            .map { line ->
                val (x, y) = line.split(',')
                x.toLong() to y.toLong()
            }
            .toList()
    }

    val maxArea = points.withIndex().drop(1).maxOf { (index, a) ->
        points.take(index).maxOf { b ->
            val width = abs(a.first - b.first) + 1
            val height = abs(a.second - b.second) + 1
            width * height
        }
    }

    println(maxArea)
}
