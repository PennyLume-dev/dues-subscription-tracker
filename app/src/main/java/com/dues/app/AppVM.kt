package com.dues.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dues.app.data.AppDb
import com.dues.app.data.DEFAULT_CATEGORY
import com.dues.app.data.DEFAULT_LIST
import com.dues.app.data.PriceChange
import com.dues.app.data.Service
import com.dues.app.data.Plan
import com.dues.app.data.POPULAR_NAMES
import com.dues.app.data.PRICED_COUNTRIES
import com.dues.app.data.CycleUnit
import com.dues.app.data.loadCatalog
import com.dues.app.data.CATALOG_URL
import com.dues.app.data.saveCatalogIfNewer
import com.dues.app.data.Sub
import com.dues.app.data.Tag
import com.dues.app.data.TagKind
import com.dues.app.data.cycleLabel
import com.dues.app.data.nextPayment
import com.dues.app.data.tag
import com.dues.app.data.withTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** SharedPreferences backed by Compose state so the UI recomposes on change. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("dues", Context.MODE_PRIVATE)

    var onboarded by pref("onboarded", false)
    /** Optional first name for the home greeting; never leaves the device. */
    var name by pref("name", "")
    var country by pref("country", Locale.getDefault().country.ifBlank { "US" })
    var currency by pref("currency", localCurrency())
    /** Cached USD -> [rateCurrency] exchange rate, as text. */
    var rate by pref("rate", "")
    var rateCurrency by pref("rateCurrency", "")
    var notifications by pref("notifications", false)
    var guideDone by pref("guideDone", false)
    var sort by pref("sort", "next")      // next | name | price
    var group by pref("group", "status")  // status | category | cycle
    var list by pref("list", "All")

    @Suppress("UNCHECKED_CAST")
    private fun <T> pref(key: String, default: T) = object : ReadWriteProperty<Any?, T> {
        private val state = mutableStateOf(
            when (default) {
                is Boolean -> sp.getBoolean(key, default)
                is String -> sp.getString(key, default)
                else -> error("unsupported pref type")
            } as T
        )

        override fun getValue(thisRef: Any?, property: KProperty<*>) = state.value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            state.value = value
            sp.edit().apply {
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is String -> putString(key, value)
                }
            }.apply()
        }
    }

    private fun localCurrency() = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("USD")
}

val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

/** Public legal/support pages required by app stores (hosted from /site on Vercel). */
object Legal {
    const val PRIVACY_URL = "https://dues-app.vercel.app/privacy"
    const val TERMS_URL = "https://dues-app.vercel.app/terms"
    const val SUPPORT_URL = "https://dues-app.vercel.app/support"
    const val SUPPORT_EMAIL = "pennylume@proton.me"
    /** Donation options live on the website so they can change without an app update. */
    const val DONATE_URL = "https://dues-app.vercel.app/donate"
    const val REPO_URL = "https://github.com/PennyLume-dev/dues-subscription-tracker"
}

fun round2(v: Double) = Math.round(v * 100) / 100.0

