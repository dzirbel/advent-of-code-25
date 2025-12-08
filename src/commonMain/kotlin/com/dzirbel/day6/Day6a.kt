package com.dzirbel.day6

import kotlin.io.path.Path
import kotlin.io.path.useLines

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day6.txt" }
    Path(path).useLines { lines ->
        val problems = mutableListOf<MutableList<String>>()
        val regex = """\s+""".toRegex()
        for (line in lines) {
            val tokens = line.trim().split(regex)
            for ((index, token) in tokens.withIndex()) {
                if (index in problems.indices) {
                    problems[index].add(token)
                } else {
                    problems.add(mutableListOf(token))
                }
            }
        }

        val grandTotal = problems.sumOf { problem ->
            val values = problem.dropLast(1).map { it.toLong() }
            when (val operation = problem.last()) {
                "+" -> values.sum()
                "*" -> values.reduce(Long::times)
                else -> error("unknown operation $operation")
            }
        }

        println(grandTotal)
    }
}
