package com.dues.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BillingTest {
    private fun sub(start: String, unit: CycleUnit = CycleUnit.MONTH, count: Int = 1, cancelled: String? = null) =
        Sub(name = "x", price = 10.0, startDate = LocalDate.parse(start), cycleUnit = unit, cycleCount = count,
            cancelledOn = cancelled?.let(LocalDate::parse))

    @Test fun monthEndAnchorStaysOnMonthEnd() {
        val s = sub("2025-01-31")
        assertEquals(LocalDate.parse("2025-02-28"), s.nextPayment(LocalDate.parse("2025-02-01")))
        assertEquals(LocalDate.parse("2025-03-31"), s.nextPayment(LocalDate.parse("2025-03-01")))
    }

    @Test fun nextPaymentIsTodayWhenDue() {
        assertEquals(LocalDate.parse("2026-05-15"), sub("2020-01-15").nextPayment(LocalDate.parse("2026-05-15")))
    }

    @Test fun futureStartIsNextPayment() {
        assertEquals(LocalDate.parse("2030-01-01"), sub("2030-01-01").nextPayment(LocalDate.parse("2026-01-01")))
    }

    @Test fun biweeklyAndDailyFarInPast() {
        assertEquals(LocalDate.parse("2026-01-14"), sub("2025-12-31", CycleUnit.WEEK, 2).nextPayment(LocalDate.parse("2026-01-02")))
        assertEquals(LocalDate.parse("2026-06-01"), sub("2019-03-04", CycleUnit.DAY).nextPayment(LocalDate.parse("2026-06-01")))
    }

    @Test fun cancelledHasNoNextAndStopsPayments() {
        val s = sub("2025-01-10", cancelled = "2025-03-01")
        assertNull(s.nextPayment(LocalDate.parse("2025-02-01")))
        assertEquals(
            listOf("2025-01-10", "2025-02-10").map(LocalDate::parse),
            s.paymentsIn(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-12-31")),
        )
    }

    @Test fun yearlyCost() {
        assertEquals(12.0, sub("2025-01-01").paymentsPerYear(), 0.0)
        assertEquals(26.0, sub("2025-01-01", CycleUnit.WEEK, 2).paymentsPerYear(), 0.0)
        assertEquals(4.0, sub("2025-01-01", CycleUnit.MONTH, 3).paymentsPerYear(), 0.0)
    }

    @Test fun priceChangesApplyFromTheirDate() {
        val s = sub("2025-01-01").copy(id = 7)
        val changes = listOf(PriceChange(subId = 7, date = LocalDate.parse("2025-06-01"), price = 12.0))
        assertEquals(10.0, priceOn(s, changes, LocalDate.parse("2025-05-31")), 0.0)
        assertEquals(12.0, priceOn(s, changes, LocalDate.parse("2025-06-01")), 0.0)
    }
}
