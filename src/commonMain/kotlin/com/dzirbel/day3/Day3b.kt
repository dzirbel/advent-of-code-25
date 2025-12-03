package com.dzirbel.day3

import kotlin.io.path.Path
import kotlin.io.path.useLines

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day3.txt" }
    val joltage = Path(path).useLines { lines ->
        lines
            .map { line ->
                line.map { char -> char.toString().toInt() }
            }
            .sumOf { batteries ->
                var enabled = mutableListOf<Int>()
                for ((index, battery) in batteries.withIndex()) {
                    val remaining = batteries.lastIndex - index
                    var used = false
                    for ((ei, e) in enabled.withIndex()) {
                        // after replacing the battery at index ei, we'll have (ei+1) elements
                        // OK to replace if we have at least DIGITS with (ei+1) and the remaining batteries
                        val canReplace = (ei + 1) + remaining >= DIGITS
                        if (!canReplace) continue

                        if (battery > e) {
                            // found an improvement in the existing sequence; replace everything beyond it
                            enabled = enabled.take(ei).toMutableList()
                            enabled.add(battery)
                            used = true
                            break
                        }
                    }

                    // no improvement, but we can append
                    if (!used && enabled.size < DIGITS) {
                        enabled.add(battery)
                    }
                }

                enabled.joinToString(separator = "").toLong()
            }
    }

    println(joltage)
}

private const val DIGITS = 12
