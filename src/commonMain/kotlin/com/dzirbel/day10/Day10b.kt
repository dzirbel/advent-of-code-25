package com.dzirbel.day10

import kotlin.io.path.Path
import kotlin.io.path.useLines
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.measureTimedValue

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day10.txt" }

    check(run("inputs/day10sample.txt") == 33)
    println("sample OK")
    println()

    val total = run(path)
    println()
    println(total)
}

private fun run(path: String): Int {
    return Path(path).useLines { lines ->
        lines
            .map { line ->
                val split = line.split(' ')

                val desiredLights = split.first().drop(1).dropLast(1)

                // drop first (indicator lights) and last (joltage)
                val buttons = split.subList(fromIndex = 1, toIndex = split.lastIndex)

                val joltage = split.last().trimStart('{').trimEnd('}')

                JoltageMachine(
                    buttons = buttons.map { button ->
                        val array = BooleanArray(desiredLights.length)

                        for (index in button.trimStart('(').trimEnd(')').split(',')) {
                            array[index.toInt()] = true
                        }

                        array
                    },
                    target = joltage.split(',').map { it.toInt() }.toIntArray(),
                )
            }
            .withIndex()
            .sumOf { (index, machine) ->
                println("${index + 1}")
                val greedy = measureTimedValue {
                    sequence {
                        yield(machine.greedyDeterministic(maxResidualReduction = true, maxPresses = true))
                        yield(machine.greedyDeterministic(maxResidualReduction = true, maxPresses = false))
                        yield(machine.greedyDeterministic(maxResidualReduction = false, maxPresses = true))
                        yield(machine.greedyDeterministic(maxResidualReduction = false, maxPresses = false))

                        // try a large number of times, so hard cases have many chances for a greedy solution
                        repeat(10_000) { count ->
                            yield(machine.greedyRandom(Random(seed = count), maxPresses = true))
                            yield(machine.greedyRandom(Random(seed = count), maxPresses = false))
                        }
                    }
                        .filterNotNull()
                        .take(10) // only take the first few, so easy cases don't run too many greedy iterations
                        .minOrNull()
                }
                println("  > greedy upper bound: ${greedy.value} in ${greedy.duration.inWholeMilliseconds}ms")

                val result = measureTimedValue { machine.dfs(upperBound = greedy.value) }
                println("  > ${result.value.toString().padEnd(4)} in ${result.duration.inWholeMilliseconds}ms")

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

    private val memo: MutableMap<ResidualKey, Int> = mutableMapOf()

    // map from counter index to the button indices that increment it
    private val counterToButtons: List<List<Int>> = List(numCounters) { counter ->
        (0 until numButtons).filter { button -> buttons[button][counter] }
    }

    private val buttonToCounters: List<List<Int>> = List(numButtons) { button ->
        (0 until numCounters).filter { counter -> buttons[button][counter] }
    }

    // number of counters each button affects
    private val buttonSizes: List<Int> = buttons.map { button -> button.count { it } }

    inner class State(
        val residual: IntArray, // starts at target and counts down to 0
        val cost: Int,
    ) {
        fun isTarget() = residual.all { it == 0 }

        // maxPresses[button] is the maximum number of times button can be pressed without exceeding the target
        val maxPresses: IntArray by lazy {
            IntArray(numButtons) { button ->
                buttonToCounters[button].minOf { counter -> residual[counter] }
            }
        }

        fun canPress(button: Int): Boolean = maxPresses[button] > 0
    }

    fun State.pressableButtons(): List<Int> {
        return (0 until numButtons).filter { canPress(it) }
    }

    fun State.press(button: Int, times: Int): State? {
        val next = residual.clone()
        for (counter in buttonToCounters[button]) {
            val v = next[counter] - times
            if (v < 0) return null
            next[counter] = v
        }

        return State(residual = next, cost = cost + times)
    }

    // lower bound on the number of presses remaining
    fun State.lowerBound(): Int {
        var maxButtonSize = 0
        for (button in 0 until numButtons) {
            if (canPress(button)) {
                maxButtonSize = maxOf(maxButtonSize, buttonSizes[button])
            }
        }
        check(maxButtonSize != 0) { "no pressable buttons" }

        val boundBySum = (residual.sum() + maxButtonSize - 1) / maxButtonSize

        return max(residual.max(), boundBySum)
    }

    // pruning check: various ways in which the current state cannot possibly be satisfied
    fun State.canSatisfy(): Boolean {
        val remainingButtons = pressableButtons().ifEmpty { return false }

        // check if any counter is impossible to satisfy, even by pressing all the buttons that increment it the maximum
        // number of times
        for (counter in 0 until numCounters) {
            if (residual[counter] == 0) continue

            // pressable buttons for this counter
            val buttons = remainingButtons.filter { button -> buttons[button][counter] }
                .ifEmpty { return false }

            // maximum allowed presses among all the buttons that affect this counter
            val maxAllowedPresses = buttons.sumOf { button -> maxPresses[button] }

            // if it's less than the residual, this state is not satisfiable
            if (maxAllowedPresses < residual[counter]) return false
            // TODO if equal, these are forced presses -> apply them?
        }

        return true
    }

    fun State.forcedMoves(): State? {
        var currentState = this

        while (true) {
            var anyPressed = false
            for (counter in 0 until numCounters) {
                val buttons = counterToButtons[counter].filter { currentState.canPress(it) }

                if (buttons.size == 1) {
                    val button = buttons.first()
                    currentState = currentState.press(button, currentState.maxPresses[button]) ?: return null
                    anyPressed = true
                }
            }

            if (!anyPressed) break
        }

        return currentState
    }

    // finds the counter(s) affected by the fewest number of pressable buttons
    fun State.hardestCounters(): List<Int> {
        val remainingCounters = (0 until numCounters).filter { residual[it] > 0 }
        val min = remainingCounters.minOf { counter ->
            counterToButtons[counter].count { button -> canPress(button) }
        }

        return remainingCounters
            .filter { counter ->
                counterToButtons[counter].count { button -> canPress(button) } == min
            }
    }

    fun dfs(upperBound: Int? = null): Int {
        memo.clear()
        val bestCost = IntRef(upperBound ?: Int.MAX_VALUE)
        dfs(bestCost, State(target, 0))
        check(bestCost.value != Int.MAX_VALUE)
        return bestCost.value
    }

    // simple randomized, greedy attempt to find an upper bound on the number of presses:
    // - pick a random button, among those that we can still press
    // - pick a random number of times to press it between 1 and the maximum number of allowed presses
    // repeat until success or failure
    fun greedyRandom(random: Random, maxPresses: Boolean): Int? {
        var state = State(target, 0)

        while (!state.isTarget()) {
            val counter = state.hardestCounters().random(random)
            val button = counterToButtons[counter]
                .filter { state.canPress(it) }
                .randomOrNull(random) ?: return null
            // actually use maxPresses - 1 to preserve some flexibility
            val presses = if (maxPresses) (state.maxPresses[button] - 1).coerceAtLeast(1) else 1
            state = state.press(button, presses)?.forcedMoves() ?: return null
        }

        return state.cost
    }

    fun greedyDeterministic(maxResidualReduction: Boolean, maxPresses: Boolean): Int? {
        var state = State(target, 0)

        while (!state.isTarget()) {
            val button = state.pressableButtons()
                .ifEmpty { return null }
                .maxBy { button ->
                    if (maxResidualReduction) {
                        buttonToCounters[button].sumOf { counter -> state.residual[counter] }
                    } else {
                        buttonSizes[button]
                    }
                }
            val presses = if (maxPresses) (state.maxPresses[button] - 1).coerceAtLeast(1) else 1
            state = state.press(button, presses)?.forcedMoves() ?: return null
        }

        return state.cost
    }

    private class ResidualKey(private val data: IntArray) {
        private val hash = data.contentHashCode()
        override fun hashCode() = hash
        override fun equals(other: Any?) = other is ResidualKey && other.data.contentEquals(data)
    }

    private fun dfs(bestCost: IntRef, state: State) {
        if (state.cost >= bestCost.value) return

        val currentState = state.forcedMoves() ?: return

        if (currentState.cost >= bestCost.value) return
        if (currentState.isTarget()) {
            bestCost.value = currentState.cost
            return
        }

        val key = ResidualKey(currentState.residual)
        memo[key]?.let {
            if (it <= currentState.cost) return
        }
        memo[key] = currentState.cost

        if (!currentState.canSatisfy()) {
            memo[key] = Int.MIN_VALUE
            return
        }

        val lowerBound = currentState.lowerBound()
        if (currentState.cost + lowerBound >= bestCost.value) return

        val hardestCounter = currentState.hardestCounters().first()

        val candidateButtons = counterToButtons[hardestCounter]
            .filter { currentState.canPress(it) }
            .sortedByDescending { button ->
                // heuristic: try buttons that maximize their maxPresses * (sum of residuals they affect)
                currentState.maxPresses[button] *
                    buttonToCounters[button].sumOf { counter -> currentState.residual[counter] }
            }

        for (button in candidateButtons) {
            val maxPresses = currentState.maxPresses[button]

            // start with max presses
            dfs(bestCost = bestCost, state = currentState.press(button, maxPresses)!!)
            if (state.cost >= bestCost.value) return // early abort if another branch is better

            // then try a single press
            dfs(bestCost = bestCost, state = currentState.press(button, 1)!!)
            if (state.cost >= bestCost.value) return // early abort if another branch is better

            // then the rest in descending order
            for (presses in (maxPresses - 1) downTo 2) {
                dfs(
                    bestCost = bestCost,
                    state = currentState.press(button, presses)!!,
                )
                if (state.cost >= bestCost.value) return // early abort if another branch is better
            }
        }
    }
}
