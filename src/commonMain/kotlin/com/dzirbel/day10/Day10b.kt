package com.dzirbel.day10

import kotlin.io.path.Path
import kotlin.io.path.useLines
import kotlin.time.measureTimedValue

private val expected = mapOf(
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
    50 to 214,
)

fun main(args: Array<String>) {
    val path = args.getOrElse(0) { "inputs/day10.txt" }
    val total = Path(path).useLines { lines ->
        lines
            .map { line ->
                val split = line.split(' ')

                val target = split.last()
                    .trimStart('{')
                    .trimEnd('}')
                    .split(',')
                    .map { it.toInt() }
                    .toIntArray()

                // drop first (indicator lights) and last (joltage); sort with largest buttons first
                val buttons = split.subList(fromIndex = 1, toIndex = split.lastIndex)
                    .map { button ->
                        val array = BooleanArray(target.size)

                        for (index in button.trimStart('(').trimEnd(')').split(',')) {
                            array[index.toInt()] = true
                        }

                        array
                    }
                    .sortedByDescending { button ->
                        button.count { it }
                    }

                JoltageMachine(buttons = buttons, target = target)
            }
            .withIndex()
            .sumOf { (index, machine) ->
                val lineNumber = index + 1
                print(lineNumber.toString().padEnd(length = 3))

                val result = measureTimedValue {
                    val (simplificationCost, simplified) = machine.simplify()
                    simplificationCost + simplified.dfs()
                }

                print(" >> ${result.value.toString().padEnd(length = 4)} in ${result.duration.inWholeMilliseconds}ms")
                println()

                val expectedValue = expected[lineNumber]
                if (expectedValue != null) {
                    check(result.value == expectedValue) {
                        "expected $expectedValue for $lineNumber; got ${result.value}"
                    }
                }

                result.value
            }
    }

    check(total == 20_871)
    println()
    println(total)
}

private class IntRef(var value: Int)

