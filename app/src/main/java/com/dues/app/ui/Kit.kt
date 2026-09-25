@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)

package com.dues.app.ui

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import androidx.compose.ui.platform.LocalContext
import com.dues.app.R
import com.dues.app.data.Sub
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random

// ---- Tokens: warm night, gold light ----
val Bg = Color(0xFF0E0B08)
val Glass = Color(0xC71A140E)
val GlassHi = Color(0x26FFFFFF)
val Hairline = Color(0x1AFFFFFF)
val TextHi = Color(0xFFF8F1E4)
val TextLo = Color(0xFFC2B5A2)
val Gold = Color(0xFFF6BD4A)
val Amber = Color(0xFFF08A3C)
val Coral = Color(0xFFFF7A59)
val Mint = Color(0xFF6FD3A4)
val Danger = Color(0xFFFF6161)
val OnGold = Color(0xFF1C1206)
val DialogBg = Color(0xFF1D1813)
val GoldBrush = Brush.horizontalGradient(listOf(Gold, Amber))
val TotalBrush = Brush.verticalGradient(listOf(TextHi, Gold))

private val PALETTE = listOf(
    Gold, Coral, Mint, Color(0xFF7DA7FF), Color(0xFFC69CFF),
    Color(0xFFFF9EC7), Amber, Color(0xFF5ED0E0),
)

fun colorFor(key: String) = PALETTE[Math.floorMod(key.hashCode(), PALETTE.size)]

private fun vf(res: Int, w: Int) = Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
val Display = FontFamily(vf(R.font.bricolage, 500), vf(R.font.bricolage, 600), vf(R.font.bricolage, 700), vf(R.font.bricolage, 800))
val Body = FontFamily(vf(R.font.jakarta, 400), vf(R.font.jakarta, 500), vf(R.font.jakarta, 600), vf(R.font.jakarta, 700))

@Composable
fun DuesTheme(content: @Composable () -> Unit) {
    val b = Typography()
    fun TextStyle.body() = copy(fontFamily = Body)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold, onPrimary = OnGold, primaryContainer = Gold.copy(alpha = 0.18f), onPrimaryContainer = Gold,
            secondary = Coral, onSecondary = OnGold, tertiary = Mint,
            background = Bg, onBackground = TextHi, surface = DialogBg, onSurface = TextHi,
            surfaceVariant = GlassHi, onSurfaceVariant = TextLo, outline = Hairline, outlineVariant = Hairline,
            surfaceContainerLowest = DialogBg, surfaceContainerLow = DialogBg, surfaceContainer = DialogBg,
            surfaceContainerHigh = DialogBg, surfaceContainerHighest = GlassHi, error = Danger,
        ),
        typography = Typography(
            displayLarge = b.displayLarge.copy(fontFamily = Display, fontWeight = FontWeight.ExtraBold),
            displayMedium = b.displayMedium.copy(fontFamily = Display, fontWeight = FontWeight.ExtraBold),
            displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.2).sp),
            headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, lineHeight = 44.sp, letterSpacing = (-0.2).sp),
            headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.1).sp),
            headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
            titleLarge = b.titleLarge.copy(fontFamily = Display, fontWeight = FontWeight.Bold),
            titleMedium = b.titleMedium.body(), titleSmall = b.titleSmall.body(),
            bodyLarge = b.bodyLarge.body(), bodyMedium = b.bodyMedium.body(), bodySmall = b.bodySmall.body(),
            labelLarge = b.labelLarge.copy(fontFamily = Body, fontWeight = FontWeight.SemiBold),
            labelMedium = b.labelMedium.body(), labelSmall = b.labelSmall.body(),
        ),
        // No Surface wraps our screens, so set the default text/icon colour here (otherwise black).
        content = { androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides TextHi, content = content) },
    )
}

// ---- Atmosphere ----

