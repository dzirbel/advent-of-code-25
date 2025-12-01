package com.dzirbel.day1

import kotlin.io.path.Path
import kotlin.io.path.useLines

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day1.txt" }
    val hits = Path(path).useLines { lines ->
        lines
            .map { line ->
                val match = checkNotNull(linePattern.matchEntire(line)) { "line $line is malformed" }
                val direction = match.groupValues[1]
                val turn = match.groupValues[2].toInt()

                when (direction) {
                    "L" -> -turn
                    "R" -> turn
                    else -> error("invalid direction")
                }
            }
            .scan(initial = 50) { dial, delta -> dial + delta }
            .count { it % 100 == 0 }
    }

    println(hits)
}

private val linePattern = """([L|R])(\d+)""".toRegex()
