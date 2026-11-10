package com.tungsten.fcl.pclf.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tungsten.fcl.R
import com.tungsten.fcl.pclf.theme.PclfTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text

enum class PclMainTab(@StringRes val labelRes: Int) {
    LAUNCH(R.string.pclf_tab_launch),
    DOWNLOAD(R.string.pclf_tab_download),
    LINK(R.string.pclf_tab_link),
    SETUP(R.string.pclf_tab_setup),
    MORE(R.string.pclf_tab_more)
}

/**
 * Authentic PCL2 Top Title Bar (`PanTitle`):
 * - Left: Crisp `PCLF` title (or back arrow when inside a subpage)
 * - Center: 27dp white pill radio tabs
 * - Right: Active download progress pill
 */
@Composable
fun PclTopBar(
    selectedTab: PclMainTab,
    subpageTitle: String?,
    hasDownloadTasks: Boolean,
    downloadProgress: Float,
    onSelectTab: (PclMainTab) -> Unit,
    onBackFromSubpage: () -> Unit,
    onToggleDownloadPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PclfTheme.colors
    val topBarInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(colors.titleBarStart, colors.titleBarEnd)
                )
            )
            .windowInsetsPadding(topBarInsets)
            .height(46.dp)
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (subpageTitle != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onBackFromSubpage)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "←",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = subpageTitle,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = "PCLF",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (subpageTitle == null) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PclMainTab.entries.forEach { tab ->
                        PclTitleTabPill(
                            label = stringResource(tab.labelRes),
                            selected = selectedTab == tab,
                            accentColor = colors.titleBarStart,
                            onClick = { onSelectTab(tab) }
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (hasDownloadTasks) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(27.dp)
                            .clip(RoundedCornerShape(13.5.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .clickable(onClick = onToggleDownloadPanel)
                            .padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = "↓ ${(downloadProgress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PclTitleTabPill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    val bg by animateColorAsState(
        targetValue = if (selected) Color.White else Color.Transparent
    )
    val fg by animateColorAsState(
        targetValue = if (selected) accentColor else Color.White.copy(alpha = 0.92f)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(27.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(13.5.dp))
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 13.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/**
 * Authentic PCL2 `MyButton`:
 * Crisp 3.dp corners, 1.dp neutral border, translucent white fill, spring press feedback.
 */
@Composable
fun PclButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    height: Dp = 34.dp
) {
    val colors = PclfTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(3.dp))
            .background(colors.buttonBg)
            .border(1.dp, colors.buttonBorderNeutral, RoundedCornerShape(3.dp))
            .combinedClickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) colors.textPrimary else colors.textSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Authentic PCL2 `BtnLaunch`:
 * - Idle: White/translucent background with 1.dp accent border, accent title, grey subtitle below.
 * - Pressed or Launching (`isActive`): Smoothly transitions to solid accent fill with white text.
 */
@Composable
fun PclLaunchButton(
    title: String,
    versionSubtitle: String,
    isLoadingVersion: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val colors = PclfTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val filled = pressed || isActive
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    val bg by animateColorAsState(
        targetValue = if (filled) colors.accent else colors.buttonBg
    )
    val titleColor by animateColorAsState(
        targetValue = if (filled) Color.White else colors.accent
    )
    val subColor by animateColorAsState(
        targetValue = if (filled) Color.White.copy(alpha = 0.85f) else colors.textSecondary
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .border(1.dp, colors.buttonBorderAccent, RoundedCornerShape(3.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = if (isLoadingVersion) stringResource(R.string.pclf_loading_version) else versionSubtitle,
                color = subColor,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * PCL2 `MyCard` powered by Miuix [Card] with crisp borders.
 */
@Composable
fun PclCard(
    title: String,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = PclfTheme.colors
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp)),
        cornerRadius = 8.dp,
        insideMargin = PaddingValues(14.dp),
        colors = CardDefaults.defaultColors(
            color = colors.cardBg,
            contentColor = colors.textPrimary
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            trailingAction?.invoke()
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = colors.cardBorder)
        Spacer(Modifier.height(10.dp))
        content()
    }
}
