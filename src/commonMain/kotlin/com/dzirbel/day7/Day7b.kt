package com.dzirbel.day7

import kotlin.io.path.Path
import kotlin.io.path.readLines

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day7.txt" }

    val lines = Path(path).readLines()
    val memo = Array(lines.size) { LongArray(lines[0].length) { -1 } }

    fun timelines(row: Int, beam: Int): Long {
        if (row !in lines.indices) return 1
        if (beam !in lines[row].indices) return 0
        memo[row][beam].takeIf { it >= 0 }?.let { return it }

        return when (val char = lines[row][beam]) {
            '.' -> timelines(row = row + 1, beam = beam)
            '^' -> timelines(row = row + 1, beam = beam - 1) + timelines(row = row + 1, beam = beam + 1)
            else -> error("unexpected character $char")
        }
            .also { memo[row][beam] = it }
    }

    println(timelines(row = 1, beam = lines[0].indexOf('S')))
}
