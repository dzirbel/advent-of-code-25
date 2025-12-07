package com.dzirbel.day5

import kotlin.io.path.Path
import kotlin.io.path.useLines

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day5.txt" }
    Path(path).useLines { lines ->
        val freshRanges = mutableListOf<Range>()
        var freshCount = 0

        for (line in lines) {
            if (line.isEmpty()) continue

            val split = line.split('-')

            when (split.size) {
                // line is a fresh range
                2 -> {
                    val range = Range(split[0].toLong(), split[1].toLong())
                    freshRanges.addAndConsolidate(range)
                }

                // line is an ID to query
                1 -> {
                    val id = split[0].toLong()
                    val rangeIndex = freshRanges.binarySearch(Range(id, id))
                    val fresh = if (rangeIndex >= 0) {
                        // exact hit on the range start
                        true
                    } else {
                        // rangeIndex = (-insertion point - 1) -> insertionPoint = -(rangeIndex + 1)
                        val prevRangeIndex = ((-rangeIndex - 1) - 1).coerceAtLeast(0)
                        id in freshRanges[prevRangeIndex]
                    }

                    if (fresh) {
                        freshCount++
                    }
                }

                else -> error("malformed line $line")
            }
        }

        println(freshCount)
    }
}
