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

    println(graph.dfs(current = "svr", destination = "out"))
}

private fun Map<String, List<String>>.dfs(
    current: String,
    destination: String,
    dac: Boolean = false,
    fft: Boolean = false,
    memo: MutableMap<NodeKey, Long> = mutableMapOf(),
): Long {
    if (current == destination) return if (dac && fft) 1 else 0

    val key = NodeKey(current, dac, fft)
    memo[key]?.let { return it }

    val dac = dac || current == "dac"
    val fft = fft || current == "fft"
    return getValue(current).sumOf { neighbor ->
        dfs(
            current = neighbor,
            destination = destination,
            dac = dac,
            fft = fft,
            memo = memo,
        )
    }
        .also { memo[key] = it }
}

private data class NodeKey(val node: String, val dac: Boolean, val fft: Boolean)
