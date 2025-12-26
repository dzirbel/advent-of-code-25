package com.dzirbel.day10

import kotlin.io.path.Path
import kotlin.io.path.useLines
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.measureTimedValue

fun main(args: Array<String>) {
    val sampleExpected = mapOf(
        1 to 10,
        2 to 12,
        3 to 11,
    )

    run("inputs/day10sample.txt", expected = sampleExpected)
    println("sample OK")
    println()

    val expected = mapOf(
        1 to 32,
        2 to 119,
        3 to 60,
        4 to 215,
        5 to 53,
        6 to 5,
        7 to 81,
        8 to 112,
        9 to 33,
        10 to 23,
        11 to 96,
        12 to 221,
        13 to 66,
        14 to 217,
        15 to 22,
        16 to 81,
        17 to 62,
        18 to 272,
        19 to 109,
        20 to 101,
    )

    val path = args.getOrElse(0) { "inputs/day10.txt" }
    val total = run(path, expected = expected)
    println()
    println(total)
}

private fun run(path: String, expected: Map<Int, Int>? = null): Int {
    return Path(path).useLines { lines ->
        lines
            .map { line ->
                val split = line.split(' ')

                val desiredLights = split.first().drop(1).dropLast(1)

                // drop first (indicator lights) and last (joltage)
                val buttons = split.subList(fromIndex = 1, toIndex = split.lastIndex)

                val joltage = split.last().trimStart('{').trimEnd('}')

                JoltageMachine(
                    buttons = buttons
                        .map { button ->
                            val array = BooleanArray(desiredLights.length)

                            for (index in button.trimStart('(').trimEnd(')').split(',')) {
                                array[index.toInt()] = true
                            }

                            array
                        }
                        // sort by button size descending
                        .sortedByDescending { button ->
                            button.count { it }
                        },
                    target = joltage.split(',').map { it.toInt() }.toIntArray(),
                )
            }
            .withIndex()
            .sumOf { (index, machine) ->
                val lineNumber = index + 1
                print(lineNumber.toString().padEnd(length = 3))

                val greedy = measureTimedValue { machine.greedy() }
                print(" [greedy: ${greedy.value.toString().padEnd(length = 4)} in ${greedy.duration.inWholeMilliseconds}ms]".padEnd(length = 30))

                val result = measureTimedValue { machine.dfs(upperBound = greedy.value) }

                println(" >> ${result.value.toString().padEnd(length = 4)} in ${result.duration.inWholeMilliseconds}ms")

                val expectedValue = expected?.get(lineNumber)
                if (expectedValue != null) {
                    check(result.value == expectedValue) {
                        "expected $expectedValue for $lineNumber; got ${result.value}"
                    }
                }

                result.value
            }
    }
}

private class IntRef(var value: Int)

private class JoltageMachine(
    val buttons: List<BooleanArray>,
    val target: IntArray,
) {
    val numButtons = buttons.size
    val numCounters = target.size

    // map from counter index to the button indices that increment it
    val counterToButtons: List<List<Int>> = List(numCounters) { counter ->
        (0 until numButtons).filter { button -> buttons[button][counter] }
    }

    val buttonToCounters: List<List<Int>> = List(numButtons) { button ->
        (0 until numCounters).filter { counter -> buttons[button][counter] }
    }

    // number of counters each button affects
    val buttonSizes: List<Int> = buttons.map { button -> button.count { it } }

    private val memo: MutableMap<ResidualKey, Int> = mutableMapOf()

    private class ResidualKey(private val data: IntArray) {
        private val hash = data.contentHashCode()
        override fun hashCode() = hash
        override fun equals(other: Any?) = other is ResidualKey && other.data.contentEquals(data)
    }

    fun dfs(upperBound: Int? = null): Int {
        memo.clear()
        val bestCost = IntRef(upperBound ?: Int.MAX_VALUE)
        dfs(bestCost, JoltageMachineState(this))
        check(bestCost.value != Int.MAX_VALUE) { "did not find any solutions" }
        return bestCost.value
    }

    fun greedy(): Int? {
        return sequence {
            yield(greedyDeterministic(maxResidualReduction = true, maxPresses = true))
            yield(greedyDeterministic(maxResidualReduction = true, maxPresses = false))
            yield(greedyDeterministic(maxResidualReduction = false, maxPresses = true))
            yield(greedyDeterministic(maxResidualReduction = false, maxPresses = false))

            // try a large number of times, so hard cases have many chances for a greedy solution
            repeat(1_000) { count ->
                yield(greedyRandom(Random(seed = count), maxPresses = true))
                yield(greedyRandom(Random(seed = count), maxPresses = false))
            }
        }
            .filterNotNull()
            .take(5) // only take the first few, so easy cases don't run too many greedy iterations
            .minOrNull()
    }

    // simple deterministic, greedy attempt to find an upper bound with a couple configuration options to try different
    // common routes
    private fun greedyDeterministic(maxResidualReduction: Boolean, maxPresses: Boolean): Int? {
        var state = JoltageMachineState(this)

        while (!state.isTarget()) {
            val button = state.pressableButtons()
                .ifEmpty { return null }
                .maxBy { button ->
                    if (maxResidualReduction) {
                        buttonToCounters[button].sumOf { counter -> state.residual[counter] }
                    } else {
                        numButtons - button
                    }
                }
            val presses = if (maxPresses) (state.maxPresses[button] - 1).coerceAtLeast(1) else 1
            state = state.press(button, presses) ?: return null
        }

        return state.cost
    }

    // simple randomized, greedy attempt to find an upper bound on the number of presses:
    // - pick a random button, among those that we can still press
    // - pick a random number of times to press it between 1 and the maximum number of allowed presses
    // repeat until success or failure
    private fun greedyRandom(random: Random, maxPresses: Boolean): Int? {
        var state = JoltageMachineState(this)

        while (!state.isTarget()) {
            val counter = state.hardestCounters().random(random)
            val button = counterToButtons[counter]
                .filter { state.canPress(it) }
                .randomOrNull(random) ?: return null
            // actually use maxPresses - 1 to preserve some flexibility
            val presses = if (maxPresses) (state.maxPresses[button] - 1).coerceAtLeast(1) else 1
            state = state.press(button, presses) ?: return null
        }

        return state.cost
    }

    private fun dfs(bestCost: IntRef, state: JoltageMachineState) {
        if (state.cost >= bestCost.value) return

        if (state.isTarget()) {
            bestCost.value = state.cost
            return
        }

        val key = ResidualKey(state.residual)
        memo[key]?.let { memoCost -> if (memoCost <= state.cost) return }
        memo[key] = state.cost

        if (state.cost + state.lowerBound() >= bestCost.value) return

        val hardestCounter = state.hardestCounter()
        val candidateButtons = counterToButtons[hardestCounter]

        for (button in candidateButtons) {
            val maxPresses = state.maxPresses[button]
            if (maxPresses == 0) continue

            // start with max presses
            state.press(button, maxPresses)?.let { dfs(bestCost = bestCost, it) }
            if (maxPresses == 1) continue

            // then try a single press
            state.press(button, 1)?.let { dfs(bestCost = bestCost, it) }

            // then the rest in descending order
            for (presses in (maxPresses - 1) downTo 2) {
                state.press(button, presses)?.let { dfs(bestCost = bestCost, it) }
            }
        }
    }
}

