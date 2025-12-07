package com.dzirbel.day5

import kotlin.io.path.Path
import kotlin.io.path.useLines

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day5.txt" }
    Path(path).useLines { lines ->
        val freshRanges = mutableListOf<Range>()

        for (line in lines) {
            if (line.isEmpty()) continue

            val split = line.split('-')

            when (split.size) {
                // line is a fresh range
                2 -> {
                    val range = Range(split[0].toLong(), split[1].toLong())
                    freshRanges.addAndConsolidate(range)
                }

                1 -> {} // no-op

                else -> error("malformed line $line")
            }
        }

        println(freshRanges.sumOf { it.end - it.start + 1 })
    }
}
