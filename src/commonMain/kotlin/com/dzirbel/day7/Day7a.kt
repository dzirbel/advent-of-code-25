package com.dzirbel.day7

import kotlin.io.path.Path
import kotlin.io.path.readLines

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day7.txt" }

    val lines = Path(path).readLines()
    var beams = BooleanArray(lines.first().length)
    var splits = 0

    for (line in lines) {
        val nextBeams = beams.copyOf()
        for ((index, char) in line.withIndex()) {
            when (char) {
                '.' -> {}
                'S' -> nextBeams[index] = true
                '^' -> {
                    if (beams[index]) {
                        nextBeams[index - 1] = true
                        nextBeams[index] = false
                        nextBeams[index + 1] = true
                        splits++
                    }
                }
                else -> error("unexpected character $char")
            }
        }

        beams = nextBeams
    }

    println(splits)
}
