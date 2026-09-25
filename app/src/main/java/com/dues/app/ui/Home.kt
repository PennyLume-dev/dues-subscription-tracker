package com.dues.app.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.dues.app.AppVM
import com.dues.app.DATE
import com.dues.app.R
import com.dues.app.data.Sub
import com.dues.app.data.TagKind
import com.dues.app.data.cycleGroup
import com.dues.app.data.cycleLabel
import com.dues.app.data.daysPhrase
import com.dues.app.data.isTrial
import com.dues.app.data.nextPayment
import com.dues.app.data.paymentsIn
import com.dues.app.data.paymentsPerYear
import com.dues.app.data.priceOn
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

fun openAdd(vm: AppVM, nav: NavController) = nav.navigate("add")

@Composable
fun MainScreen(vm: AppVM, nav: NavController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        when (tab) {
            0 -> Backdrop { HomeTab(vm, nav) }
            1 -> Backdrop(dim = 0.55f) { CalendarTab(vm, nav) }
            else -> SettingsTab(vm, nav)
        }
        BottomPill(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
    }
}

fun statusLine(s: Sub, today: LocalDate): String {
    s.cancelledOn?.let { return "Cancelled on ${it.format(DATE)}" }
    val next = s.nextPayment(today) ?: return ""
    val verb = if (s.isTrial(today)) "Trial ends" else "Renews"
    return "$verb ${daysPhrase(today, next)} · ${next.format(DATE)}"
}

/** Coral for trials, gold when a charge is within 3 days. */
fun statusAccent(s: Sub, today: LocalDate): Color? {
    if (s.cancelledOn != null) return null
    if (s.isTrial(today)) return Coral
    val next = s.nextPayment(today) ?: return null
    return if (ChronoUnit.DAYS.between(today, next) <= 3) Gold else null
}

private val GROUPS = listOf("status" to "Status", "category" to "Category", "list" to "List", "cycle" to "Cycle")
private val SORTS = listOf("next" to "Next", "name" to "Name", "price" to "Price")

