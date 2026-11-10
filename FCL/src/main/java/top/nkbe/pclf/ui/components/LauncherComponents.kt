package top.nkbe.pclf.ui.components

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
import top.nkbe.pclf.theme.LauncherTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text

enum class MainTab(@StringRes val labelRes: Int) {
    LAUNCH(R.string.pclf_tab_launch),
    DOWNLOAD(R.string.pclf_tab_download),
    LINK(R.string.pclf_tab_link),
    SETUP(R.string.pclf_tab_setup),
    MORE(R.string.pclf_tab_more)
}

/**
 * Top title bar:
 * - Left: Launcher mark (or back arrow when inside a subpage)
 * - Center: 27dp white pill radio tabs
 * - Right: Active download progress pill
 */
@Composable
fun TopNavBar(
    selectedTab: MainTab,
    subpageTitle: String?,
    hasDownloadTasks: Boolean,
    downloadProgress: Float,
    onSelectTab: (MainTab) -> Unit,
    onBackFromSubpage: () -> Unit,
    onToggleDownloadPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
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
                    text = "ProCraft",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
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
                    MainTab.entries.forEach { tab ->
                        TitleTabPill(
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
private fun TitleTabPill(
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
 * Outlined desktop-style action button with spring press feedback.
 */
@Composable
fun LauncherButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    accentStyle: Boolean = false,
    height: Dp = 34.dp
) {
    val colors = LauncherTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    val borderColor = if (accentStyle) colors.buttonBorderAccent else colors.buttonBorderNeutral
    val bgColor = if (accentStyle && enabled) colors.accent else colors.buttonBg
    val fgColor = when {
        !enabled -> colors.textSecondary
        accentStyle -> Color.White
        else -> colors.textPrimary
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor.copy(alpha = if (enabled) 1f else 0.45f), RoundedCornerShape(4.dp))
            .combinedClickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = text,
            color = fgColor,
            fontSize = 12.5.sp,
            fontWeight = if (accentStyle) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Primary launch button:
 * - Idle: clean translucent surface with 1.dp accent border, accent title, grey subtitle below.
 * - Pressed or Launching (`isActive`): transitions to solid accent fill with white text.
 */
@Composable
fun LaunchActionButton(
    title: String,
    versionSubtitle: String,
    isLoadingVersion: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val colors = LauncherTheme.colors
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
 * Section card container backed by Miuix [Card] with crisp borders.
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LauncherTheme.colors
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

/**
 * Shared left-sidebar navigation item with 3dp vertical accent indicator bar.
 */
@Composable
fun SidebarNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable(onClick = onClick)
            .padding(end = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (selected) 20.dp else 0.dp)
                .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                .background(if (selected) colors.accent else Color.Transparent)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = if (selected) colors.accent else colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