/** Painted night sky (generated art) + drifting gold dust. */
@Composable
fun Backdrop(modifier: Modifier = Modifier, dim: Float = 0f, content: @Composable BoxScope.() -> Unit) {
    val dust = remember { Random(7).let { r -> List(38) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 1.6f, 0.15f + r.nextFloat() * 0.5f) } } }
    val drift by rememberInfiniteTransition(label = "dust").animateFloat(
        0f, 1f, infiniteRepeatable(tween(26000), RepeatMode.Restart), label = "drift",
    )
    Box(modifier.fillMaxSize().background(Bg)) {
        Image(
            painterResource(R.drawable.backdrop), null, Modifier.fillMaxSize().alpha(1f - dim), contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
        Canvas(Modifier.fillMaxSize()) {
            dust.forEach { (x, y, s, a) ->
                val yy = ((y - drift * 0.15f) % 1f + 1f) % 1f
                drawCircle(Gold.copy(alpha = a * (1f - yy * 0.7f)), s.dp.toPx() / 2, Offset(x * size.width, yy * size.height))
            }
        }
        content()
    }
}

/** Soft radial light, for placing behind hero objects. */
fun Modifier.glow(color: Color = Gold, alpha: Float = 0.32f, scale: Float = 0.9f) = drawBehind {
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center = center, radius = size.maxDimension * scale),
        radius = size.maxDimension * scale,
    )
}

// ---- Layout ----

/** Back button + actions, then a scrolling column with a big display title. */
@Composable
fun Screen(
    title: String?,
    onBack: (() -> Unit)?,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    Backdrop(dim = 0.55f) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onBack != null) CircleButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                Spacer(Modifier.weight(1f))
                actions()
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (title != null) item {
                    Column(Modifier.padding(bottom = 8.dp)) {
                        Text(title, style = MaterialTheme.typography.displaySmall, color = TextHi)
                        if (subtitle != null) Text(subtitle, color = TextLo, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                content()
            }
        }
    }
}

@Composable
fun CircleButton(icon: ImageVector, desc: String, onClick: () -> Unit, tint: Color = TextHi, gold: Boolean = false) {
    Box(
        Modifier.size(46.dp).clip(CircleShape)
            .then(if (gold) Modifier.background(GoldBrush) else Modifier.background(Glass).border(1.dp, Hairline, CircleShape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = if (gold) OnGold else tint) }
}

@Composable
fun Pill(text: String, onClick: () -> Unit, icon: ImageVector? = null, primary: Boolean = false, selected: Boolean = false) {
    val fg = if (primary) OnGold else if (selected) Gold else TextHi
    Row(
        Modifier.height(42.dp).clip(CircleShape)
            .then(
                when {
                    primary -> Modifier.background(GoldBrush)
                    selected -> Modifier.background(Gold.copy(alpha = 0.14f)).border(1.dp, Gold.copy(alpha = 0.6f), CircleShape)
                    else -> Modifier.background(Glass).border(1.dp, Hairline, CircleShape)
                }
            )
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Text(text, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
    }
}

/** Primary CTA: gold gradient with warm glow. [secondary] = glass. */
@Composable
fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, secondary: Boolean = false) {
    Box(
        modifier.fillMaxWidth().height(58.dp)
            .then(if (secondary || !enabled) Modifier else Modifier.shadow(22.dp, CircleShape, ambientColor = Amber, spotColor = Amber))
            .clip(CircleShape)
            .then(if (secondary) Modifier.background(Glass).border(1.dp, Hairline, CircleShape) else Modifier.background(GoldBrush))
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (secondary) TextHi else OnGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(Glass).border(1.dp, Hairline, shape), content = content)
}

@Composable
fun PanelRow(
    label: String,
    value: String? = null,
    icon: ImageVector? = null,
    valueColor: Color = TextLo,
    labelColor: Color = TextHi,
    iconTint: Color = Gold,
    sub: String? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 58.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        } else if (icon != null) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(iconTint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = labelColor)
            if (sub != null) Text(sub, fontSize = 13.sp, color = TextLo, modifier = Modifier.padding(top = 2.dp))
        }
        if (value != null) Text(value, color = valueColor, fontSize = 15.sp, textAlign = TextAlign.End, modifier = Modifier.padding(start = 8.dp))
        if (trailing != null) trailing()
        else if (onClick != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextLo)
    }
}

@Composable
fun PanelDivider() = HorizontalDivider(Modifier.padding(start = 16.dp), color = Hairline)

@Composable
fun SectionHeader(text: String, trailing: String? = null, onTrailing: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = TextLo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (trailing != null) Text(
            trailing, color = if (onTrailing != null) Gold else TextLo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = if (onTrailing != null) Modifier.clickable(onClick = onTrailing) else Modifier,
        )
    }
}

