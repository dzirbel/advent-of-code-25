package com.dzirbel.day4

import kotlin.io.path.Path
import kotlin.io.path.useLines

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day4.txt" }
    val accessible = Path(path).useLines { lines ->
        val linesList = lines.filter { it.isNotBlank() }.toList()
        val rows = linesList.size
        val cols = linesList.first().length
        var initial = 0
        val adjacent = Array(rows) { row ->
            IntArray(cols) { col ->
                when (val char = linesList[row][col]) {
                    '.' -> -1
                    '@' -> {
                        initial++
                        0
                    }
                    else -> error("unexpected character $char")
                }
            }
        }

        while (true) {
            for ((row, line) in adjacent.withIndex()) {
                for ((col, count) in line.withIndex()) {
                    if (count == -1) continue

                    val minRow = (row - 1).coerceAtLeast(0)
                    val maxRow = (row + 1).coerceAtMost(rows - 1)
                    val minCol = (col - 1).coerceAtLeast(0)
                    val maxCol = (col + 1).coerceAtMost(cols - 1)
                    for (x in minRow..maxRow) {
                        for (y in minCol..maxCol) {
                            if (x == row && y == col) continue
                            if (adjacent[x][y] == -1) continue
                            adjacent[x][y]++
                        }
                    }
                }
            }

            var removed = false
            var remaining = 0
            for ((row, line) in adjacent.withIndex()) {
                for ((col, count) in line.withIndex()) {
                    if (count == -1) continue

                    if (count < 4) {
                        adjacent[row][col] = -1
                        removed = true
                    } else {
                        remaining++
                    }
                }
            }

            if (!removed) return@useLines initial - remaining

            for ((row, line) in adjacent.withIndex()) {
                for ((col, count) in line.withIndex()) {
                    if (count != -1) adjacent[row][col] = 0
                }
            }
        }
    }

    println(accessible)
}
