@file:OptIn(ExperimentalLayoutApi::class)

package com.dues.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.Brush
import com.dues.app.BuildConfig
import com.dues.app.Legal
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Description
import java.util.Locale
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.dues.app.AppVM
import com.dues.app.R
import com.dues.app.DATE
import com.dues.app.data.DEFAULT_CATEGORY
import com.dues.app.data.DEFAULT_LIST
import com.dues.app.data.EMAIL_SEARCH_TERMS
import com.dues.app.data.TagKind
import com.dues.app.data.nextPayment
import com.dues.app.data.priceOn
import com.dues.app.data.tag
import com.dues.app.sendTest
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Currency

@Composable
fun SettingsTab(vm: AppVM, nav: NavController) {
    val ctx = LocalContext.current
    val p = vm.prefs
    val country = COUNTRIES.firstOrNull { it.code == p.country }
    var editName by remember { mutableStateOf(false) }
    if (editName) TextDialog("Your name", p.name, "First name (optional)", allowEmpty = true, onDismiss = { editName = false }) { p.name = it.take(24) }

    Screen("Settings", onBack = null) {
        item { SectionHeader("Region") }
        item {
            Panel {
                PanelRow(
                    country?.name ?: "Choose your country",
                    sub = "All prices in ${currencyLabel(p.currency)}",
                    leading = {
                        Box(Modifier.size(46.dp).clip(CircleShape).background(GlassHi), contentAlignment = Alignment.Center) {
                            Text(flagOf(p.country), fontSize = 24.sp)
                        }
                    },
                    onClick = { nav.navigate("region") },
                    trailing = {
                        Text(
                            p.currency, color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.clip(CircleShape).background(Gold.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextLo)
                    },
                )
            }
        }
        item { Panel { PanelRow("Your name", p.name.ifBlank { "Not set" }, Icons.Outlined.Person, onClick = { editName = true }) } }
        item { SectionHeader("Organise") }
        item {
            Panel {
                PanelRow("Lists", icon = Icons.AutoMirrored.Outlined.List, onClick = { nav.navigate("lists") })
                PanelDivider()
                PanelRow("Categories", icon = Icons.AutoMirrored.Outlined.Label, iconTint = Coral, onClick = { nav.navigate("categories") })
                PanelDivider()
                PanelRow("Payment methods", icon = Icons.Outlined.CreditCard, iconTint = Mint, onClick = { nav.navigate("payments") })
            }
        }
        item { SectionHeader("Reminders & data") }
        item {
            Panel {
                PanelRow("Notifications", if (p.notifications) "On" else "Off", Icons.Outlined.Notifications, onClick = { nav.navigate("notifications") })
                PanelDivider()
                PanelRow("Data", icon = Icons.Outlined.Storage, iconTint = Color(0xFF7DA7FF), onClick = { nav.navigate("data") })
            }
        }
        item { SectionHeader("Spread the word") }
        item {
            Panel {
                PanelRow("Leave a review", icon = Icons.Outlined.Star, onClick = {
                    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${ctx.packageName}"))
                    runCatching { ctx.startActivity(market) }.onFailure {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${ctx.packageName}"))) }
                    }
                })
                PanelDivider()
                PanelRow("Share with a friend", icon = Icons.Outlined.Share, iconTint = Coral, onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(
                        Intent.EXTRA_TEXT,
                        "I keep track of my subscriptions with Dues: https://play.google.com/store/apps/details?id=${ctx.packageName}",
                    )
                    ctx.startActivity(Intent.createChooser(send, "Share Dues"))
                })
            }
        }
        item { SectionHeader("About") }
        item {
            Panel {
                PanelRow("Privacy policy", icon = Icons.Outlined.Shield, iconTint = Mint, onClick = { nav.navigate("legal/privacy") })
                PanelDivider()
                PanelRow("Terms of use", icon = Icons.Outlined.Description, iconTint = TextLo, onClick = { nav.navigate("legal/terms") })
                PanelDivider()
                PanelRow("Contact support", Legal.SUPPORT_EMAIL, Icons.Outlined.Mail, iconTint = Coral, onClick = {
                    runCatching {
                        ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Legal.SUPPORT_EMAIL}")).putExtra(Intent.EXTRA_SUBJECT, "Dues support"))
                    }
                }, trailing = {})
            }
        }
        item {
            Text(
                "Dues 1.0 by Pennylume  ·  Logos by Logo.dev", color = TextLo, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://logo.dev"))) }
                },
            )
        }
    }
}

