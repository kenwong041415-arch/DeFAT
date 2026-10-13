package com.defat.core.domain.calc

/**
 * The row of eight 250 ml cups on Home (plan §11), as pure arithmetic.
 *
 * The cups cover the first 2000 ml of the day; anything beyond that, or any
 * odd amount typed into the 自訂 field, is carried along unchanged as
 * [extraMl] so tapping a cup never loses a custom entry.
 */
object WaterCups {

    const val CUP_ML: Int = 250
    const val CUP_COUNT: Int = 8
    const val MAX_DAILY_ML: Int = 10_000

    /** How many of the eight cups are full. */
    fun filledCups(currentMl: Int): Int = (currentMl / CUP_ML).coerceIn(0, CUP_COUNT)

    /** Millilitres that are not accounted for by the filled cups. */
    fun extraMl(currentMl: Int): Int = currentMl - filledCups(currentMl) * CUP_ML

    /**
     * Tapping cup [cupNumber] (1..8): fills up to it; tapping the last filled
     * cup removes that one cup instead.
     */
    fun tap(currentMl: Int, cupNumber: Int): Int {
        require(cupNumber in 1..CUP_COUNT) { "cupNumber must be in 1..$CUP_COUNT, was $cupNumber" }
        val filled = filledCups(currentMl)
        val newFilled = if (cupNumber == filled) filled - 1 else cupNumber
        return (newFilled * CUP_ML + extraMl(currentMl)).coerceIn(0, MAX_DAILY_ML)
    }

    /** Adds a typed amount (the 自訂 field). */
    fun add(currentMl: Int, addMl: Int): Int {
        require(addMl > 0) { "addMl must be > 0, was $addMl" }
        return (currentMl + addMl).coerceAtMost(MAX_DAILY_ML)
    }
}
