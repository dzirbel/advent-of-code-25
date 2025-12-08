package com.dzirbel.day6

import kotlin.io.path.Path
import kotlin.io.path.useLines

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day6.txt" }
    Path(path).useLines { lines ->
        val linesList = lines.toList()
        val problems = mutableListOf<List<String>>()

        var index = 0
        while (true) {
            val endIndex = linesList.maxOf { line -> line.indexOf(" ", startIndex = index) }

            val problem = linesList.map { line -> line.substring(index, endIndex.takeIf { it >= 0 } ?: line.length) }
            problems.add(problem)

            if (endIndex < 0) break
            index = endIndex + 1
        }

        val grandTotal = problems.sumOf { problem ->
            val operands = problem.dropLast(1)
            val cols = operands.maxOf { it.length }

            val values = List(cols) { col ->
                operands
                    .joinToString(separator = "") { operand ->
                        operand.getOrNull(col)?.toString()?.ifBlank { null }.orEmpty()
                    }
                    .toLong()
            }
            when (val operation = problem.last().trim()) {
                "+" -> values.sum()
                "*" -> values.reduce(Long::times)
                else -> error("unknown operation: $operation")
            }
        }

        println(grandTotal)
    }
}
