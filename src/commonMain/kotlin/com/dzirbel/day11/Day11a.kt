package com.dzirbel.day11

import kotlin.io.path.Path
import kotlin.io.path.useLines

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day11.txt" }

    val graph: Map<String, List<String>> = Path(path).useLines { lines ->
        lines.associate { line ->
            val split = line.split(' ')
            split.first().removeSuffix(":") to split.drop(1)
        }
    }

    println(graph.dfs(current = "you", destination = "out"))
}

private fun Map<String, List<String>>.dfs(current: String, destination: String): Int {
    if (current == destination) return 1
    return getValue(current).sumOf { neighbor -> dfs(neighbor, destination) }
}