@Composable
fun RegionScreen(vm: AppVM, nav: NavController) {
    var sel by remember { mutableStateOf(vm.prefs.country) }
    var currencyDialog by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    LaunchedEffect(vm.conversionNote) {
        vm.conversionNote?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_LONG).show()
            vm.conversionNote = null
        }
    }
    Backdrop(dim = 0.55f) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", { nav.popBackStack() })
                Spacer(Modifier.weight(1f))
                Pill("Currency only", { currencyDialog = true })
            }
            Text("Country & currency", style = MaterialTheme.typography.displaySmall, color = TextHi, modifier = Modifier.padding(top = 4.dp))
            Text(
                "Your country sets the currency for every price. Now showing ${currencyLabel(vm.prefs.currency)}.",
                color = TextLo, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
            CountryPicker(sel, { sel = it; vm.setCountry(it) }, suggested = Locale.getDefault().country, modifier = Modifier.weight(1f))
        }
    }
    if (currencyDialog) {
        val all = remember { Currency.getAvailableCurrencies().sortedBy { it.currencyCode } }
        PickDialog("Currency", all, { "${it.currencyCode}  ${it.symbol} · ${it.displayName}" }, all.firstOrNull { it.currencyCode == vm.prefs.currency }, { currencyDialog = false }) {
            vm.setCurrency(it.currencyCode)
        }
    }
}
@Composable
fun TagsScreen(vm: AppVM, nav: NavController, kind: String) {
    val subs by vm.subs.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val changes by vm.changes.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val names = tags.filter { it.kind == kind }.map { it.name }
    val (title, noun, desc) = when (kind) {
        TagKind.LIST -> Triple("Lists", "list", "Separate subscriptions into lists like Personal, Family or Business. Tap Select to move several at once.")
        TagKind.CATEGORY -> Triple("Categories", "category", "Organise subscriptions by type, like Streaming, Music or Gaming. Tap Select to move several at once.")
        else -> Triple("Payment methods", "payment method", "Track which card or account pays for what.")
    }
    val protected = setOf(DEFAULT_LIST, DEFAULT_CATEGORY)

    var selecting by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Long>() }
    var menu by remember { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf<Pair<String, String>?>(null) } // action to tag name

    // Unassigned payment methods get a pseudo-group so every sub is reachable here.
    val groups = names.map { it to subs.filter { s -> s.tag(kind) == it } } +
        if (kind == TagKind.PAYMENT) listOf("" to subs.filter { it.paymentMethod.isBlank() }).filter { it.second.isNotEmpty() } else emptyList()

    Box(Modifier.fillMaxSize()) {
        Screen(title, onBack = { nav.popBackStack() }, subtitle = desc, actions = {
            if (selecting) {
                Text("${selected.size} selected", color = TextLo)
                Pill("Cancel", { selecting = false; selected.clear() })
            } else {
                CircleButton(Icons.Default.Add, "Add $noun", { dialog = "add" to "" })
                Pill("Select", { selecting = true })
            }
        }) {
            if (kind == TagKind.LIST) {
                val suggestions = listOf("Family", "Business", "Work", "Shared").filter { it !in names }.take(3)
                if (suggestions.isNotEmpty()) item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { Pill(it, { vm.addTag(kind, it) }, Icons.Default.Add) }
                    }
                }
            }
            groups.forEach { (name, list) ->
                item(key = "g-$name") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name.ifBlank { "No payment method" }, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (name.isNotBlank()) Box {
                                IconButton({ menu = name }) { Icon(Icons.Default.MoreHoriz, "More") }
                                DropdownMenu(menu == name, { menu = null }) {
                                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = null; dialog = "rename" to name })
                                    if (name !in protected) DropdownMenuItem(
                                        text = { Text("Delete", color = Danger) }, onClick = { menu = null; dialog = "delete" to name },
                                    )
                                }
                            }
                        }
                        if (list.isEmpty()) Panel { Text("Empty", color = TextLo, modifier = Modifier.padding(16.dp)) }
                        list.forEach { s ->
                            val sel = s.id in selected
                            SubRow(
                                s, s.nextPayment(today)?.format(DATE) ?: "Cancelled", vm.money(priceOn(s, changes, today)),
                                onClick = {
                                    if (!selecting) nav.navigate("detail/${s.id}")
                                    else if (sel) selected.remove(s.id) else selected.add(s.id)
                                },
                                leading = if (selecting) { { CheckDot(sel) } } else null,
                            )
                        }
                    }
                }
            }
        }
        if (selecting && selected.isNotEmpty()) {
            BigButton("Move to…", { dialog = "move" to "" }, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
        }
    }

    val close = { dialog = null }
    when (dialog?.first) {
        "add" -> TextDialog("New $noun", confirm = "Add", onDismiss = close) { vm.addTag(kind, it) }
        "rename" -> dialog?.second?.let { old -> TextDialog("Rename $noun", initial = old, onDismiss = close) { vm.renameTag(kind, old, it) } }
        "delete" -> dialog?.second?.let { name ->
            val fallback = when (kind) {
                TagKind.LIST -> "moves to $DEFAULT_LIST"
                TagKind.CATEGORY -> "moves to $DEFAULT_CATEGORY"
                else -> "will have no payment method"
            }
            ConfirmDialog("Delete \"$name\"?", "Anything in it $fallback.", "Delete", close) { vm.deleteTag(kind, name) }
        }
        "move" -> PickDialog("Move to", names, { it }, null, close) {
            vm.moveSubs(selected.toSet(), kind, it)
            selected.clear()
            selecting = false
        }
    }
}

