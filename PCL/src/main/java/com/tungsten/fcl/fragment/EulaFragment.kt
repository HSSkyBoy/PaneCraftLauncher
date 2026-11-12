package com.tungsten.fcl.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.SplashActivity
import com.tungsten.fcl.util.LegalAgreement
import com.tungsten.fcllibrary.component.theme.ThemeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LauncherButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import java.util.regex.Pattern

class EulaFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val context = LocalContext.current
                val isDark = ThemeEngine.isNightMode(context)
                val theme = ThemeEngine.getInstance().getTheme()
                LauncherTheme(seedColorInt = theme.getColor(), darkTheme = isDark) {
                    EulaScreen(
                        onAccept = {
                            LegalAgreement.accept(context)
                            (activity as? SplashActivity)?.onLegalAgreementAccepted()
                        },
                        onDecline = {
                            activity?.finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EulaScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val colors = LauncherTheme.colors
    val documents = remember { LegalAgreement.Document.values() }

    var selectedIndex by remember { mutableIntStateOf(0) }
    var loadedContents by remember { mutableStateOf<Map<LegalAgreement.Document, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val results = documents.associateWith { doc ->
                runCatching {
                    LegalAgreement.load(context, doc)
                }.getOrElse {
                    context.getString(R.string.legal_load_failed)
                }
            }
            withContext(Dispatchers.Main) {
                loadedContents = results
                isLoading = false
            }
        }
    }

    val topBarInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvasTint)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(colors.titleBarStart, colors.titleBarEnd)
                    )
                )
                .windowInsetsPadding(topBarInsets)
                .height(44.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = stringResource(R.string.splash_title),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.70f)
                    .fillMaxHeight()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    documents.forEachIndexed { index, doc ->
                        val selected = index == selectedIndex
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) colors.accent else Color.Transparent)
                                .clickable { selectedIndex = index }
                                .padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = stringResource(doc.titleRes),
                                color = if (selected) Color.White else colors.accent,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.cardBg)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                ) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
                        }
                    } else {
                        val currentDoc = documents.getOrNull(selectedIndex) ?: documents[0]
                        val rawText = loadedContents[currentDoc].orEmpty()
                        val scrollState = rememberScrollState()

                        LaunchedEffect(selectedIndex) {
                            scrollState.scrollTo(0)
                        }

                        val annotated = remember(rawText, colors.accent, colors.textPrimary) {
                            buildAnnotatedDocument(rawText, colors.accent, colors.textPrimary)
                        }

                        ClickableText(
                            text = annotated,
                            style = TextStyle(
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = colors.textPrimary
                            ),
                            onClick = { offset ->
                                annotated.getStringAnnotations("URL", offset, offset)
                                    .firstOrNull()?.let { span ->
                                        runCatching { uriHandler.openUri(span.item) }
                                    }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(14.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.30f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.cardBg)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.legal_summary),
                        color = colors.textSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                LauncherButton(
                    text = stringResource(R.string.legal_agree),
                    onClick = onAccept,
                    enabled = !isLoading,
                    accentStyle = true,
                    height = 42.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LauncherButton(
                    text = stringResource(R.string.legal_disagree),
                    onClick = onDecline,
                    accentStyle = false,
                    height = 40.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private val URL_PATTERN = Pattern.compile("https?://[a-zA-Z0-9./_?&=#%+-]+")

private fun buildAnnotatedDocument(raw: String, accentColor: Color, textColor: Color) = buildAnnotatedString {
    val matcher = URL_PATTERN.matcher(raw)
    var lastEnd = 0
    while (matcher.find()) {
        val start = matcher.start()
        val end = matcher.end()
        if (start > lastEnd) {
            append(raw.substring(lastEnd, start))
        }
        val url = matcher.group()
        pushStringAnnotation(tag = "URL", annotation = url)
        pushStyle(SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline))
        append(url)
        pop()
        pop()
        lastEnd = end
    }
    if (lastEnd < raw.length) {
        append(raw.substring(lastEnd))
    }
}
