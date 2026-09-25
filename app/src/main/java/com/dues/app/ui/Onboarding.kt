package com.dues.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dues.app.AppVM
import com.dues.app.R
import com.dues.app.data.PRICED_COUNTRIES
import java.util.Currency
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingScreen(vm: AppVM, onDone: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val picked = remember { androidx.compose.runtime.mutableStateMapOf<String, com.dues.app.data.Plan>() }
    BackHandler(step > 0) { step-- }

    fun finish(add: Boolean) {
        if (add) vm.addFromOnboarding(vm.popular.mapNotNull { s -> picked[s.name]?.let { s to it } })
        vm.prefs.onboarded = true
        onDone()
    }

    Backdrop {
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "onboarding") { s ->
                when (s) {
                    0 -> Hero(
                        title = "Welcome to Dues",
                        body = "Find every subscription, see what you really spend, and never get caught by a renewal again.",
                        button = "Start tracking",
                        onNext = { step = 1 },
                    ) { LogoOrbit(vm.popular) }

                    1 -> CountryStep(vm) { step = 2 }

                    2 -> Hero(
                        title = "Small charges add up quietly",
                        body = "A few dollars here and there becomes a surprising yearly number. Let's find yours.",
                        button = "Show me",
                        onNext = { step = 3 },
                    ) { Penny(230.dp, pose = R.drawable.penny_think) }

                    3 -> PickStep(vm, picked, onSkip = { finish(false) }, onNext = { if (picked.isEmpty()) finish(false) else step = 4 })

                    4 -> {
                        val yearly = vm.popular.sumOf { s -> picked[s.name]?.let { (vm.priceOrEstimate(it) ?: 0.0) * it.perYear() } ?: 0.0 }
                        Hero(
                            title = "",
                            body = "And that's only the ones you remember. Dues finds the rest and nudges you before every charge.",
                            button = "Continue",
                            onNext = { finish(true) },
                            headline = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("That's", style = MaterialTheme.typography.headlineMedium, color = TextHi)
                                    Text(
                                        vm.money(yearly),
                                        style = TextStyle(brush = TotalBrush, fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 52.sp, letterSpacing = (-1.5).sp),
                                    )
                                    Text("a year", style = MaterialTheme.typography.headlineMedium, color = TextHi)
                                }
                            },
                        ) { Penny(240.dp, pose = R.drawable.penny_celebrate) }
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun Hero(
    title: String,
    body: String,
    button: String,
    onNext: () -> Unit,
    headline: (@Composable () -> Unit)? = null,
    art: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { art() }
        if (headline != null) headline()
        else Text(title, style = MaterialTheme.typography.headlineLarge, color = TextHi, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(body, color = TextLo, fontSize = 17.sp, textAlign = TextAlign.Center, lineHeight = 25.sp)
        Spacer(Modifier.height(32.dp))
        BigButton(button, onNext)
    }
}

/** Popular services orbiting Penny on faint rings. */
@Composable
private fun LogoOrbit(popular: List<com.dues.app.data.Service>) {
    Box(Modifier.size(330.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            listOf(0.36f, 0.49f).forEach { f -> drawCircle(Color.White.copy(alpha = 0.07f), size.minDimension * f, style = Stroke(1.dp.toPx())) }
        }
        Penny(150.dp, pose = R.drawable.penny_wave)
        popular.take(10).forEachIndexed { i, s ->
            val r = if (i % 2 == 0) 160 else 118
            val a = Math.toRadians(i * 36.0 - 80)
            Box(Modifier.offset(x = (r * cos(a)).dp, y = (r * sin(a)).dp).glow(Gold, 0.12f, 0.8f)) {
                Logo(s.name, s.domain, "", if (i % 2 == 0) 46.dp else 38.dp)
            }
        }
    }
}

@Composable
private fun CountryStep(vm: AppVM, onNext: () -> Unit) {
    var selected by remember { mutableStateOf(vm.prefs.country) }
    var name by remember { mutableStateOf(vm.prefs.name) }
    val cur = COUNTRIES.firstOrNull { it.code == selected }?.currency
    Column(Modifier.fillMaxSize()) {
        Text("Where do you live?", style = MaterialTheme.typography.headlineLarge, color = TextHi, modifier = Modifier.padding(top = 12.dp))
        Text("We'll show every price in your currency.", color = TextLo, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        OutlinedTextField(
            name, { name = it.take(24) }, Modifier.fillMaxWidth().padding(bottom = 12.dp), singleLine = true, shape = RoundedCornerShape(18.dp),
            placeholder = { Text("Your first name (optional)", color = TextLo) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Glass, unfocusedContainerColor = Glass,
                focusedBorderColor = Gold.copy(alpha = 0.6f), unfocusedBorderColor = Hairline, cursorColor = Gold,
            ),
        )
        CountryPicker(selected, { selected = it }, suggested = Locale.getDefault().country, modifier = Modifier.weight(1f))
        Spacer(Modifier.height(12.dp))
        BigButton(if (cur != null) "Continue · ${currencyLabel(cur)}" else "Continue", {
            vm.setCountry(selected)
            vm.prefs.name = name.trim()
            onNext()
        })
    }
}

data class Country(val code: String, val name: String, val currency: String)

val COUNTRIES: List<Country> by lazy {
    Locale.getISOCountries().mapNotNull { cc ->
        val l = Locale("", cc)
        runCatching { Currency.getInstance(l).currencyCode }.getOrNull()?.let { Country(cc, l.displayCountry, it) }
    }.sortedBy { it.name }
}

fun currencyLabel(code: String): String {
    val sym = runCatching { Currency.getInstance(code).symbol }.getOrDefault("")
    return if (sym.isBlank() || sym == code) code else "$code $sym"
}

/** Searchable country list with flags and currencies; [suggested] floats to the top. */
@Composable
fun CountryPicker(selected: String, onSelect: (String) -> Unit, suggested: String, modifier: Modifier = Modifier) {
    var q by remember { mutableStateOf("") }
    val filtered = COUNTRIES.filter { q.isBlank() || it.name.contains(q.trim(), true) || it.currency.contains(q.trim(), true) }
    val top = if (q.isBlank()) COUNTRIES.filter { it.code == suggested || it.code == selected }.distinct() else emptyList()
    Column(modifier) {
        OutlinedTextField(
            q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextLo) }, placeholder = { Text("Search country or currency", color = TextLo) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Glass, unfocusedContainerColor = Glass,
                focusedBorderColor = Gold.copy(alpha = 0.6f), unfocusedBorderColor = Hairline, cursorColor = Gold,
            ),
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
            if (top.isNotEmpty()) {
                item { Text("Suggested", color = TextLo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(4.dp)) }
                items(top, key = { "s" + it.code }) { CountryRow(it, it.code == selected) { onSelect(it.code) } }
                item { Text("All countries", color = TextLo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)) }
            }
            items(filtered, key = { it.code }) { CountryRow(it, it.code == selected) { onSelect(it.code) } }
        }
    }
}