/** Equal-width, single-line segments. */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(CircleShape).background(Glass).border(1.dp, Hairline, CircleShape).padding(4.dp)) {
        options.forEach { o ->
            val sel = o == selected
            Box(
                Modifier.weight(1f).height(40.dp).clip(CircleShape)
                    .then(if (sel) Modifier.background(GoldBrush) else Modifier)
                    .clickable { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), color = if (sel) OnGold else TextHi, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
fun CheckDot(on: Boolean, size: Dp = 26.dp) {
    Box(
        Modifier.size(size).clip(CircleShape)
            .then(if (on) Modifier.background(GoldBrush) else Modifier.border(2.dp, TextLo.copy(alpha = 0.6f), CircleShape)),
        contentAlignment = Alignment.Center,
    ) { if (on) Icon(Icons.Default.Check, null, tint = OnGold, modifier = Modifier.size(size * 0.62f)) }
}

// ---- Logos & flags ----

fun cleanDomain(s: String) = s.trim().lowercase()
    .removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore('/')

fun googleFaviconUrl(domain: String) = "https://www.google.com/s2/favicons?domain=${Uri.encode(cleanDomain(domain))}&sz=256"

/** logo.dev when a key is configured, else Google favicons. */
fun faviconUrl(domain: String): String {
    val key = com.dues.app.BuildConfig.LOGO_DEV_KEY
    return if (key.isNotEmpty()) "https://img.logo.dev/${Uri.encode(cleanDomain(domain))}?token=$key&size=128&format=png&retina=true&fallback=404"
    else googleFaviconUrl(domain)
}

/** Some sources return a fully transparent image instead of a 404 (e.g. logo.dev for perplexity.ai). */
private fun isBlank(bmp: android.graphics.Bitmap): Boolean {
    var visible = 0
    for (i in 1..7) for (j in 1..7) {
        if (android.graphics.Color.alpha(bmp.getPixel(bmp.width * i / 8, bmp.height * j / 8)) > 24) visible++
    }
    return visible < 2
}

fun flagOf(countryCode: String): String =
    if (countryCode.length != 2) "🏳️" else countryCode.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")

/** Emoji > website favicon > gradient monogram (also the offline fallback). */
@Composable
fun Logo(name: String, domain: String, emoji: String, size: Dp = 46.dp) {
    val shape = RoundedCornerShape(size * 0.3f)
    var loaded by remember(domain) { mutableStateOf(false) }
    val c = colorFor(name)
    Box(
        Modifier.size(size).clip(shape).then(
            when {
                emoji.isNotEmpty() -> Modifier.background(GlassHi)
                loaded -> Modifier.background(Color.White)
                else -> Modifier.background(Brush.linearGradient(listOf(c, c.copy(alpha = 0.6f))))
            }
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (emoji.isNotEmpty()) {
            Text(emoji, fontSize = (size.value * 0.5f).sp)
        } else {
            if (!loaded) Text(name.take(1).uppercase(), color = OnGold, fontFamily = Display, fontSize = (size.value * 0.44f).sp, fontWeight = FontWeight.ExtraBold)
            if (domain.isNotBlank()) {
                // logo.dev first; on a miss or a blank image, fall back to Google, then to the monogram.
                var source by remember(domain) { mutableStateOf(faviconUrl(domain)) }
                val ctx = LocalContext.current
                val request = remember(source) { ImageRequest.Builder(ctx).data(source).allowHardware(false).build() }
                val google = googleFaviconUrl(domain)
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    onSuccess = { s ->
                        val bmp = runCatching { s.result.image.toBitmap() }.getOrNull()
                        if (bmp != null && isBlank(bmp)) {
                            if (source != google) source = google
                        } else loaded = true
                    },
                    onError = { if (source != google) source = google },
                    modifier = Modifier.fillMaxSize().padding(size * 0.14f).alpha(if (loaded) 1f else 0f),
                )
            }
        }
    }
}

@Composable
fun Logo(sub: Sub, size: Dp = 46.dp) = Logo(sub.name, sub.domain, sub.emoji, size)

@Composable
fun SubRow(
    sub: Sub,
    subtitle: String,
    price: String,
    onClick: () -> Unit,
    faded: Boolean = false,
    accent: Color? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Glass).border(1.dp, Hairline, shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp).alpha(if (faded) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Logo(sub)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(sub.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextHi, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = accent ?: TextLo, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        Text(price, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextHi, modifier = Modifier.padding(start = 8.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextLo.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
    }
}

// ---- Dialogs ----

@Composable
fun TextDialog(
    title: String,
    initial: String = "",
    placeholder: String = "",
    confirm: String = "Save",
    allowEmpty: Boolean = false,
    onDismiss: () -> Unit,
    onDone: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text(title, fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = { OutlinedTextField(text, { text = it }, singleLine = true, placeholder = { Text(placeholder) }) },
        confirmButton = {
            TextButton(onClick = { onDone(text.trim()); onDismiss() }, enabled = allowEmpty || text.isNotBlank()) { Text(confirm, color = Gold) }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = TextLo) } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text(title, fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = { Text(text, color = TextLo) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = Danger, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = TextLo) } },
    )
}

@Composable
fun <T> PickDialog(
    title: String,
    options: List<T>,
    label: (T) -> String,
    selected: T?,
    onDismiss: () -> Unit,
    onPick: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text(title, fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(options) { o ->
                    val sel = o == selected
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .then(if (sel) Modifier.background(Gold.copy(alpha = 0.12f)) else Modifier)
                            .clickable { onDismiss(); onPick(o) }.padding(vertical = 13.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label(o), fontSize = 16.sp, color = if (sel) Gold else TextHi, modifier = Modifier.weight(1f))
                        if (sel) Icon(Icons.Default.Check, null, tint = Gold, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Close", color = TextLo) } },
    )
}

@Composable
fun DateDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("OK", color = Gold) }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = TextLo) } },
    ) { DatePicker(state) }
}