@Composable
private fun HomeTab(vm: AppVM, nav: NavController) {
    val subs by vm.subs.collectAsStateWithLifecycle()
    val changes by vm.changes.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val p = vm.prefs
    val today = remember { LocalDate.now() }

    val visible = subs.filter { p.list == "All" || it.listName == p.list }
    val live = visible.filter { it.cancelledOn == null }
    fun cur(s: Sub) = priceOn(s, changes, today)
    fun yearly(s: Sub) = cur(s) * s.paymentsPerYear()
    val paying = live.filter { !it.isTrial(today) }
    val total = paying.sumOf(::yearly)
    val byCategory = paying.groupBy { it.category }
        .map { (c, l) -> c to l.sumOf(::yearly) }.sortedByDescending { it.second }

    val sorter: Comparator<Sub> = when (p.sort) {
        "name" -> compareBy { it.name.lowercase() }
        "price" -> compareByDescending { yearly(it) }
        else -> compareBy { it.nextPayment(today) ?: LocalDate.MAX }
    }
    val sections = when (p.group) {
        "category" -> live.groupBy { it.category }.toSortedMap().toList()
        "list" -> live.groupBy { it.listName }.toSortedMap().toList()
        "cycle" -> live.groupBy { it.cycleGroup() }.toList()
        else -> listOf("Free trials" to live.filter { it.isTrial(today) }, "Active" to live.filter { !it.isTrial(today) })
    } + ("Cancelled" to visible.filter { it.cancelledOn != null })

    var listMenu by remember { mutableStateOf(false) }
    var groupDialog by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                CircleButton(Icons.Default.Add, "Add subscription", { openAdd(vm, nav) }, gold = true)
            }
        }
        if (p.name.isNotBlank()) item {
            val hour = java.time.LocalTime.now().hour
            val part = when (hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; in 17..21 -> "Good evening"; else -> "Hello" }
            Text("$part, ${p.name}", style = MaterialTheme.typography.headlineSmall, color = TextHi)
        }
        item {
            Box(Modifier.fillMaxWidth().height(330.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(240.dp).glow(Amber, 0.16f, 0.7f))
                SpendingOrbit(byCategory.map { colorFor(it.first) to it.second }, live.sortedByDescending(::yearly), size = 320.dp) {
                    Penny(140.dp, pose = if (visible.isEmpty()) R.drawable.penny_sleepy else R.drawable.penny_master, glow = false)
                }
            }
        }
        if (byCategory.isNotEmpty()) item {
            val sum = byCategory.sumOf { it.second }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                byCategory.take(3).forEach { (cat, v) ->
                    Row(
                        Modifier.clip(CircleShape).background(Glass).border(1.dp, Hairline, CircleShape).padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(colorFor(cat)))
                        Spacer(Modifier.width(7.dp))
                        Text("$cat ${(v / sum * 100).toInt()}%", fontSize = 13.sp, color = TextHi, maxLines = 1)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("${live.size}", fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, color = TextHi)
                    Box {
                        Row(Modifier.clip(CircleShape).clickable { listMenu = true }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (p.list == "All") "All lists" else p.list, color = TextLo, fontSize = 16.sp)
                            Icon(Icons.Default.UnfoldMore, null, tint = TextLo, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(listMenu, { listMenu = false }, containerColor = DialogBg) {
                            (listOf("All") + tags.filter { it.kind == TagKind.LIST }.map { it.name }).forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(if (name == "All") "All lists" else name, color = if (name == p.list) Gold else TextHi) },
                                    onClick = { p.list = name; listMenu = false },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        vm.money(total),
                        style = TextStyle(brush = TotalBrush, fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, letterSpacing = (-0.8).sp),
                    )
                    Text("Total yearly · ${vm.money(total / 12)}/mo", color = TextLo, fontSize = 14.sp)
                }
            }
        }
        if (!p.guideDone) item { GuideCard(onOpen = { nav.navigate("guide") }, onDismiss = { p.guideDone = true }) }

        if (visible.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nothing tracked yet", style = MaterialTheme.typography.headlineSmall, color = TextHi)
                    Text("Add what you pay for and Dues does the math.", color = TextLo, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
                    BigButton("Add a subscription", { openAdd(vm, nav) })
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(GROUPS.first { it.first == p.group }.second, { groupDialog = true }, Icons.Outlined.ViewAgenda)
                    Pill(SORTS.first { it.first == p.sort }.second, {
                        p.sort = SORTS[(SORTS.indexOfFirst { it.first == p.sort } + 1) % SORTS.size].first
                    }, Icons.Default.SwapVert)
                }
            }
            sections.filter { it.second.isNotEmpty() }.forEachIndexed { i, (title, list) ->
                item(key = "h$i$title") {
                    val right = if (title == "Cancelled") "${list.size}" else "${vm.money(list.sumOf(::yearly))}/yr"
                    SectionHeader(title, right)
                }
                items(list.sortedWith(sorter), key = { it.id }) { s ->
                    SubRow(
                        s, statusLine(s, today), vm.money(cur(s)), { nav.navigate("detail/${s.id}") },
                        faded = s.cancelledOn != null, accent = statusAccent(s, today),
                    )
                }
            }
        }
    }

    if (groupDialog) PickDialog("Group by", GROUPS, { it.second }, GROUPS.first { it.first == p.group }, { groupDialog = false }) {
        p.group = it.first
    }
}

@Composable
private fun GuideCard(onOpen: () -> Unit, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Glass).border(1.dp, Hairline, shape)
            .clickable(onClick = onOpen).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(GlassHi), contentAlignment = Alignment.Center) { Text("👋", fontSize = 22.sp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Get started guide", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextHi)
            Text("Find subscriptions you forgot", color = TextLo, fontSize = 13.sp)
        }
        Icon(Icons.Default.Close, "Dismiss", tint = TextLo, modifier = Modifier.clip(CircleShape).clickable(onClick = onDismiss).padding(6.dp))
    }
}