private class JoltageMachineState(
    val machine: JoltageMachine,
    val residual: IntArray = machine.target.clone(), // starts at target and counts down to 0
    val cost: Int = 0,
) {
    // maxPresses[button] is the maximum number of times button can be pressed without exceeding the target
    val maxPresses: IntArray by lazy {
        IntArray(machine.numButtons) { button ->
            machine.buttonToCounters[button].minOf { counter -> residual[counter] }
        }
    }

    fun isTarget() = residual.all { it == 0 }

    fun canPress(button: Int): Boolean = maxPresses[button] > 0

    fun pressableButtons(): List<Int> {
        return (0 until machine.numButtons).filter { canPress(it) }
    }

    fun press(button: Int, times: Int, applyForced: Boolean = true): JoltageMachineState? {
        if (times == 0) return this

        val next = residual.clone()
        for (counter in machine.buttonToCounters[button]) {
            val v = next[counter] - times
            if (v < 0) return null
            next[counter] = v
        }

        return JoltageMachineState(machine = machine, residual = next, cost = cost + times)
            .let { if (applyForced) it.forcedMoves() else it }
    }

    // lower bound on the number of presses remaining
    fun lowerBound(): Int {
        // buttons are sorted by size in advance, so the first we find is always the largest
        val maxButtonSize = machine.buttonSizes[(0 until machine.numButtons).indexOfFirst { canPress(it) }]

        val boundBySum = (residual.sum() + maxButtonSize - 1) / maxButtonSize

        return max(residual.max(), boundBySum)
    }

    private fun forcedMoves(): JoltageMachineState? {
        for (counter in 0 until machine.numCounters) {
            val r = residual[counter]
            if (r == 0) continue

            val buttons = machine.counterToButtons[counter]

            // total allowed presses among all the buttons that affect this counter
            val maxAllowedPresses = buttons.sumOf { button -> maxPresses[button] }

            // if it's less than the residual, this state is not satisfiable
            if (maxAllowedPresses < r) return null

            // if it's equal to the residual, or there's only one button which can affect it, press the button(s)
            // until it's satisfied
            if (maxAllowedPresses == r || buttons.count { button -> canPress(button) } == 1) {
                var currentState = this
                for (button in buttons) {
                    currentState = currentState.press(button, currentState.maxPresses[button], applyForced = false)
                        ?: return null
                }
                if (currentState.residual[counter] != 0) return null
                return currentState.forcedMoves()
            }
        }

        return this
    }

    // finds the counter(s) affected by the fewest number of pressable buttons
    fun hardestCounters(): List<Int> {
        val remainingCounters = (0 until machine.numCounters).filter { residual[it] > 0 }
        val min = remainingCounters.minOf { counter ->
            machine.counterToButtons[counter].count { button -> canPress(button) }
        }

        return remainingCounters
            .filter { counter ->
                machine.counterToButtons[counter].count { button -> canPress(button) } == min
            }
    }

    // finds one of the counter(s) affected by the fewest number of pressable buttons
    fun hardestCounter(): Int {
        return (0 until machine.numCounters).minBy { counter ->
            machine.counterToButtons[counter].count { button -> canPress(button) }
                .let { count -> if (count == 0) Int.MAX_VALUE else count }
        }
    }
}
