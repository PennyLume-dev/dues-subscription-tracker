package com.dues.app.data

import android.content.Context
import org.json.JSONObject

/**
 * Verified plan prices from assets/catalog.json for the PRICED_COUNTRIES; where a plan has no local
 * price, users see a converted US estimate as a hint.
 */
data class Plan(
    val name: String,
    val unit: CycleUnit,
    val count: Int,
    val family: Boolean,
    val student: Boolean,
    val trialDays: Int,
    /** Country code -> price in that country's currency. */
    val prices: Map<String, Double>,
    /** True for derived plans (e.g. yearly = 12x monthly) whose price isn't officially listed. */
    val estimated: Boolean = false,
    /** Countries where this plan isn't sold. */
    val unavailableIn: Set<String> = emptySet(),
) {
    fun perYear(): Double = when (unit) {
        CycleUnit.DAY -> 365.0
        CycleUnit.WEEK -> 52.0
        CycleUnit.MONTH -> 12.0
        CycleUnit.YEAR -> 1.0
    } / count
}

data class Service(val name: String, val domain: String, val category: String, val plans: List<Plan>) {
    /** Everyday plan: monthly, not family/student. */
    val defaultPlan: Plan
        get() = plans.firstOrNull { it.unit == CycleUnit.MONTH && !it.family && !it.student } ?: plans.first()
}

/** Currency each verified country's prices are recorded in. */
val PRICED_COUNTRIES = mapOf(
    "US" to "USD", "IN" to "INR", "GB" to "GBP", "DE" to "EUR", "FR" to "EUR", "CA" to "CAD",
    "AU" to "AUD", "SG" to "SGD", "AE" to "AED", "BR" to "BRL", "JP" to "JPY", "MX" to "MXN",
)

const val CATALOG_URL = "https://dues-app.vercel.app/catalog.json"
private const val CATALOG_FILE = "catalog.json"

private fun catalogVersion(text: String): String = JSONObject(text).optString("v")

/** Downloaded catalog if it's newer than the bundled one and parses; otherwise the bundled asset. */
fun loadCatalog(ctx: Context): List<Service> {
    val bundled = ctx.assets.open(CATALOG_FILE).bufferedReader().use { it.readText() }
    val saved = java.io.File(ctx.filesDir, CATALOG_FILE).takeIf { it.exists() }?.readText()
    if (saved != null) {
        runCatching { if (catalogVersion(saved) > catalogVersion(bundled)) return parseCatalog(saved).also { require(it.isNotEmpty()) } }
    }
    return parseCatalog(bundled)
}

/** Stores [text] for the next launch if it's a valid catalog newer than what we have. Returns true if saved. */
fun saveCatalogIfNewer(ctx: Context, text: String): Boolean {
    val current = java.io.File(ctx.filesDir, CATALOG_FILE).takeIf { it.exists() }?.readText()
        ?: ctx.assets.open(CATALOG_FILE).bufferedReader().use { it.readText() }
    val ok = runCatching { catalogVersion(text) > catalogVersion(current) && parseCatalog(text).size >= 10 }.getOrDefault(false)
    if (ok) {
        val tmp = java.io.File(ctx.filesDir, "$CATALOG_FILE.tmp")
        tmp.writeText(text)
        tmp.renameTo(java.io.File(ctx.filesDir, CATALOG_FILE))
    }
    return ok
}

fun parseCatalog(text: String): List<Service> {
    val root = JSONObject(text)
    val arr = root.getJSONArray("services")
    return (0 until arr.length()).map { i ->
        val s = arr.getJSONObject(i)
        val plans = s.getJSONArray("p")
        Service(
            name = s.getString("n"),
            domain = s.getString("d"),
            category = s.getString("c"),
            plans = (0 until plans.length()).map { j ->
                val p = plans.getJSONObject(j)
                val pr = p.getJSONObject("pr")
                Plan(
                    name = p.getString("n"),
                    unit = runCatching { CycleUnit.valueOf(p.getString("u").uppercase()) }.getOrDefault(CycleUnit.MONTH),
                    count = p.optInt("k", 1),
                    family = p.optBoolean("fam"),
                    student = p.optBoolean("stu"),
                    trialDays = p.optInt("t"),
                    prices = pr.keys().asSequence().associateWith { pr.getDouble(it) },
                    unavailableIn = p.optJSONArray("x")?.let { x -> (0 until x.length()).map(x::getString).toSet() } ?: emptySet(),
                )
            },
        )
    }.filter { it.plans.isNotEmpty() }
}

fun soldIn(s: Service, country: String) = s.plans.any { country !in it.unavailableIn }

/** Plans sold in [country] that have a local price or a US price to estimate from. */
fun plansFor(s: Service, country: String): List<Plan> =
    s.plans.filter { country !in it.unavailableIn && (it.prices.isEmpty() || country in it.prices || "US" in it.prices) }
        .ifEmpty { s.plans.filter { country !in it.unavailableIn } }
        .ifEmpty { s.plans }

/** Shown during onboarding. */
val POPULAR_NAMES = listOf(
    "Netflix", "Amazon Prime", "Disney+", "Spotify", "YouTube Premium", "ChatGPT",
    "JioHotstar", "Apple Music", "Max", "Google One / Google AI plans", "Claude", "Microsoft 365",
)

val EMAIL_SEARCH_TERMS = listOf("Renewal", "Subscription confirmation", "Receipt", "Free trial", "Your plan", "Invoice")
