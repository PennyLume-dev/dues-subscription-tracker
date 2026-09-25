package com.dues.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the merged India prices (PremiumKing index, Aug 2026) in catalog.json. */
class CatalogIndiaTest {
    private val services = JSONObject(File("src/main/assets/catalog.json").readText()).getJSONArray("services")

    private fun inPrices(service: String): Map<String, Double> {
        for (i in 0 until services.length()) {
            val s = services.getJSONObject(i)
            if (s.getString("n") != service) continue
            val plans = s.getJSONArray("p")
            return (0 until plans.length()).map { plans.getJSONObject(it) }
                .filter { it.getJSONObject("pr").has("IN") }
                .associate { "${it.getString("n")}/${it.getString("u")}${it.getInt("k")}" to it.getJSONObject("pr").getDouble("IN") }
        }
        error("no $service")
    }

    @Test fun netflixIndiaHasFourPlans() {
        assertEquals(mapOf("Standard/month1" to 499.0, "Premium/month1" to 649.0, "Mobile/month1" to 149.0, "Basic/month1" to 199.0), inPrices("Netflix"))
    }

    @Test fun primeTiersStaySeparate() {
        val p = inPrices("Amazon Prime")
        assertEquals(1499.0, p["Prime Annual/year1"])
        assertEquals(799.0, p["Prime Lite Annual/year1"])
        assertEquals(399.0, p["Prime Shopping Edition Annual/year1"])
    }

    @Test fun zee5SportsIsItsOwnPlan() {
        val p = inPrices("ZEE5")
        assertEquals(299.0, p["Premium - All Access/month1"])
        assertEquals(399.0, p["All Access + Sports/month1"])
    }

    @Test fun usdBilledAiToolsHaveNoInrPrice() {
        assertTrue(inPrices("Claude").isEmpty())
    }
}