class AppVM(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    private val dao = AppDb.get(app).dao()

    val subs = dao.subs().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val changes = dao.changes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val tags = dao.tags().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Prefilled subscription handed from the catalog to the edit form. */
    var draft by mutableStateOf<Sub?>(null)

    init {
        refreshRate()
        refreshCatalog()
    }

    /** Fetches the hosted price list; a newer valid one is saved and used from the next launch. */
    private fun refreshCatalog() = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val c = URL(CATALOG_URL).openConnection() as HttpURLConnection
            c.connectTimeout = 8000
            c.readTimeout = 8000
            saveCatalogIfNewer(getApplication(), c.inputStream.bufferedReader().use { it.readText() })
        }
    }

    fun money(v: Double): String = NumberFormat.getCurrencyInstance().apply {
        currency = runCatching { Currency.getInstance(prefs.currency) }.getOrDefault(Currency.getInstance("USD"))
    }.format(v)

    /** USD -> preferred currency; 1.0 until a live rate for this currency has been fetched. */
    val usdRate: Double get() = if (prefs.rateCurrency == prefs.currency) prefs.rate.toDoubleOrNull() ?: 1.0 else 1.0
    val hasRate: Boolean get() = prefs.currency == "USD" || (prefs.rateCurrency == prefs.currency && prefs.rate.isNotEmpty())

    /** Catalog prices are USD; show and store them in the user's currency. */
    fun local(usd: Double) = if (hasRate) usd * usdRate else usd
    fun localMoney(usd: Double) = if (hasRate) money(local(usd)) else "$" + "%.2f".format(Locale.US, usd)


    fun setCountry(cc: String) {
        prefs.country = cc
        runCatching { Currency.getInstance(Locale("", cc)).currencyCode }.getOrNull()?.let { setCurrency(it) }
    }

    /** One-line result of the last currency switch, shown by the region screen. */
    var conversionNote by mutableStateOf<String?>(null)

    /** Switches currency and converts every saved price (and price change) at the live rate. */
    fun setCurrency(code: String) {
        val old = prefs.currency
        if (old == code) return
        prefs.currency = code
        viewModelScope.launch {
            val rates = fetchRates()
            rates?.get(code)?.let { cacheRate(code, it) }
            val subsNow = subs.value
            val changesNow = changes.value
            if (subsNow.isEmpty()) return@launch
            val from = rates?.get(old)
            val to = rates?.get(code)
            if (from == null || to == null) {
                conversionNote = "Offline: saved prices weren't converted from $old. Edit them if needed."
                return@launch
            }
            val f = to / from
            dao.saveAll(subsNow.map { it.copy(price = round2(it.price * f)) })
            dao.saveChanges(changesNow.map { it.copy(price = round2(it.price * f)) })
            conversionNote = "Converted ${subsNow.size} subscription${if (subsNow.size == 1) "" else "s"} from $old to $code."
        }
    }

    /** ponytail: free, keyless daily rates (USD base); null when offline. */
    private suspend fun fetchRates(): Map<String, Double>? = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL("https://open.er-api.com/v6/latest/USD").openConnection() as HttpURLConnection
            c.connectTimeout = 8000
            c.readTimeout = 8000
            val r = c.inputStream.bufferedReader().use { JSONObject(it.readText()).getJSONObject("rates") }
            r.keys().asSequence().associateWith { r.getDouble(it) }
        }.getOrNull()
    }

    private fun cacheRate(cur: String, r: Double) {
        prefs.rate = r.toString()
        prefs.rateCurrency = cur
    }

    fun refreshRate() {
        val cur = prefs.currency
        if (cur == "USD") return
        viewModelScope.launch {
            fetchRates()?.get(cur)?.let { if (prefs.currency == cur) cacheRate(cur, it) }
        }
    }

    val catalog: List<Service> by lazy { loadCatalog(app) }

    // Parse off the main thread before first use. Must follow the property: init blocks run in declaration order.
    init { viewModelScope.launch(Dispatchers.Default) { catalog } }
    val popular: List<Service> get() = POPULAR_NAMES.mapNotNull { n -> catalog.firstOrNull { it.name == n } }.filter(::sold)

    /** Hides services not offered in the user's country (e.g. Hulu outside the US). */
    fun sold(s: Service) = com.dues.app.data.soldIn(s, prefs.country)

    /** Verified price, only when the user's country is covered and they use its currency. */
    fun exactPrice(plan: Plan): Double? =
        PRICED_COUNTRIES[prefs.country]?.takeIf { it == prefs.currency }?.let { plan.prices[prefs.country] }

    /** US price converted live; a hint, never stored silently as fact. */
    fun estimate(plan: Plan): Double? = plan.prices["US"]?.takeIf { hasRate }?.let { it * usdRate }

    fun priceOrEstimate(plan: Plan) = exactPrice(plan) ?: estimate(plan)

    /** Plans sold in the user's country that have a local price or a US price to estimate from. */
    fun plansFor(s: Service): List<Plan> = com.dues.app.data.plansFor(s, prefs.country)

    /** Everyday monthly plan, preferring one with a verified local price over a converted estimate. */
    fun defaultPlan(s: Service): Plan = plansFor(s).let { l ->
        val everyday = l.filter { it.unit == CycleUnit.MONTH && !it.family && !it.student }
        everyday.firstOrNull { exactPrice(it) != null } ?: everyday.firstOrNull() ?: l.first()
    }

    fun planLabel(plan: Plan): String =
        exactPrice(plan)?.let { (if (plan.estimated) "≈ " else "") + money(it) } ?: estimate(plan)?.let { "≈ " + money(it) } ?: "Set price"

    /** Shown under the price field when we have no verified price for the user's country. */
    var priceHint by mutableStateOf<String?>(null)

    fun draftFrom(service: Service?, plan: Plan? = null, name: String = "") {
        val p = plan ?: service?.let(::defaultPlan)
        val exact = p?.let(::exactPrice)
        priceHint = when {
            p == null || exact != null -> null
            else -> estimate(p)?.let { "No verified price for your country yet. Check Google Play or your receipt (estimate ≈ ${money(it)})." }
                ?: "Enter the price shown in Google Play or on your receipt."
        }
        draft = Sub(
            name = service?.name ?: name,
            price = exact?.let(::round2) ?: 0.0,
            domain = service?.domain ?: "",
            category = service?.category ?: DEFAULT_CATEGORY,
            startDate = LocalDate.now(),
            cycleUnit = p?.unit ?: com.dues.app.data.CycleUnit.MONTH,
            cycleCount = p?.count ?: 1,
            notes = if (p != null && service != null && service.plans.size > 1) "${p.name} plan" else "",
        )
    }

    fun save(s: Sub) = viewModelScope.launch {
        dao.save(s)
        // Keep tags in sync if a sub names one that doesn't exist yet.
        dao.insert(Tag(TagKind.LIST, s.listName))
        dao.insert(Tag(TagKind.CATEGORY, s.category))
        if (s.paymentMethod.isNotBlank()) dao.insert(Tag(TagKind.PAYMENT, s.paymentMethod))
    }

    fun delete(s: Sub) = viewModelScope.launch {
        dao.deleteChangesOf(s.id)
        dao.delete(s)
    }

    fun setCancelled(s: Sub, cancelled: Boolean) =
        save(s.copy(cancelledOn = if (cancelled) LocalDate.now() else null))

    fun addChange(subId: Long, date: LocalDate, price: Double) =
        viewModelScope.launch { dao.insert(PriceChange(subId = subId, date = date, price = price)) }

    fun deleteChange(p: PriceChange) = viewModelScope.launch { dao.delete(p) }

    fun addTag(kind: String, name: String) = viewModelScope.launch {
        if (name.isNotBlank()) dao.insert(Tag(kind, name.trim()))
    }

    fun renameTag(kind: String, old: String, new: String) = viewModelScope.launch {
        val name = new.trim()
        if (name.isEmpty() || name == old || tags.value.any { it.kind == kind && it.name == name }) return@launch
        dao.renameTag(kind, old, name)
        dao.saveAll(subs.value.filter { it.tag(kind) == old }.map { it.withTag(kind, name) })
        if (kind == TagKind.LIST && prefs.list == old) prefs.list = name
    }

    /** Subs in a deleted tag fall back to the default one. */
    fun deleteTag(kind: String, name: String) = viewModelScope.launch {
        val fallback = when (kind) {
            TagKind.LIST -> DEFAULT_LIST
            TagKind.CATEGORY -> DEFAULT_CATEGORY
            else -> ""
        }
        dao.saveAll(subs.value.filter { it.tag(kind) == name }.map { it.withTag(kind, fallback) })
        dao.delete(Tag(kind, name))
        if (kind == TagKind.LIST && prefs.list == name) prefs.list = "All"
    }

    fun moveSubs(ids: Set<Long>, kind: String, to: String) = viewModelScope.launch {
        dao.saveAll(subs.value.filter { it.id in ids }.map { it.withTag(kind, to) })
    }

    fun addFromOnboarding(picked: List<Pair<Service, Plan>>) = viewModelScope.launch {
        val start = LocalDate.now().plusMonths(1)
        dao.saveAll(picked.map { (it, plan) ->
            val exact = exactPrice(plan)
            Sub(
                name = it.name, price = round2(exact ?: estimate(plan) ?: 0.0), domain = it.domain, category = it.category,
                startDate = start, cycleUnit = plan.unit, cycleCount = plan.count,
                notes = if (exact == null || plan.estimated) "Estimated price. Tap Edit to set what you pay." else "${plan.name} plan",
            )
        })
    }

    fun deleteAll() = viewModelScope.launch {
        dao.deleteAllChanges()
        dao.deleteAllSubs()
    }

    fun csv(): String {
        val today = LocalDate.now()
        fun esc(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
        return buildString {
            appendLine("Name,Price,Cycle,Next payment,Category,List,Payment method,Status")
            subs.value.forEach { s ->
                val status = if (s.cancelledOn != null) "Cancelled" else if (s.freeTrial) "Free trial" else "Active"
                appendLine(
                    listOf(
                        s.name, "%.2f".format(Locale.US, s.price), s.cycleLabel(), s.nextPayment(today)?.format(DATE) ?: "",
                        s.category, s.listName, s.paymentMethod, status,
                    ).joinToString(",") { esc(it) }
                )
            }
        }
    }
}
