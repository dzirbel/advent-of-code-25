package com.dzirbel.day10

import java.util.PriorityQueue
import kotlin.io.path.Path
import kotlin.io.path.useLines
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.TimedValue
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
                    presses = 0,
                )
            }
            .map { machine ->
                machine.simplify()
            }
            .withIndex()
            .sumOf { (index, machine) ->
                val lineNumber = index + 1
                print(lineNumber.toString().padEnd(length = 3))

                fun display(result: TimedValue<*>): String {
                    return "${result.value.toString().padEnd(length = 4)} in ${result.duration.inWholeMilliseconds}ms"
                }

                val greedy = measureTimedValue { machine.greedy() }
                print(" [greedy: ${display(greedy)}]".padEnd(length = 28))

                val result = measureTimedValue { machine.dfs(upperBound = greedy.value) }
                print(" >> ${display(result)}")
                println()

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
    private val presses: Int,
) {
    val numButtons = buttons.size
    val numCounters = target.size

    // map from counter index to the button indices that increment it
    val counterToButtons: List<List<Int>> = List(numCounters) { counter ->
        (0 until numButtons).filter { button -> buttons[button][counter] }
    }

    // map from button index to the counters it increments
    val buttonToCounters: List<List<Int>> = List(numButtons) { button ->
        (0 until numCounters).filter { counter -> buttons[button][counter] }
    }

    // number of counters each button affects
    val buttonSizes: List<Int> = buttons.map { button -> button.count { it } }

    // int mask of the counters affected by each button
    val buttonMasks: IntArray = IntArray(numButtons) { button ->
        var mask = 0
        for (counter in 0 until numCounters) {
            if (buttons[button][counter]) mask = mask or (1 shl counter)
        }
        mask
    }

    private val dfsMemo: MutableMap<ResidualKey, Int> = mutableMapOf()

    private class ResidualKey(val data: IntArray) {
        private val hash = data.contentHashCode()
        override fun hashCode() = hash
        override fun equals(other: Any?) = other is ResidualKey && other.data.contentEquals(data)
    }

    private class QueueEntry(val key: ResidualKey, val g: Int, val f: Int)

    fun simplify(): JoltageMachine {
        for (counterA in 0 until numCounters) {
            val buttonsA = counterToButtons[counterA].toSet()
            for (counterB in 0 until numCounters) {
                if (counterA == counterB) continue

                val buttonsB = counterToButtons[counterB].toSet()

                val diff = target[counterA] - target[counterB]

                if (buttonsA == buttonsB) {
                    check(diff == 0) { "impossible: counters with identical buttons but different targets" }

                    return minusCounter(counterA)
                        .simplify()
                }

                if (diff <= 0) continue

                // find the only button for which A is try and B is not, if there is exactly one
                var importantButton: Int? = null
                for (button in 0 until numButtons) {
                    if (buttons[button][counterA] && !buttons[button][counterB]) {
                        if (importantButton == null) {
                            importantButton = button
                        } else {
                            importantButton = null
                            break
                        }
                    }
                }

                if (importantButton != null) {
                    // perfect if this important button is the only button that affects A and not B
                    val perfect = buttonsA == buttonsB.plus(importantButton)

                    if (perfect) {
                        // delete counter A (or B; equivalent)
                        // delete the pressed button
                        return withPresses(button = importantButton, presses = diff)
                            .minusButton(importantButton)
                            .minusCounter(counterA)
                            .simplify()
                    } else {
                        return withPresses(button = importantButton, presses = diff)
                            .simplify()
                    }
                }
            }
        }

        for (buttonA in 0 until numButtons) {
            for (buttonB in buttonA + 1 until numButtons) {
                if (buttonToCounters[buttonA] == buttonToCounters[buttonB]) {
                    return minusButton(buttonA)
                        .simplify()
                }
            }
        }

        return this
    }

    private fun withPresses(button: Int, presses: Int): JoltageMachine {
        return JoltageMachine(
            buttons = buttons,
            target = target.mapIndexed { counter, value ->
                if (buttons[button][counter]) value - presses else value
            }.toIntArray(),
            presses = this.presses + presses,
        )
    }

    private fun minusButton(button: Int): JoltageMachine {
        return JoltageMachine(
            buttons = buttons.filterIndexed { index, _ -> index != button },
            target = target,
            presses = presses,
        )
    }

    private fun minusCounter(counter: Int): JoltageMachine {
        return JoltageMachine(
            buttons = buttons.map { button ->
                button.filterIndexed { index, _ -> index != counter }.toBooleanArray()
            },
            target = target.filterIndexed { index, _ -> index != counter }.toIntArray(),
            presses = presses,
        )
    }

    fun dfs(upperBound: Int? = null): Int {
        dfsMemo.clear()
        val bestCost = IntRef(upperBound ?: Int.MAX_VALUE)
        dfs(bestCost, JoltageMachineState(this))
        check(bestCost.value != Int.MAX_VALUE) { "did not find any solutions" }
        return bestCost.value + presses
    }

    fun greedy(): Int? {
        return sequence {
            yield(greedyDeterministic(maxResidualReduction = true, maxPresses = true))
            yield(greedyDeterministic(maxResidualReduction = true, maxPresses = false))
            yield(greedyDeterministic(maxResidualReduction = false, maxPresses = true))
            yield(greedyDeterministic(maxResidualReduction = false, maxPresses = false))

            // try a large number of times, so hard cases have many chances for a greedy solution
            repeat(5_000) { count ->
                yield(greedyRandom(Random(seed = count), maxPresses = true))
                yield(greedyRandom(Random(seed = count), maxPresses = false))
            }
        }
            .filterNotNull()
            .take(3) // only take the first few, so easy cases don't run too many greedy iterations
            .minOrNull()
            ?.let { it + presses }
    }

    private fun h(key: ResidualKey, state: JoltageMachineState): Int {
        if (key.data.all { it == 0 }) return 0

        val pressableButtons = (0 until numButtons).filter { state.canPress(it) }
        if (pressableButtons.isEmpty()) return Int.MAX_VALUE / 4

        val maxButtonSize = pressableButtons.maxOf { buttonSizes[it] }
        val boundBySum = (key.data.sum() + maxButtonSize - 1) / maxButtonSize

        var boundBySubset = 0
        val maxSubsetSize = min(numCounters, 2)

        fun updateBound(mask: Int, sum: Int) {
            if (sum == 0) return
            var cover = 0
            for (button in pressableButtons) {
                val c = Integer.bitCount(buttonMasks[button] and mask)
                if (c > cover) cover = c
            }
            if (cover == 0) return
            val bound = (sum + cover - 1) / cover
            if (bound > boundBySubset) boundBySubset = bound
        }

        fun visit(start: Int, remaining: Int, mask: Int, sum: Int) {
            if (remaining == 0) {
                updateBound(mask, sum)
                return
            }
            val lastStart = numCounters - remaining
            for (i in start..lastStart) {
                visit(
                    start = i + 1,
                    remaining = remaining - 1,
                    mask = mask or (1 shl i),
                    sum = sum + key.data[i],
                )
            }
        }

        for (size in 1..maxSubsetSize) {
            visit(start = 0, remaining = size, mask = 0, sum = 0)
        }

        val boundByMax = key.data.max()

        return maxOf(boundByMax, boundBySum, boundBySubset)
    }

    fun aStar(upperBound: Int?): Int {
        val startState = JoltageMachineState(machine = this).forcedMoves()!!
        val start = ResidualKey(startState.residual)
        val startCost = presses + startState.cost

        // gScore[residual] is the currently known cheapest cost to a particular residual
        val gScore = hashMapOf(start to startCost)

        val queue = PriorityQueue(compareBy<QueueEntry> { it.f }.thenComparing { it.g })
        queue.add(QueueEntry(key = start, g = startCost, f = startCost + h(start, startState)))

        while (true) {
            val current = checkNotNull(queue.poll()) { "A* queue is empty!" }

            val gCurrent = gScore.getValue(current.key)
            if (current.g != gCurrent) continue // stale queue entry

            val currentState = JoltageMachineState(machine = this, residual = current.key.data, cost = gCurrent)
            if (currentState.isTarget()) return gCurrent

            val hardestCounter = currentState.hardestCounter()
            for (button in counterToButtons[hardestCounter]) {
                val attempts = when (val maxPresses = currentState.maxPresses[button]) {
                    0 -> intArrayOf()
                    1 -> intArrayOf(1)
                    2, 3, 4 -> intArrayOf(1, maxPresses)
                    else -> intArrayOf(1, maxPresses / 2, maxPresses)
                }

                for (presses in attempts) {
                    val neighbor = currentState.press(button = button, times = presses) ?: break

                    val neighborKey = ResidualKey(neighbor.residual)
                    val gNeighbor = gScore[neighborKey]
                    if (gNeighbor == null || neighbor.cost < gNeighbor) {
                        gScore[neighborKey] = neighbor.cost
                        val neighborF = neighbor.cost + h(neighborKey, neighbor)
                        if (upperBound != null && neighborF > upperBound) continue
                        queue.add(QueueEntry(key = neighborKey, g = neighbor.cost, f = neighborF))
                    }
                }
            }
        }
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
        dfsMemo[key]?.let { memoCost -> if (memoCost <= state.cost) return }
        dfsMemo[key] = state.cost

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

    fun forcedMoves(): JoltageMachineState? {
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
        var minButtons = Int.MAX_VALUE
        val hardestCounters = mutableListOf<Int>()

        for (counter in 0 until machine.numCounters) {
            if (residual[counter] == 0) continue
            val buttons = machine.counterToButtons[counter].count { button -> canPress(button) }

            if (buttons < minButtons) {
                hardestCounters.clear()
                hardestCounters.add(counter)
                minButtons = buttons
            } else if (buttons == minButtons) {
                hardestCounters.add(counter)
            }
        }

        return hardestCounters
    }

    // finds one of the counter(s) affected by the fewest number of pressable buttons
    // breaks ties with the counter that has the lowest residual
    fun hardestCounter(): Int {
        var minButtons = Int.MAX_VALUE
        var maxResidual = -1
        var hardestCounter = -1

        for (counter in 0 until machine.numCounters) {
            if (residual[counter] == 0) continue
            val buttons = machine.counterToButtons[counter].count { button -> canPress(button) }

            if (buttons < minButtons || (buttons == minButtons && residual[counter] < maxResidual)) {
                hardestCounter = counter
                minButtons = buttons
                maxResidual = residual[counter]
            }
        }

        return hardestCounter
    }

    fun mostImpactfulButton(): Int {
        val counters = hardestCounters()

        // only one limiting counter; return the first (largest due to sorting) button which affects it
        if (counters.size == 1) return machine.counterToButtons[counters.first()].first { canPress(it) }

        val countersSet = counters.toSet()
        var mostAffectedCounters = 0
        var bestButton = 0
        for (button in 0 until machine.numButtons) {
            if (!canPress(button)) continue

            val affected = machine.buttonToCounters[button].count { counter -> counter in countersSet }
            if (affected > mostAffectedCounters) {
                mostAffectedCounters = affected
                bestButton = button
            }
        }

        return bestButton
    }
}