@Composable
fun NotificationsScreen(vm: AppVM, nav: NavController) {
    val ctx = LocalContext.current
    val p = vm.prefs
    var countdown by remember { mutableIntStateOf(0) }
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }
    fun hasPermission() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var pendingTest by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            Toast.makeText(ctx, "Allow notifications for Dues in system settings", Toast.LENGTH_LONG).show()
        } else if (pendingTest) {
            sendTest(ctx)
            countdown = 10
        } else {
            p.notifications = true
        }
        pendingTest = false
    }

    Screen("Notifications", onBack = { nav.popBackStack() }) {
        item {
            Panel {
                PanelRow("Renewal reminders", trailing = {
                    Switch(
                        p.notifications,
                        { on -> if (on && !hasPermission()) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else p.notifications = on },
                        colors = SwitchDefaults.colors(checkedTrackColor = Gold),
                    )
                })
            }
        }
        item { Text("Get a heads-up before each charge. Choose how early on each subscription.", color = TextLo, modifier = Modifier.padding(horizontal = 4.dp)) }
        item {
            Panel {
                PanelRow("Send test notification", if (countdown > 0) "Sending in ${countdown}s…" else null, valueColor = Coral, onClick = {
                    if (countdown == 0) {
                        if (!hasPermission()) {
                            pendingTest = true
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            sendTest(ctx)
                            countdown = 10
                        }
                    }
                }, trailing = {})
            }
        }
        item { Text("Arrives in about 10 seconds, even if you leave the app.", color = TextLo, modifier = Modifier.padding(horizontal = 4.dp)) }
    }
}