@Composable
private fun CalendarTab(vm: AppVM, nav: NavController) {
    val subs by vm.subs.collectAsStateWithLifecycle()
    val changes by vm.changes.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val month = YearMonth.now().plusMonths(offset.toLong())
    val first = month.atDay(1)
    val last = month.atEndOfMonth()
    var selected by remember(month) { mutableStateOf<LocalDate?>(null) }

    val pays: Map<LocalDate, List<Pair<Sub, Double>>> = remember(subs, changes, month, vm.prefs.list) {
        subs.filter { vm.prefs.list == "All" || it.listName == vm.prefs.list }
            .flatMap { s -> s.paymentsIn(first, last).map { d -> d to (s to priceOn(s, changes, d)) } }
            .groupBy({ it.first }, { it.second })
    }
    val total = pays.values.flatten().sumOf { it.second }
    val upcoming = pays.filterKeys { !it.isBefore(today) }.values.flatten().sumOf { it.second }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val name = month.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(name, style = MaterialTheme.typography.displaySmall, color = TextHi)
                    if (month.year != today.year) Text("${month.year}", color = TextLo)
                }
                CircleButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month", { offset-- })
                CircleButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month", { offset++ })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat(vm.money(total), "This month", Modifier.weight(1f))
                if (!last.isBefore(today)) Stat(vm.money(upcoming), "Still to pay", Modifier.weight(1f), gold = true)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                        Text(it, Modifier.weight(1f), color = TextLo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    }
                }
                val lead = first.dayOfWeek.value - 1
                val cells = lead + month.lengthOfMonth()
                for (r in 0 until (cells + 6) / 7) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        for (c in 0..6) {
                            val day = r * 7 + c - lead + 1
                            if (day < 1 || day > month.lengthOfMonth()) Spacer(Modifier.weight(1f))
                            else {
                                val date = month.atDay(day)
                                DayCell(day, pays[date].orEmpty().map { it.first }, date == today, date == selected, date.isBefore(today), Modifier.weight(1f)) {
                                    selected = if (selected == date) null else date
                                }
                            }
                        }
                    }
                }
            }
        }
        val sel = selected
        val entries: List<Triple<LocalDate, Sub, Double>> =
            if (sel != null) pays[sel].orEmpty().map { Triple(sel, it.first, it.second) }
            else pays.toSortedMap().flatMap { (d, l) -> l.map { Triple(d, it.first, it.second) } }
        item { SectionHeader(sel?.format(DATE) ?: "Payments this month", if (entries.isNotEmpty()) "${entries.size}" else null) }
        if (entries.isEmpty()) item { Text("No payments", color = TextLo, modifier = Modifier.padding(start = 4.dp)) }
        items(entries, key = { "${it.first}-${it.second.id}" }) { (d, s, price) ->
            SubRow(s, if (sel != null) s.cycleLabel() else d.format(DATE), vm.money(price), { nav.navigate("detail/${s.id}") })
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier, gold: Boolean = false) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.clip(shape).background(if (gold) Gold.copy(alpha = 0.10f) else Glass)
            .border(1.dp, if (gold) Gold.copy(alpha = 0.4f) else Hairline, shape).padding(14.dp),
    ) {
        Text(label, color = TextLo, fontSize = 13.sp)
        Text(value, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = if (gold) Gold else TextHi, maxLines = 1)
    }
}

@Composable
private fun DayCell(day: Int, subs: List<Sub>, isToday: Boolean, isSelected: Boolean, past: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.height(72.dp).clip(shape)
            .then(
                when {
                    isSelected -> Modifier.background(GoldBrush)
                    isToday -> Modifier.background(Gold.copy(alpha = 0.10f)).border(1.5.dp, Gold, shape)
                    else -> Modifier.background(Glass).border(1.dp, Hairline, shape)
                }
            )
            .clickable(onClick = onClick).padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "$day", fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = when {
                isSelected -> OnGold
                isToday -> Gold
                past -> TextLo.copy(alpha = 0.6f)
                else -> TextHi
            },
        )
        Spacer(Modifier.height(5.dp))
        if (subs.isNotEmpty()) {
            Logo(subs.first(), 26.dp)
            if (subs.size > 1) Text("+${subs.size - 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) OnGold else Gold)
        }
    }
}
