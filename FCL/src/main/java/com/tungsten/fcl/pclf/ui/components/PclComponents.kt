package com.tungsten.fcl.pclf.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tungsten.fcl.pclf.theme.PclfTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight

enum class PclMainTab(val label: String) {
    LAUNCH("启动"),
    DOWNLOAD("下载"),
    LINK("联机"),
    SETUP("设置"),
    MORE("更多")
}

/**
 * Authentic PCL2 Top Title Bar (`PanTitle`):
 * - Left: Stylized `PCL` logo (or back arrow when inside a subpage)
 * - Center: 27dp white pill radio tabs (`启动`, `下载`, `联机`, `设置`, `更多`)
 * - Right: Active download progress pill & utility actions
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
            .height(48.dp)
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Subpage Back or PCL Brand Mark
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "PCL",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Fold",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Center: PCL White Pill Radio Buttons (27.dp height, 13.5.dp radius)
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
                            label = tab.label,
                            selected = selectedTab == tab,
                            accentColor = colors.titleBarStart,
                            onClick = { onSelectTab(tab) }
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            // Right: Download Wave/Progress Pill when tasks exist
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
            .padding(horizontal = 12.dp)
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
 * Crisp 4.dp corners, 1.dp accent border, translucent fill, and Miuix-style spring press feedback.
 */
@Composable
fun PclButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    height: Dp = 36.dp
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
            .clip(RoundedCornerShape(4.dp))
            .background(colors.buttonBg)
            .border(1.dp, colors.buttonBorder, RoundedCornerShape(4.dp))
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
            color = if (enabled) colors.accent else colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Authentic PCL2 `BtnLaunch`:
 * 54.dp height, 4.dp corners, bold "启动游戏" title + version subtitle nestled inside the bottom.
 */
@Composable
fun PclLaunchButton(
    title: String,
    versionSubtitle: String,
    isLoadingVersion: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PclfTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(4.dp, RoundedCornerShape(4.dp), clip = false)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(colors.launchFillStart, colors.launchFillEnd)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = if (isLoadingVersion) "正在加载版本..." else versionSubtitle,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * PCL2 `MyCard` powered by Miuix [Card] with PCL's characteristic left accent bar in the header.
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.accent)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            trailingAction?.invoke()
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = colors.cardBorder)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

/**
 * Authentic PCL2 `MyHint` banner.
 */
@Composable
fun PclHintBanner(
    text: String,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = PclfTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.accent.copy(alpha = if (colors.isDark) 0.18f else 0.10f))
            .border(1.dp, colors.accent.copy(alpha = 0.28f), RoundedCornerShape(6.dp))
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(42.dp)
                .background(colors.accent)
        )
        Text(
            text = text,
            color = colors.textPrimary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
        if (onDismiss != null) {
            Text(
                text = "✕",
                color = colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}
