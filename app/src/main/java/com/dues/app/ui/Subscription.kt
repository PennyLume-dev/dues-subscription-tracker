package com.dues.app.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.dues.app.AppVM
import com.dues.app.DATE
import com.dues.app.data.Service
import com.dues.app.data.Plan
import com.dues.app.data.PRICED_COUNTRIES
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import com.dues.app.data.Sub
import androidx.compose.ui.text.style.TextAlign
import com.dues.app.data.CycleUnit
import com.dues.app.data.TagKind
import com.dues.app.data.cycleLabel
import com.dues.app.data.daysPhrase
import com.dues.app.data.isTrial
import com.dues.app.data.nextPayment
import com.dues.app.data.paymentsPerYear
import com.dues.app.data.priceOn
import java.time.LocalDate
import java.util.Locale

private val NOTIFY = listOf(-1 to "None", 0 to "On the day", 1 to "1 day before", 2 to "2 days before", 3 to "3 days before", 7 to "1 week before")
fun notifyLabel(n: Int) = NOTIFY.firstOrNull { it.first == n }?.second ?: "$n days before"

private const val NEW = "＋ New…"

@Composable
fun CatalogScreen(vm: AppVM, nav: NavController) {
    var q by rememberSaveable { mutableStateOf("") }
    var planFor by remember { mutableStateOf<Service?>(null) }
    val matches = vm.catalog.filter { vm.sold(it) && (q.isBlank() || it.name.contains(q.trim(), ignoreCase = true)) }.groupBy { it.category }
    fun pick(s: Service, p: Plan?) {
        vm.draftFrom(s, p)
        nav.navigate("edit/0")
    }
    Screen("Add subscription", onBack = { nav.popBackStack() }) {
        item {
            OutlinedTextField(
                q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextLo) }, placeholder = { Text("Search ${vm.catalog.count(vm::sold)} services", color = TextLo) },
            )
        }
        item {
            Panel {
                PanelRow(
                    if (q.isBlank()) "Custom subscription" else "Add \"${q.trim()}\" yourself", icon = Icons.Default.Add,
                    sub = "Not listed? Add your own price",
                    onClick = { vm.draftFrom(null, name = q.trim()); nav.navigate("edit/0") },
                )
            }
        }
        if (vm.prefs.country !in PRICED_COUNTRIES) item {
            Text(
                "Verified prices cover 12 countries so far. For yours, pick your plan and enter the price from your receipt.",
                color = TextLo, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        matches.forEach { (category, list) ->
            item(key = category) {
                SectionHeader(category)
                Spacer(Modifier.height(8.dp))
                Panel {
                    list.forEachIndexed { i, s ->
                        if (i > 0) PanelDivider()
                        Row(
                            Modifier.fillMaxWidth().clickable { planFor = s }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Logo(s.name, s.domain, "", 40.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextHi)
                                if (vm.plansFor(s).size > 1) Text("${vm.plansFor(s).size} plans", fontSize = 12.sp, color = TextLo)
                            }
                            Text(vm.planLabel(vm.defaultPlan(s)), color = TextLo, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
    planFor?.let { s ->
        PlanSheet(vm, s, { planFor = null }, onCustom = {
            planFor = null
            vm.draftFrom(s, null)
            vm.draft = vm.draft?.copy(price = 0.0, notes = "")
            vm.priceHint = "Enter what you pay and set the billing cycle, e.g. every year."
            nav.navigate("edit/0")
        }) { p -> planFor = null; pick(s, p) }
    }
}

@Composable
fun PlanSheet(vm: AppVM, s: Service, onDismiss: () -> Unit, onCustom: (() -> Unit)?, onPick: (Plan) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        icon = { Logo(s.name, s.domain, "", 52.dp) },
        title = { Text("Choose your ${s.name} plan", fontFamily = Display, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.plansFor(s).size) { i ->
                    val p = vm.plansFor(s)[i]
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier.fillMaxWidth().clip(shape).background(Glass).border(1.dp, Hairline, shape)
                            .clickable { onPick(p) }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, color = TextHi, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            val tags = listOfNotNull(
                                Sub(name = "", price = 0.0, startDate = LocalDate.now(), cycleUnit = p.unit, cycleCount = p.count).cycleLabel(),
                                "Family".takeIf { p.family }, "Student".takeIf { p.student },
                                "${p.trialDays}-day trial".takeIf { p.trialDays > 0 },
                            )
                            Text(tags.joinToString(" · "), color = TextLo, fontSize = 12.sp)
                        }
                        Text(vm.planLabel(p), color = if (vm.exactPrice(p) != null && !p.estimated) Gold else TextLo, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                if (onCustom != null) item {
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier.fillMaxWidth().clip(shape).border(1.dp, Gold.copy(alpha = 0.5f), shape)
                            .clickable(onClick = onCustom).padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Yearly or a different plan?", color = Gold, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Add it with your own price and billing cycle", color = TextLo, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.Add, null, tint = Gold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Cancel", color = TextLo) } },
    )
}

@Composable
fun EditScreen(vm: AppVM, nav: NavController, id: Long) {
    val subs by vm.subs.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val changes by vm.changes.collectAsStateWithLifecycle()
    val original = (if (id == 0L) vm.draft else subs.find { it.id == id }) ?: return
    val ctx = LocalContext.current

    var name by remember(original.id) { mutableStateOf(original.name) }
    var price by remember(original.id) { mutableStateOf(if (original.price == 0.0) "" else "%.2f".format(Locale.US, original.price)) }
    var domain by remember(original.id) { mutableStateOf(original.domain) }
    var emoji by remember(original.id) { mutableStateOf(original.emoji) }
    var start by remember(original.id) { mutableStateOf(original.startDate) }
    var unit by remember(original.id) { mutableStateOf(original.cycleUnit) }
    var count by remember(original.id) { mutableIntStateOf(original.cycleCount) }
    var trial by remember(original.id) { mutableStateOf(original.freeTrial) }
    var list by remember(original.id) { mutableStateOf(original.listName) }
    var category by remember(original.id) { mutableStateOf(original.category) }
    var payment by remember(original.id) { mutableStateOf(original.paymentMethod) }
    var notify by remember(original.id) { mutableIntStateOf(original.notifyDaysBefore) }
    var notes by remember(original.id) { mutableStateOf(original.notes) }

    var dialog by remember { mutableStateOf("") } // date | cycle | list | category | payment | notify | logo | new:<kind>

    fun save() {
        val p = price.replace(',', '.').toDoubleOrNull()
        if (name.isBlank() || p == null || p < 0) {
            Toast.makeText(ctx, "Enter a name and a valid price", Toast.LENGTH_SHORT).show()
            return
        }
        vm.save(
            original.copy(
                name = name.trim(), price = p, domain = cleanDomain(domain), emoji = emoji, startDate = start,
                cycleUnit = unit, cycleCount = count, freeTrial = trial, listName = list, category = category,
                paymentMethod = payment, notifyDaysBefore = notify, notes = notes.trim(),
            )
        )
        vm.draft = null
        if (id == 0L) nav.popBackStack("main", false) else nav.popBackStack()
    }

    Screen(
        if (id == 0L) "New subscription" else "Edit subscription",
        onBack = { nav.popBackStack() },
        actions = { Pill("Save", ::save, primary = true) },
    ) {
        item {
            Panel {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable { dialog = "logo" }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Logo(name.ifBlank { "?" }, domain, emoji, 64.dp)
                        Text("Change", color = Gold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Name") })
                        OutlinedTextField(
                            price, { price = it }, singleLine = true, label = { Text("Price (${vm.prefs.currency})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                }
            }
        }
        if (id == 0L) vm.priceHint?.let { hint -> item { Text(hint, color = Gold, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 4.dp)) } }
        val current = priceOn(original, changes, LocalDate.now())
        if (id != 0L && current != original.price) item {
            Text("Current price is ${vm.money(current)} from price history.", color = TextLo, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
        }
        item {
            Panel {
                PanelRow(if (trial) "Trial ends" else "Payment date", start.format(DATE), onClick = { dialog = "date" })
                PanelDivider()
                PanelRow("Billing cycle", original.copy(cycleUnit = unit, cycleCount = count).cycleLabel(), onClick = { dialog = "cycle" })
                if (id == 0L && unit == CycleUnit.YEAR && original.cycleUnit != CycleUnit.YEAR) Text(
                    "Tip: yearly plans often come with a discount. Enter the amount on your receipt for exact tracking.",
                    color = TextLo, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                PanelDivider()
                PanelRow("Free trial", trailing = {
                    Switch(trial, { trial = it }, colors = SwitchDefaults.colors(checkedTrackColor = Gold))
                })
            }
        }
        if (trial) item {
            val note = if (notify < 0) "Turn on a notification below so you can cancel before the trial ends on ${start.format(DATE)}."
            else "You'll be reminded on ${start.minusDays(notify.toLong()).format(DATE)}, before the trial ends on ${start.format(DATE)}."
            Panel { Text("🔔  $note", Modifier.padding(16.dp), fontSize = 14.sp) }
        }
        item {
            Panel {
                PanelRow("List", list, onClick = { dialog = "list" })
                PanelDivider()
                PanelRow("Category", category, onClick = { dialog = "category" })
                PanelDivider()
                PanelRow("Payment method", payment.ifBlank { "None" }, onClick = { dialog = "payment" })
            }
        }
        item { Panel { PanelRow("Notification", notifyLabel(notify), onClick = { dialog = "notify" }) } }
        if (id != 0L) item { Panel { PanelRow("Price history", onClick = { nav.navigate("prices/$id") }) } }
        item {
            SectionHeader("Website")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(domain, { domain = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("e.g. netflix.com") }, shape = RoundedCornerShape(16.dp))
        }
        item {
            SectionHeader("Notes")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(16.dp))
        }
    }

    fun tagNames(kind: String) = tags.filter { it.kind == kind }.map { it.name }
    val close = { dialog = "" }
    // PickDialog dismisses before calling onPick, so picking NEW can open the next dialog.
    when (dialog) {
        "date" -> DateDialog(start, close) { start = it }
        "cycle" -> CycleDialog(unit, count, close) { u, c -> unit = u; count = c }
        "list" -> PickDialog("List", tagNames(TagKind.LIST) + NEW, { it }, list, close) {
            if (it == NEW) dialog = "new:${TagKind.LIST}" else list = it
        }
        "category" -> PickDialog("Category", tagNames(TagKind.CATEGORY) + NEW, { it }, category, close) {
            if (it == NEW) dialog = "new:${TagKind.CATEGORY}" else category = it
        }
        "payment" -> PickDialog("Payment method", listOf("None") + tagNames(TagKind.PAYMENT) + NEW, { it }, payment.ifBlank { "None" }, close) {
            when (it) {
                NEW -> dialog = "new:${TagKind.PAYMENT}"
                "None" -> payment = ""
                else -> payment = it
            }
        }
        "notify" -> PickDialog("Notification", NOTIFY, { it.second }, NOTIFY.firstOrNull { it.first == notify }, close) { notify = it.first }
        "logo" -> LogoDialog(name, domain, emoji, close) { d, e -> domain = d; emoji = e }
        "" -> Unit
        else -> {
            val kind = dialog.removePrefix("new:")
            TextDialog("New ${if (kind == TagKind.PAYMENT) "payment method" else kind}", confirm = "Add", onDismiss = close) { n ->
                vm.addTag(kind, n)
                when (kind) {
                    TagKind.LIST -> list = n
                    TagKind.CATEGORY -> category = n
                    else -> payment = n
                }
            }
        }
    }
}

@Composable
private fun CycleDialog(unit: CycleUnit, count: Int, onDismiss: () -> Unit, onPick: (CycleUnit, Int) -> Unit) {
    var u by remember { mutableStateOf(unit) }
    var c by remember { mutableIntStateOf(count) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text("Billing cycle", fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Every", Modifier.weight(1f), fontSize = 17.sp, color = TextLo)
                    CircleButton(Icons.Default.Remove, "Less", { if (c > 1) c-- })
                    Text("$c", fontFamily = Display, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TextHi, textAlign = TextAlign.Center, modifier = Modifier.width(56.dp))
                    CircleButton(Icons.Default.Add, "More", { if (c < 99) c++ })
                }
                Segmented(CycleUnit.entries, u, { it.name.lowercase().replaceFirstChar(Char::uppercase) + if (c > 1) "s" else "" }, { u = it })
                Text(Sub(name = "", price = 0.0, startDate = LocalDate.now(), cycleUnit = u, cycleCount = c).cycleLabel(), color = Gold, fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = { TextButton({ onPick(u, c); onDismiss() }) { Text("Done", color = Gold, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = TextLo) } },
    )
}

private val EMOJIS = listOf(
    "🎬", "🎵", "🎧", "📺", "🎮", "📚", "📰", "🤖", "☁️", "💾", "🏋️", "🧘",
    "🍿", "🛒", "📦", "🚗", "🏠", "💡", "📱", "💻", "✏️", "🎨", "📷", "✈️",
    "🍔", "☕", "🐶", "💊", "🎓", "💼", "🔒", "⭐", "❤️", "🌱", "🎁", "💳",
)

@Composable
private fun LogoDialog(name: String, domain: String, emoji: String, onDismiss: () -> Unit, onPick: (String, String) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf(domain.ifBlank { name }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text("Logo", fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Segmented(listOf(0, 1), tab, { if (it == 0) "Website" else "Emoji" }, { tab = it })
                if (tab == 0) {
                    OutlinedTextField(q, { q = it }, singleLine = true, placeholder = { Text("Brand or website") })
                    val slug = q.lowercase().filter { it.isLetterOrDigit() }
                    val candidates = (listOf(cleanDomain(q)).filter { '.' in it } + listOf("$slug.com", "$slug.app", "$slug.io"))
                        .filter { it.length > 4 }.distinct()
                    candidates.forEach { d ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onPick(d, ""); onDismiss() }.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Logo(name.ifBlank { d }, d, "", 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(d)
                        }
                    }
                } else {
                    LazyVerticalGrid(GridCells.Fixed(6), Modifier.height(260.dp)) {
                        items(EMOJIS) { e ->
                            Box(
                                Modifier.clip(RoundedCornerShape(10.dp)).clickable { onPick(domain, e); onDismiss() }.padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text(e, fontSize = 24.sp) }
                        }
                    }
                    if (emoji.isNotEmpty()) TextButton({ onPick(domain, ""); onDismiss() }) { Text("Remove emoji") }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Close") } },
    )
}

@Composable
fun DetailScreen(vm: AppVM, nav: NavController, id: Long) {
    val subs by vm.subs.collectAsStateWithLifecycle()
    val changes by vm.changes.collectAsStateWithLifecycle()
    val s = subs.find { it.id == id } ?: return
    val today = LocalDate.now()
    val current = priceOn(s, changes, today)
    val next = s.nextPayment(today)
    val uri = LocalUriHandler.current
    var confirmDelete by remember { mutableStateOf(false) }

    Screen(null, onBack = { nav.popBackStack() }, actions = { Pill("Edit", { nav.navigate("edit/$id") }) }) {
        item {
            Column {
                Logo(s, 72.dp)
                Spacer(Modifier.height(12.dp))
                Text(s.name, style = MaterialTheme.typography.displaySmall)
                Text(
                    vm.money(current),
                    style = androidx.compose.ui.text.TextStyle(brush = TotalBrush, fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp),
                )
            }
        }
        item {
            Panel {
                PanelRow("Billing", s.cycleLabel(), valueColor = TextHi)
                if (s.isTrial(today)) {
                    PanelDivider()
                    PanelRow("Free trial", "Ends ${daysPhrase(today, s.startDate)}", valueColor = Coral)
                }
                PanelDivider()
                PanelRow(
                    if (s.cancelledOn != null) "Cancelled on" else "Next payment",
                    (s.cancelledOn ?: next)?.format(DATE) ?: "—", valueColor = TextHi,
                )
                PanelDivider()
                PanelRow("Yearly cost", vm.money(current * s.paymentsPerYear()), valueColor = TextHi)
                PanelDivider()
                PanelRow("Payment method", s.paymentMethod.ifBlank { "None" }, valueColor = TextHi)
                PanelDivider()
                PanelRow("Category", s.category, valueColor = TextHi)
                PanelDivider()
                PanelRow("List", s.listName, valueColor = TextHi)
                PanelDivider()
                PanelRow("Reminder", notifyLabel(s.notifyDaysBefore), valueColor = TextHi)
                if (s.domain.isNotBlank()) {
                    PanelDivider()
                    PanelRow("Website", "${s.domain} ↗", valueColor = Gold, onClick = { runCatching { uri.openUri("https://${s.domain}") } }, trailing = {})
                }
            }
        }
        if (s.notes.isNotBlank()) item {
            Panel {
                Column(Modifier.padding(16.dp)) {
                    Text("Notes", fontWeight = FontWeight.SemiBold)
                    Text(s.notes, color = TextLo, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        item {
            val cancelled = s.cancelledOn != null
            BigButton(if (cancelled) "Resume subscription" else "Mark as cancelled", { vm.setCancelled(s, !cancelled) }, secondary = !cancelled)
        }
        item {
            TextButton({ confirmDelete = true }, Modifier.fillMaxWidth()) { Text("Delete subscription", color = Danger) }
        }
    }
    if (confirmDelete) ConfirmDialog(
        "Delete ${s.name}?", "This removes it and its price history. This can't be undone.", "Delete", { confirmDelete = false },
    ) {
        vm.delete(s)
        nav.popBackStack()
    }
}

@Composable
fun PriceHistoryScreen(vm: AppVM, nav: NavController, id: Long) {
    val subs by vm.subs.collectAsStateWithLifecycle()
    val changes by vm.changes.collectAsStateWithLifecycle()
    val s = subs.find { it.id == id } ?: return
    var adding by remember { mutableStateOf(false) }

    Screen(
        "Price history", onBack = { nav.popBackStack() },
        subtitle = "Record price changes so totals and the calendar stay accurate.",
        actions = { Pill("Add", { adding = true }, Icons.Default.Add) },
    ) {
        item {
            Panel {
                changes.filter { it.subId == id }.sortedByDescending { it.date }.forEach { c ->
                    PanelRow(c.date.format(DATE), vm.money(c.price), valueColor = TextHi, trailing = {
                        IconButton({ vm.deleteChange(c) }) { Icon(Icons.Outlined.Delete, "Delete", tint = TextLo) }
                    })
                    PanelDivider()
                }
                PanelRow("Starting price · ${s.startDate.format(DATE)}", vm.money(s.price), valueColor = TextHi)
            }
        }
    }
    if (adding) AddPriceDialog(vm, { adding = false }) { date, price -> vm.addChange(id, date, price) }
}

@Composable
private fun AddPriceDialog(vm: AppVM, onDismiss: () -> Unit, onAdd: (LocalDate, Double) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var price by remember { mutableStateOf("") }
    var pickDate by remember { mutableStateOf(false) }
    val value = price.replace(',', '.').toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add price change") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Panel { PanelRow("From", date.format(DATE), onClick = { pickDate = true }) }
                OutlinedTextField(
                    price, { price = it }, singleLine = true, label = { Text("New price (${vm.prefs.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton({ if (value != null) onAdd(date, value); onDismiss() }, enabled = value != null && value >= 0) { Text("Add") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
    if (pickDate) DateDialog(date, { pickDate = false }) { date = it }
}