@Composable
private fun CountryRow(c: Country, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) Gold.copy(alpha = 0.12f) else Glass)
            .border(1.dp, if (selected) Gold.copy(alpha = 0.7f) else Hairline, shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(GlassHi), contentAlignment = Alignment.Center) {
            Text(flagOf(c.code), fontSize = 22.sp)
        }
        Spacer(Modifier.width(14.dp))
        Text(c.name, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextHi)
        Text(currencyLabel(c.currency), color = if (selected) Gold else TextLo, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 10.dp))
        CheckDot(selected, 22.dp)
    }
}

@Composable
private fun PickStep(vm: AppVM, picked: MutableMap<String, com.dues.app.data.Plan>, onSkip: () -> Unit, onNext: () -> Unit) {
    var choosing by remember { mutableStateOf<com.dues.app.data.Service?>(null) }
    choosing?.let { svc -> PlanSheet(vm, svc, { choosing = null }, null) { p -> picked[svc.name] = p; choosing = null } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Pill("Skip", onSkip) }
        Text("Which do you pay for?", style = MaterialTheme.typography.headlineLarge, color = TextHi, modifier = Modifier.padding(top = 8.dp))
        Text(
            if (vm.prefs.country in PRICED_COUNTRIES) "Current prices in your country. Pick your exact plan later." else "Estimated prices. You can set what you actually pay.",
            color = TextLo, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
        )
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            items(vm.popular) { s ->
                val plan = picked[s.name]
                val sel = plan != null
                val shape = RoundedCornerShape(22.dp)
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(if (sel) Gold.copy(alpha = 0.12f) else Glass)
                        .border(1.dp, if (sel) Gold.copy(alpha = 0.7f) else Hairline, shape)
                        .clickable { if (sel) picked.remove(s.name) else if (vm.plansFor(s).size > 1) choosing = s else picked[s.name] = vm.defaultPlan(s) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Logo(s.name, s.domain, "")
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextHi)
                        Text(plan?.name ?: "Tap to choose your plan", fontSize = 12.sp, color = if (plan != null) Gold else TextLo)
                    }
                    Text(vm.planLabel(plan ?: vm.defaultPlan(s)), color = TextLo, fontSize = 15.sp)
                    Spacer(Modifier.width(12.dp))
                    CheckDot(sel)
                }
            }
        }
        BigButton(if (picked.isEmpty()) "None of these" else "See my yearly total", onNext, secondary = picked.isEmpty())
    }
}
