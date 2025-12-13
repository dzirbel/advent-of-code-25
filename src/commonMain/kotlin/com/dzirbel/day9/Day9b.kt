package com.dzirbel.day9

import kotlin.io.path.Path
import kotlin.io.path.useLines
import kotlin.math.abs

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day9.txt" }

    val points: List<Point> = Path(path).useLines { lines ->
        lines
            .map { line ->
                val (x, y) = line.split(',')
                x.toInt() to y.toInt()
            }
            .toList()
    }

    val lines = points.zipWithNext() + (points.last() to points.first())

    val rects: Sequence<Rect> = points
        .asSequence()
        .drop(1)
        .flatMapIndexed { index, a ->
            points.asSequence().take(index).map { b -> a to b }
        }

    val maxArea = rects
        .filter { rect ->
            lines.none { line -> rect.intersects(line) }
        }
        .maxOf { rect -> rect.size }

    println(maxArea)
}

private typealias Point = Pair<Int, Int>
private val Point.x get() = first
private val Point.y get() = second

private typealias Rect = Pair<Point, Point>

// whether the given line overlaps with the interior of this rectangle
private fun Rect.intersects(line: Pair<Point, Point>): Boolean {
    // +/-1 to get the "interior" points which are not on the boundary of the rectangle
    val left = minOf(first.x, second.x) + 1
    val right = maxOf(first.x, second.x) - 1
    val top = maxOf(first.y, second.y) - 1
    val bottom = minOf(first.y, second.y) + 1

    return if (line.first.x == line.second.x) {
        // vertical line
        val x = line.first.x
        val lineTop = maxOf(line.first.y, line.second.y)
        val lineBottom = minOf(line.first.y, line.second.y)
        val above = lineBottom >= top // true if the line is entirely above the rectangle
        val below = lineTop <= bottom // true if the line is entirely below the rectangle
        !above && !below && x in left..right
    } else if (line.first.y == line.second.y) {
        // horizontal line
        val y = line.first.y
        val lineLeft = minOf(line.first.x, line.second.x)
        val lineRight = maxOf(line.first.x, line.second.x)
        val toLeft = lineRight <= left // true if the line is entirely to the left of the rectangle
        val toRight = lineLeft >= right // true if the line is entirely to the right of the rectangle
        !toLeft && !toRight && y in bottom..top
    } else {
        error("$line is not a vertical or horizontal line")
    }
}

private val Rect.size: Long
    get() {
        val width = abs(first.x - second.x) + 1
        val height = abs(first.y - second.y) + 1
        return width.toLong() * height
    }
