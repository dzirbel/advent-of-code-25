package com.dzirbel.day10

import kotlin.io.path.Path
import kotlin.io.path.useLines

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day10.txt" }

    val total = Path(path).useLines { lines ->
        lines
            .map { line ->
                val split = line.split(' ')

                val desiredLights = split.first().drop(1).dropLast(1)

                // drop first (indicator lights) and last (joltage)
                val buttons = split.subList(fromIndex = 1, toIndex = split.lastIndex)

                LightMachine(
                    desiredLights = BooleanArray(desiredLights.length) { i ->
                        when (val char = desiredLights[i]) {
                            '#' -> true
                            '.' -> false
                            else -> error("unexpected indicator light state $char")
                        }
                    },
                    buttons = buttons.map { button ->
                        val array = BooleanArray(desiredLights.length)

                        for (index in button.trimStart('(').trimEnd(')').split(',')) {
                            array[index.toInt()] = true
                        }

                        array
                    },
                )
            }
            .sumOf { machine -> machine.solve() }
    }

    println(total)
}

private class LightMachine(
    val desiredLights: BooleanArray,
    val buttons: List<BooleanArray>,
) {
    fun solve(lights: BooleanArray = BooleanArray(desiredLights.size), i: Int = 0): Int {
        val button = buttons.getOrNull(i) ?: return buttons.size + 1
        val newLights = BooleanArray(lights.size) { j -> lights[j] xor button[j] }
        if (newLights.contentEquals(desiredLights)) return 1

        return minOf(
            1 + solve(lights = newLights, i = i + 1),
            solve(lights = lights, i = i + 1),
        )
    }
}
