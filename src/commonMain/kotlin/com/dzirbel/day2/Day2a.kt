package com.dzirbel.day2

import kotlin.io.path.Path
import kotlin.io.path.readText

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day2.txt" }
    val result = Path(path)
        .readText()
        .splitToSequence(',')
        .flatMap { range ->
            val (start, end) = range.trim().split('-')
            start.toLong()..end.toLong()
        }
        .filter { id ->
            val idString = id.toString()
            if (idString.length % 2 == 0) {
                val mid = idString.length / 2
                idString.take(mid) == idString.takeLast(mid)
            } else {
                false
            }
        }
        .sum()

    println(result)
}
