package com.dues.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** k-th payment date. Stepping from the anchor keeps "31st" billing on month-ends. */
fun Sub.occurrence(k: Long): LocalDate = when (cycleUnit) {
    CycleUnit.DAY -> startDate.plusDays(k * cycleCount)
    CycleUnit.WEEK -> startDate.plusWeeks(k * cycleCount)
    CycleUnit.MONTH -> startDate.plusMonths(k * cycleCount)
    CycleUnit.YEAR -> startDate.plusYears(k * cycleCount)
}

private fun Sub.approxDays() = cycleCount * when (cycleUnit) {
    CycleUnit.DAY -> 1.0
    CycleUnit.WEEK -> 7.0
    CycleUnit.MONTH -> 30.44
    CycleUnit.YEAR -> 365.25
}

/** Index of the first payment on or after [d]. */
private fun Sub.firstIndexFrom(d: LocalDate): Long {
    val gap = ChronoUnit.DAYS.between(startDate, d)
    var k = if (gap <= 0) 0L else maxOf(0L, (gap / approxDays()).toLong() - 1)
    while (k > 0 && occurrence(k - 1) >= d) k--
    while (occurrence(k) < d) k++
    return k
}

fun Sub.nextPayment(today: LocalDate): LocalDate? =
    if (cancelledOn != null) null else occurrence(firstIndexFrom(today))

/** Payment dates in [from]..[to], none after cancellation. */
fun Sub.paymentsIn(from: LocalDate, to: LocalDate): List<LocalDate> {
    val end = cancelledOn?.let { minOf(it, to) } ?: to
    val out = mutableListOf<LocalDate>()
    var k = firstIndexFrom(from)
    while (true) {
        val d = occurrence(k++)
        if (d > end) break
        out += d
    }
    return out
}

fun Sub.paymentsPerYear(): Double = when (cycleUnit) {
    CycleUnit.DAY -> 365.0
    CycleUnit.WEEK -> 52.0
    CycleUnit.MONTH -> 12.0
    CycleUnit.YEAR -> 1.0
} / cycleCount

fun Sub.isTrial(today: LocalDate) = freeTrial && cancelledOn == null && !today.isAfter(startDate)

fun priceOn(sub: Sub, changes: List<PriceChange>, date: LocalDate): Double =
    changes.filter { it.subId == sub.id && !it.date.isAfter(date) }.maxByOrNull { it.date }?.price ?: sub.price

fun Sub.cycleLabel(): String {
    val unit = cycleUnit.name.lowercase()
    return if (cycleCount == 1) "Every $unit" else "Every $cycleCount ${unit}s"
}

fun Sub.cycleGroup(): String = if (cycleCount != 1) cycleLabel() else when (cycleUnit) {
    CycleUnit.DAY -> "Daily"
    CycleUnit.WEEK -> "Weekly"
    CycleUnit.MONTH -> "Monthly"
    CycleUnit.YEAR -> "Yearly"
}

fun daysPhrase(today: LocalDate, date: LocalDate): String = when (val n = ChronoUnit.DAYS.between(today, date)) {
    0L -> "today"
    1L -> "tomorrow"
    else -> "in $n days"
}
