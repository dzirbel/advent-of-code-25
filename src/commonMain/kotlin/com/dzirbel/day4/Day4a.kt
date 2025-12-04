package com.dzirbel.day4

import kotlin.io.path.Path
import kotlin.io.path.useLines

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day4.txt" }
    val accessible = Path(path).useLines { lines ->
        val linesList = lines.filter { it.isNotBlank() }.toList()
        val rows = linesList.size
        val cols = linesList.first().length
        val adjacent = Array(rows) { IntArray(cols) }

        for ((row, line) in linesList.withIndex()) {
            for ((col, char) in line.withIndex()) {
                when (char) {
                    '.' -> adjacent[row][col] = -1

                    '@' -> {
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

                    else -> error("unexpected character $char")
                }
            }
        }

        adjacent.sumOf { row -> row.count { it in 0..<4 } }
    }

    println(accessible)
}
