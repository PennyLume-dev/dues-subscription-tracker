package com.dues.app

import com.dues.app.data.parseCatalog
import com.dues.app.data.plansFor
import com.dues.app.data.soldIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the 12-country merge (tools/merge_world_prices.py) and per-country plan filtering. */
class CatalogWorldTest {
    private val catalog = parseCatalog(File("src/main/assets/catalog.json").readText())
    private fun svc(name: String) = catalog.first { it.name == name }
    private fun plan(service: String, name: String, unit: String = "MONTH", count: Int = 1) =
        svc(service).plans.first { it.name == name && it.unit.name == unit && it.count == count }

    @Test fun netflixUkPlansAndPrices() {
        val names = plansFor(svc("Netflix"), "GB").map { it.name }
        assertTrue("Standard" in names && "Premium" in names && "Standard with ads" in names)
        assertFalse("Mobile" in names)  // India-only
        assertEquals(12.99, plan("Netflix", "Standard").prices["GB"])
        assertEquals(18.99, plan("Netflix", "Premium").prices["GB"])
    }

    @Test fun unavailablePlanHiddenOnlyThere() {
        assertTrue("IN" in plan("Netflix", "Standard with ads").unavailableIn)
        assertFalse(plansFor(svc("Netflix"), "IN").any { it.name.startsWith("Standard with ads") })
        assertEquals(setOf("Standard", "Premium", "Mobile", "Basic"),
            plansFor(svc("Netflix"), "IN").map { it.name }.toSet())
    }

    @Test fun handVerifiedPricesWinOverDataset() {
        assertEquals(30.0, plan("Grammarly", "Pro").prices["US"])
        assertEquals(11.99, plan("Deezer", "Premium").prices["US"])
        assertEquals(20.0, plan("ChatGPT", "Plus").prices["US"])
        assertEquals(2199.0, plan("JioHotstar", "Premium", "YEAR").prices["IN"])
        assertEquals(119.0, plan("YouTube Music", "Individual").prices["IN"])
        assertEquals(17.99, plan("PlayStation Plus", "Premium").prices["US"])
    }

    @Test fun nonLocalCurrencyRowsNotStored() {
        assertNull(plan("Calm", "Premium Monthly").prices["DE"])  // dataset had USD for Germany
        assertNull(plan("Dropbox", "Plus Annual", "YEAR").prices["US"])  // dataset's $9.99 was a per-month rate
        assertNull(plan("Claude", "Pro").prices["GB"])  // App Store SKU; web bills USD
    }

    @Test fun mergedServicesKeepOurNames() {
        assertEquals(69, catalog.size)
        assertTrue(catalog.none { it.name == "Amazon Prime bundle" || it.name == "Google One storage" })
    }

    @Test fun usOnlyServicesHiddenAbroad() {
        assertTrue(soldIn(svc("Hulu"), "US"))
        assertFalse(soldIn(svc("Hulu"), "GB"))
        assertFalse(soldIn(svc("Disney+"), "IN"))
        assertTrue(soldIn(svc("Disney+"), "GB"))
    }

    @Test fun duplicatePlansFolded() {
        assertEquals(1, svc("Apple Music").plans.count { it.name == "Individual" && it.unit.name == "MONTH" })
    }
}
