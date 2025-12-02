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
            (1..(idString.length / 2)).any { divisor ->
                idString.hasDuplicates(divisor)
            }
        }
        .sum()

    println(result)
}

private fun String.hasDuplicates(length: Int): Boolean {
    if (this.length % length != 0 || length * 2 > this.length) return false
    val substring = substring(startIndex = 0, endIndex = length)
    return (length..lastIndex step length).all { i ->
        substring(startIndex = i, endIndex = i + length) == substring
    }
}