private class JoltageMachine(
    val buttons: List<BooleanArray>,
    val target: IntArray,
) {
    private val numButtons = buttons.size
    private val numCounters = target.size

    // map from counter index to the button indices that increment it
    private val counterToButtons: Array<IntArray> = Array(numCounters) { counter ->
        val tmp = IntArray(numButtons)
        var size = 0
        for (button in 0 until numButtons) {
            if (buttons[button][counter]) {
                tmp[size++] = button
            }
        }
        tmp.copyOf(size)
    }

    // map from button index to the counters it increments
    private val buttonToCounters: Array<IntArray> = Array(numButtons) { button ->
        val tmp = IntArray(numCounters)
        var size = 0
        for (counter in 0 until numCounters) {
            if (buttons[button][counter]) {
                tmp[size++] = counter
            }
        }
        tmp.copyOf(size)
    }

    // number of counters each button affects
    private val buttonSizes: IntArray = IntArray(numButtons) { button ->
        buttonToCounters[button].size
    }

    // int mask of the counters affected by each button
    private val buttonToCounterMask: IntArray = IntArray(numButtons) { button ->
        var mask = 0
        for (counter in buttonToCounters[button]) {
            mask = mask or (1 shl counter)
        }
        mask
    }

    private val maskBitCount: IntArray? = if (numCounters <= 20) {
        val counts = IntArray(1 shl numCounters)
        for (mask in 1 until counts.size) {
            counts[mask] = counts[mask shr 1] + (mask and 1)
        }
        counts
    } else {
        null
    }

    init {
        check(numCounters < 32) { "bitmask operations can't support more than 31 counters" }
    }

    /**
     * Simplifies this [JoltageMachine] by iteratively looking for:
     * 1. pairs of counters for which there is exactly one button affected by one counter and not the other; in this
     *    case we can set a min (and sometimes max) bound on the number of times a button must be pressed
     * 2. duplicate buttons (which can sometimes occur after the above simplification)
     */
    fun simplify(): Pair<Int, JoltageMachine> {
        fun JoltageMachine.withPresses(button: Int, presses: Int): JoltageMachine {
            return JoltageMachine(
                buttons = buttons,
                target = target.mapIndexed { counter, value ->
                    if (buttons[button][counter]) value - presses else value
                }.toIntArray(),
            )
        }

        fun JoltageMachine.minusButton(button: Int): JoltageMachine {
            return JoltageMachine(
                buttons = buttons.filterIndexed { index, _ -> index != button },
                target = target,
            )
        }

        fun JoltageMachine.minusCounter(counter: Int): JoltageMachine {
            return JoltageMachine(
                buttons = buttons
                    .map { button ->
                        button.filterIndexed { index, _ -> index != counter }.toBooleanArray()
                    }
                    // button size sorting can break when removing a counter; re-sort to ensure correctness
                    .sortedByDescending { button ->
                        button.count { it }
                    },
                target = target.filterIndexed { index, _ -> index != counter }.toIntArray(),
            )
        }

        for (counterA in 0 until numCounters) {
            val buttonsA = counterToButtons[counterA].toSet()
            for (counterB in 0 until numCounters) {
                if (counterA == counterB) continue

                val buttonsB = counterToButtons[counterB].toSet()

                val diff = target[counterA] - target[counterB]

                if (buttonsA == buttonsB) {
                    return minusCounter(counterA).simplify()
                }

                if (diff <= 0) continue

                // find the only button for which A is true and B is not, if there is exactly one
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

                    return if (perfect) {
                        // delete counter A (or B; equivalent)
                        // delete the pressed button
                        withPresses(button = importantButton, presses = diff)
                            .minusButton(importantButton)
                            .minusCounter(counterA)
                    } else {
                        withPresses(button = importantButton, presses = diff)
                    }
                        .simplify()
                        .let { (presses, machine) -> (presses + diff) to machine }
                }
            }
        }

        // remove duplicate buttons
        for (buttonA in 0 until numButtons) {
            for (buttonB in buttonA + 1 until numButtons) {
                if (buttonToCounters[buttonA].contentEquals(buttonToCounters[buttonB])) {
                    return minusButton(buttonA)
                        .simplify()
                }
            }
        }

        return 0 to this
    }

    fun dfs(): Int {
        val ref = IntRef(Int.MAX_VALUE)
        dfs(State(), ref)
        check(ref.value != Int.MAX_VALUE)
        return ref.value
    }

    private fun dfs(state: State, bestCost: IntRef) {
        if (state.cost >= bestCost.value) return
        if (state.isSolved()) {
            bestCost.value = state.cost
            return
        }

        val state = state.forced() ?: return

        if (state.cost >= bestCost.value) return
        if (state.isSolved()) {
            bestCost.value = state.cost
            return
        }

        if (state.cost + state.lowerBound() >= bestCost.value) return

        val button = state.selectButton()
        if (button == -1) return

        val maxPresses = state.maxPresses[button]

        for (presses in maxPresses downTo 0) {
            dfs(state.applyPresses(button, presses) ?: continue, bestCost)
        }
    }

    inner class State(
        val residual: IntArray,
        val remainingButtons: BooleanArray,
        val cost: Int,
    ) {
        constructor() : this(
            residual = target,
            remainingButtons = BooleanArray(numButtons) { true },
            cost = 0,
        )

        val maxPresses: IntArray = IntArray(numButtons)
        val maxPressableButtonSize: Int

        init {
            var maxSize = 0
            for (button in 0 until numButtons) {
                if (!remainingButtons[button]) continue

                val counters = buttonToCounters[button]
                var minResidual = Int.MAX_VALUE
                for (i in counters.indices) {
                    val r = residual[counters[i]]
                    if (r < minResidual) minResidual = r
                }
                if (minResidual == Int.MAX_VALUE) minResidual = 0

                maxPresses[button] = minResidual
                if (minResidual > 0) {
                    val size = buttonSizes[button]
                    if (size > maxSize) maxSize = size
                }
            }
            maxPressableButtonSize = maxSize
        }

        fun canPress(button: Int) = maxPresses[button] > 0

        fun isSolved(): Boolean {
            for (r in residual) {
                if (r != 0) return false
            }
            return true
        }

        fun anyPressable(): Boolean {
            for (button in 0 until numButtons) {
                if (maxPresses[button] > 0) return true
            }
            return false
        }

        /**
         * Applies forced button presses and returns the resulting [State], or null if it becomes unsatisfiable.
         */
        fun forced(): State? {
            var state = this
            val forcedButtons = IntArray(numButtons)
            val forcedCounts = IntArray(numButtons)
            val forcedMark = IntArray(numButtons)
            var stamp = 1

            while (true) {
                if (state.isSolved()) return state
                if (!state.anyPressable()) return null

                var forcedSize = 0
                val currentStamp = stamp++
                var infeasible = false

                fun setForced(button: Int, count: Int): Boolean {
                    if (forcedMark[button] == currentStamp) {
                        return forcedCounts[button] == count
                    }
                    forcedMark[button] = currentStamp
                    forcedCounts[button] = count
                    forcedButtons[forcedSize++] = button
                    return true
                }

                for (counter in 0 until numCounters) {
                    val r = state.residual[counter]
                    if (r == 0) continue

                    var numPressableButtons = 0
                    var totalPossiblePresses = 0
                    var soleButton = -1

                    val buttons = counterToButtons[counter]
                    for (i in buttons.indices) {
                        val button = buttons[i]
                        val max = state.maxPresses[button]
                        if (max > 0) {
                            numPressableButtons++
                            totalPossiblePresses += max
                            if (numPressableButtons == 1) soleButton = button
                        }
                    }

                    if (numPressableButtons == 0 || r > totalPossiblePresses) {
                        infeasible = true
                        break
                    }

                    if (numPressableButtons == 1) {
                        // only a single button can cover this residual
                        if (!setForced(soleButton, r)) {
                            infeasible = true
                            break
                        }
                    } else if (totalPossiblePresses == r) {
                        // all remaining capacity must be used exactly
                        for (i in buttons.indices) {
                            val button = buttons[i]
                            val max = state.maxPresses[button]
                            if (max > 0 && !setForced(button, max)) {
                                infeasible = true
                                break
                            }
                        }
                        if (infeasible) break
                    }
                }

                if (infeasible) return null
                if (forcedSize == 0) return state

                state = state.applyPresses(forcedButtons, forcedCounts, forcedSize) ?: return null
            }
        }

        /**
         * Attempts to find a lower bound on the number of additional button presses to solve this state. It employs
         * three different bounding mechanisms:
         * 1. there must be at least as many presses as the maximum residual
         * 2. with the maximum pressable button size, there must be at least ceil(total residual / size) presses
         * 3. for every subset of counters, looks for the pressable buttons which affect that counter subset, and the
         *    total number of times they must be pressed
         */
        fun lowerBound(): Int {
            var residualSum = 0
            var boundByMax = 0

            for (r in residual) {
                residualSum += r
                if (r > boundByMax) boundByMax = r
            }

            if (maxPressableButtonSize == 0) return Int.MAX_VALUE / 4
            val boundBySum = (residualSum + maxPressableButtonSize - 1) / maxPressableButtonSize

            var boundBySubset = 0
            val numMasks = 1 shl numCounters
            val pressableMasks = IntArray(numButtons)
            var pressableCount = 0
            for (button in 0 until numButtons) {
                if (maxPresses[button] > 0) {
                    pressableMasks[pressableCount++] = buttonToCounterMask[button]
                }
            }

            val localMaskBitCount = maskBitCount
            if (localMaskBitCount != null) {
                val sumByMask = IntArray(numMasks)
                for (mask in 1 until numMasks) {
                    val lsb = mask and -mask
                    val index = Integer.numberOfTrailingZeros(lsb)
                    sumByMask[mask] = sumByMask[mask xor lsb] + residual[index]
                }

                for (mask in 1 until numMasks) {
                    val sum = sumByMask[mask]
                    if (sum == 0) continue

                    val maskSize = localMaskBitCount[mask]
                    var cover = 0
                    for (i in 0 until pressableCount) {
                        val c = Integer.bitCount(pressableMasks[i] and mask)
                        if (c > cover) {
                            cover = c
                            if (cover == maskSize) break
                        }
                    }
                    if (cover == 0) continue

                    val bound = (sum + cover - 1) / cover
                    if (bound > boundBySubset) boundBySubset = bound
                }
            } else {
                for (mask in 1 until numMasks) {
                    var sum = 0
                    var remaining = mask
                    while (remaining != 0) {
                        val lsb = remaining and -remaining
                        val index = Integer.numberOfTrailingZeros(lsb)
                        sum += residual[index]
                        remaining = remaining xor lsb
                    }
                    if (sum == 0) continue

                    val maskSize = Integer.bitCount(mask)
                    var cover = 0
                    for (i in 0 until pressableCount) {
                        val c = Integer.bitCount(pressableMasks[i] and mask)
                        if (c > cover) {
                            cover = c
                            if (cover == maskSize) break
                        }
                    }
                    if (cover == 0) continue

                    val bound = (sum + cover - 1) / cover
                    if (bound > boundBySubset) boundBySubset = bound
                }
            }

            return maxOf(boundByMax, boundBySum, boundBySubset)
        }

        /**
         * Chooses the most optimal button to select for the next DFS branch. This is the largest button affecting the
         * hardest counter, which is the counter which can be incremented by the smallest number of pressable buttons.
         */
        fun selectButton(): Int {
            var hardestCounter = -1
            var minButtons = Int.MAX_VALUE
            var minResidual = Int.MAX_VALUE

            for (counter in 0 until numCounters) {
                val r = residual[counter]
                if (r == 0) continue

                var pressableButtons = 0
                val buttons = counterToButtons[counter]
                for (i in buttons.indices) {
                    if (maxPresses[buttons[i]] > 0) pressableButtons++
                }

                if (pressableButtons < minButtons || (pressableButtons == minButtons && r < minResidual)) {
                    hardestCounter = counter
                    minButtons = pressableButtons
                    minResidual = r
                }
            }

            // buttons are sorted by size, so the largest one is always the lowest index
            val hardestButtons = counterToButtons[hardestCounter]
            for (i in hardestButtons.indices) {
                val button = hardestButtons[i]
                if (maxPresses[button] > 0) return button
            }
            return -1
        }

        fun applyPresses(button: Int, count: Int): State? {
            val newResidual = if (count == 0) residual else residual.clone()
            if (count > 0) {
                for (counter in buttonToCounters[button]) {
                    val updated = newResidual[counter] - count
                    if (updated < 0) return null
                    newResidual[counter] = updated
                }
            }

            val newRemainingButtons = remainingButtons.clone()
            newRemainingButtons[button] = false
            return State(residual = newResidual, remainingButtons = newRemainingButtons, cost = cost + count)
        }

        fun applyPresses(buttons: IntArray, countsByButton: IntArray, size: Int): State? {
            val newResidual = residual.clone()
            val newRemainingButtons = remainingButtons.clone()
            var cost = this.cost

            for (i in 0 until size) {
                val button = buttons[i]
                val count = countsByButton[button]
                if (count <= 0) continue

                newRemainingButtons[button] = false
                cost += count
                for (counter in buttonToCounters[button]) {
                    val updated = newResidual[counter] - count
                    if (updated < 0) return null
                    newResidual[counter] = updated
                }
            }

            return State(residual = newResidual, remainingButtons = newRemainingButtons, cost = cost)
        }
    }
}
