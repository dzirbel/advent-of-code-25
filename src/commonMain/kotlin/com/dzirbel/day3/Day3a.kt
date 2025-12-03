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
                var first = 0
                var second = 0
                for ((index, battery) in batteries.withIndex()) {
                    if (battery > first && index != batteries.lastIndex) {
                        first = battery
                        second = 0
                    } else if (battery > second) {
                        second = battery
                    }
                }

                (first.toString() + second.toString()).toInt()
            }
    }

    println(joltage)
}
