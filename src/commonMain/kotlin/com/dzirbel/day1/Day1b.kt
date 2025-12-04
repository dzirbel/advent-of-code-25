package com.dzirbel.day1

import kotlin.io.path.Path
import kotlin.io.path.useLines
import kotlin.math.abs

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day1.txt" }
    val hits = Path(path).useLines { lines ->
        var dial = 50
        var hits = 0
        for (line in lines) {
            val match = checkNotNull(linePattern.matchEntire(line)) { "line $line is malformed" }
            val direction = match.groupValues[1]
            val turn = match.groupValues[2].toInt()

            val delta = when (direction) {
                "L" -> -turn
                "R" -> turn
                else -> error("invalid direction")
            }

            val prev = dial

            dial += delta

            if (dial == 0) hits++
            if (prev != 0 && dial < 0) hits++
            hits += (abs(dial) / 100).coerceAtLeast(0)

            dial = ((dial % 100) + 100) % 100 // floorMod 100
        }

        hits
    }

    println(hits)
}

private val linePattern = """([L|R])(\d+)""".toRegex()