// ---- Graphics ----

/** Penny, the coin mascot (rendered art), floating on a soft gold glow. */
@Composable
fun Penny(size: Dp = 120.dp, modifier: Modifier = Modifier, @DrawableRes pose: Int = R.drawable.penny_master, glow: Boolean = true) {
    val bob by rememberInfiniteTransition(label = "penny").animateFloat(
        -7f, 7f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob",
    )
    Box(modifier.size(size).then(if (glow) Modifier.glow(Gold, 0.28f, 0.75f) else Modifier), contentAlignment = Alignment.Center) {
        Image(
            painterResource(pose), contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer { translationY = bob * density },
        )
    }
}

/** Spending ring with rounded segments; sweeps in on first show. */
@Composable
fun Donut(segments: List<Pair<Color, Double>>, modifier: Modifier = Modifier, stroke: Dp = 16.dp) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    val total = segments.sumOf { it.second }
    Canvas(modifier) {
        val sw = stroke.toPx()
        val tl = Offset(sw / 2, sw / 2)
        val arc = Size(size.width - sw, size.height - sw)
        drawArc(Color.White.copy(alpha = 0.07f), 0f, 360f, false, tl, arc, style = Stroke(sw))
        if (total <= 0) return@Canvas
        val gap = if (segments.size > 1) 9f else 0f
        var start = -90f
        segments.forEach { (color, value) ->
            val sweep = (value / total * 360f).toFloat() * progress.value
            drawArc(color, start + gap / 2, (sweep - gap).coerceAtLeast(0.1f), false, tl, arc, style = Stroke(sw, cap = StrokeCap.Round))
            start += sweep
        }
    }
}

/** Floating glass navigation. */
@Composable
fun BottomPill(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        "Subscriptions" to Icons.Outlined.DonutLarge,
        "Calendar" to Icons.Outlined.CalendarMonth,
        "Settings" to Icons.Outlined.Tune,
    )
    Row(
        modifier.navigationBarsPadding().padding(bottom = 14.dp)
            .shadow(24.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(CircleShape).background(Color(0xF01B1611)).border(1.dp, Hairline, CircleShape).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        items.forEachIndexed { i, (label, icon) ->
            val sel = i == selected
            // No ripple: a haptic tick plus an animated gold highlight.
            val glow by androidx.compose.animation.core.animateFloatAsState(if (sel) 1f else 0f, androidx.compose.animation.core.spring(stiffness = 500f), label = "tab")
            Row(
                Modifier.clip(CircleShape)
                    .drawBehind { if (glow > 0f) drawRect(GoldBrush, alpha = glow) }
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (!sel) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onSelect(i)
                        }
                    }
                    .animateContentSize(androidx.compose.animation.core.spring(stiffness = 500f))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, label, tint = androidx.compose.ui.graphics.lerp(TextLo, OnGold, glow))
                if (sel) {
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = OnGold, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                }
            }
        }
    }
}