@Composable
fun DataScreen(vm: AppVM, nav: NavController) {
    val ctx = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.csv().toByteArray()) } ?: error("no stream")
            }.isSuccess
            Toast.makeText(ctx, if (ok) "Exported" else "Export failed", Toast.LENGTH_SHORT).show()
        }
    }

    Screen("Data", onBack = { nav.popBackStack() }) {
        item { Panel { PanelRow("Export as CSV", icon = Icons.Outlined.FileDownload, onClick = { exporter.launch("dues-subscriptions.csv") }) } }
        item { Text("Save all your subscriptions as a spreadsheet file.", color = TextLo, modifier = Modifier.padding(horizontal = 4.dp)) }
        item { SectionHeader("Danger zone") }
        item { Panel { PanelRow("Delete all subscriptions", icon = Icons.Outlined.Delete, labelColor = Danger, onClick = { confirm = true }, trailing = {}) } }
    }
    if (confirm) ConfirmDialog(
        "Delete all subscriptions?", "This permanently deletes every subscription and its price history. This can't be undone.",
        "Delete all", { confirm = false },
    ) {
        vm.deleteAll()
        Toast.makeText(ctx, "All subscriptions deleted", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun GuideScreen(vm: AppVM, nav: NavController) {
    val ctx = LocalContext.current
    var bankTips by remember { mutableStateOf(false) }
    Screen("Get started", onBack = { nav.popBackStack() }, subtitle = "Find every subscription in four quick steps.") {
        item {
            Panel {
                PanelRow("Check Google Play subscriptions", icon = Icons.Outlined.Shop, onClick = {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))) }
                })
                PanelDivider()
                PanelRow("Search your email", icon = Icons.Outlined.Mail, onClick = { nav.navigate("email") })
                PanelDivider()
                PanelRow("Scan your bank statement", icon = Icons.Outlined.AccountBalance, onClick = { bankTips = true })
                PanelDivider()
                PanelRow("Add what you found", icon = Icons.Default.Add, onClick = { openAdd(vm, nav) })
            }
        }
        item {
            TextButton({ vm.prefs.guideDone = true; nav.popBackStack() }, Modifier.fillMaxWidth()) { Text("I'm done, hide this guide", color = TextLo) }
        }
    }
    if (bankTips) AlertDialog(
        onDismissRequest = { bankTips = false },
        title = { Text("Bank statement") },
        text = {
            Text(
                "Open your banking app and look through the last two or three months. " +
                    "Charges that repeat on the same day each month are almost always subscriptions. " +
                    "Look for small amounts too: they're the easiest to forget."
            )
        },
        confirmButton = { TextButton({ bankTips = false }) { Text("Got it") } },
    )
}

@Composable
fun EmailGuideScreen(nav: NavController) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Screen("Email", onBack = { nav.popBackStack() }, subtitle = "Receipts and renewal notices reveal hidden subscriptions. Search your inbox with these terms.") {
        item {
            Panel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Step(1, "Copy a search term")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EMAIL_SEARCH_TERMS.forEach { t ->
                            Pill(t, {
                                clipboard.setText(AnnotatedString(t))
                                Toast.makeText(ctx, "Copied \"$t\"", Toast.LENGTH_SHORT).show()
                            }, Icons.Default.ContentCopy)
                        }
                    }
                    Step(2, "Open your email app")
                    Pill("Open email", {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL)) }
                            .onFailure { Toast.makeText(ctx, "No email app found", Toast.LENGTH_SHORT).show() }
                    }, Icons.Outlined.Mail)
                    Step(3, "Paste it into the search bar")
                    Step(4, "Add each subscription you find")
                    Pill("Add subscription", { nav.navigate("add") }, Icons.Default.Add, primary = true)
                }
            }
        }
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(GoldBrush), contentAlignment = Alignment.Center) {
            Text("$n", color = OnGold, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 16.sp)
    }
}


fun openUrl(ctx: android.content.Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

/** Bundled copy of the hosted legal page (works offline), styled like the website. */
@Composable
fun LegalScreen(nav: NavController, doc: String) {
    Backdrop(dim = 0.55f) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", { nav.popBackStack() })
            }
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { c ->
                    android.webkit.WebView(c).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        settings.javaScriptEnabled = false
                        // Keep in-app for bundled pages; open anything else in the browser.
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun shouldOverrideUrlLoading(v: android.webkit.WebView, r: android.webkit.WebResourceRequest): Boolean {
                                val url = r.url.toString()
                                if (url.startsWith("file:///android_asset/")) return false
                                openUrl(c, url)
                                return true
                            }
                        }
                        loadUrl("file:///android_asset/legal/${if (doc == "terms") "terms" else "privacy"}.html")
                    }
                },
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            )
        }
    }
}
