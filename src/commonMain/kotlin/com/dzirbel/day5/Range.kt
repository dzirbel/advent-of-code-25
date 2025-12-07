package com.dzirbel.day5

data class Range(val start: Long, val end: Long) : Comparable<Range> {
    operator fun contains(value: Long) = value in start..end
    override fun compareTo(other: Range) = start.compareTo(other.start)

    fun mergeWith(other: Range): Range = Range(
        start = minOf(this.start, other.start),
        end = maxOf(this.end, other.end),
    )
}

fun MutableList<Range>.addAndConsolidate(range: Range) {
    val search = binarySearch(range)
    if (search >= 0) {
        val existing = this[search]
        if (range.end > existing.end) {
            this[search] = range
        }
    } else {
        val newIndex = -search - 1

        this.add(newIndex, range)
        consolidateUp(newIndex)
        consolidateDown(newIndex)
    }
}

fun MutableList<Range>.consolidateDown(index: Int) {
    if (index == 0) return

    val curr = this[index]
    val prev = this[index - 1]

    check(prev <= curr)
    if (prev.end >= curr.start) {
        this[index - 1] = prev.mergeWith(curr)
        this.removeAt(index)
        consolidateDown(index - 1)
    }
}

fun MutableList<Range>.consolidateUp(index: Int) {
    if (index == lastIndex) return

    val curr = this[index]
    val next = this[index + 1]

    check(curr <= next)
    if (next.start <= curr.end) {
        this[index] = curr.mergeWith(next)
        this.removeAt(index + 1)
        consolidateUp(index)
    }
}