/**
 * Spending "orbit": tick dial + glowing category arcs, with subscription logos slowly orbiting outside.
 * Rotation runs in the graphics layer only (no recomposition per frame).
 */
@Composable
fun SpendingOrbit(
    segments: List<Pair<Color, Double>>,
    logos: List<Sub>,
    modifier: Modifier = Modifier,
    size: Dp = 300.dp,
    center: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
    val spin by rememberInfiniteTransition(label = "orbit").animateFloat(
        0f, 360f, infiniteRepeatable(tween(60000, easing = androidx.compose.animation.core.LinearEasing)), label = "spin",
    )
    val total = segments.sumOf { it.second }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = this.center
            val ringR = this.size.minDimension * 0.36f
            val sw = 14.dp.toPx()
            // Dial ticks
            val tickOuter = ringR + sw * 1.35f
            for (i in 0 until 72) {
                val a = Math.toRadians(i * 5.0).toFloat()
                val len = if (i % 6 == 0) 7.dp.toPx() else 3.dp.toPx()
                val dir = Offset(kotlin.math.cos(a), kotlin.math.sin(a))
                drawLine(
                    Color.White.copy(alpha = if (i % 6 == 0) 0.22f else 0.10f),
                    c + dir * tickOuter, c + dir * (tickOuter + len), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round,
                )
            }
            // Faint outer orbit path for the logos
            drawCircle(Color.White.copy(alpha = 0.06f), this.size.minDimension * 0.47f, c, style = Stroke(1.dp.toPx()))
            // Track
            val tl = Offset(c.x - ringR, c.y - ringR)
            val arcSize = Size(ringR * 2, ringR * 2)
            drawArc(Color.White.copy(alpha = 0.07f), 0f, 360f, false, tl, arcSize, style = Stroke(sw))
            if (total > 0) {
                val gap = if (segments.size > 1) 10f else 0f
                // Rotate so 0° is 12 o'clock: arcs and their gradients start at the top with no seam.
                rotate(-90f, c) {
                    var start = 0f
                    segments.forEach { (color, value) ->
                        val sweep = (value / total * 360f).toFloat() * progress.value
                        val s = (sweep - gap).coerceAtLeast(0.1f)
                        val f0 = (start + gap / 2) / 360f
                        val f1 = ((start + gap / 2 + s) / 360f).coerceAtMost(1f)
                        drawArc(color.copy(alpha = 0.10f), start + gap / 2, s, false, tl, arcSize, style = Stroke(sw * 2.2f, cap = StrokeCap.Round))
                        drawArc(
                            Brush.sweepGradient(
                                0f to color.copy(alpha = 0.55f), f0 to color.copy(alpha = 0.55f),
                                f1 to androidx.compose.ui.graphics.lerp(color, Color.White, 0.35f), 1f to color, center = c,
                            ),
                            start + gap / 2, s, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round),
                        )
                        start += sweep
                    }
                }
            }
        }
        // Orbiting logos
        val shown = logos.take(8)
        if (shown.isNotEmpty()) {
            Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin }) {
                shown.forEachIndexed { i, sub ->
                    val a = Math.toRadians(i * 360.0 / shown.size - 90)
                    val r = size.value * 0.47f
                    Box(
                        Modifier.align(Alignment.Center)
                            .offset(x = (r * kotlin.math.cos(a)).dp, y = (r * kotlin.math.sin(a)).dp)
                            .graphicsLayer { rotationZ = -spin }
                            .shadow(10.dp, RoundedCornerShape(10.dp), ambientColor = Color.Black, spotColor = Color.Black),
                    ) { Logo(sub, 30.dp) }
                }
            }
        }
        center()
    }
}

