package com.tungsten.fcl.pclf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tungsten.fcl.R
import com.tungsten.fcl.pclf.theme.PclfTheme
import com.tungsten.fcl.pclf.ui.components.PclCard
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference

private enum class MoreSection {
    TOOLS,
    DIAGNOSTICS,
    ABOUT
}

/**
 * Authentic PCL2 `更多` two-pane layout (matching screenshots 3 & 4):
 * - Left white sidebar with 3.dp vertical accent indicator bar
 * - Right soft-tinted canvas with crisp white Miuix cards
 */
@Composable
fun PclfMorePage(
    versionName: String,
    onOpenController: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenVersions: () -> Unit,
    onShareLatestLog: () -> Unit,
    onOpenShell: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PclfTheme.colors
    var section by remember { mutableStateOf(MoreSection.TOOLS) }

    Row(modifier = modifier.fillMaxSize()) {
        // Left sub-navigation sidebar (`172.dp` white flush pane)
        Column(
            modifier = Modifier
                .background(colors.sidebarBg)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Left))
                .width(172.dp)
                .fillMaxHeight()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MoreNavItem(
                label = stringResource(R.string.pclf_more_tools_title),
                selected = section == MoreSection.TOOLS,
                onClick = { section = MoreSection.TOOLS }
            )
            MoreNavItem(
                label = stringResource(R.string.pclf_more_diag_title),
                selected = section == MoreSection.DIAGNOSTICS,
                onClick = { section = MoreSection.DIAGNOSTICS }
            )
            MoreNavItem(
                label = stringResource(R.string.pclf_about_title),
                selected = section == MoreSection.ABOUT,
                onClick = { section = MoreSection.ABOUT }
            )
        }

        // PCL 4.dp vertical shadow divider
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = if (colors.isDark) 0.25f else 0.07f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Right content area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.widthIn(max = 680.dp)) {
                when (section) {
                    MoreSection.TOOLS -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp)),
                            cornerRadius = 10.dp,
                            insideMargin = PaddingValues(0.dp),
                            colors = CardDefaults.defaultColors(
                                color = colors.cardBg,
                                contentColor = colors.textPrimary
                            )
                        ) {
                            ArrowPreference(
                                title = stringResource(R.string.pclf_more_controller),
                                summary = stringResource(R.string.pclf_more_controller_desc),
                                onClick = onOpenController
                            )
                            ArrowPreference(
                                title = stringResource(R.string.pclf_more_account),
                                summary = stringResource(R.string.pclf_more_account_desc),
                                onClick = onOpenAccount
                            )
                            ArrowPreference(
                                title = stringResource(R.string.pclf_more_versions),
                                summary = stringResource(R.string.pclf_more_versions_desc),
                                onClick = onOpenVersions
                            )
                        }
                    }

                    MoreSection.DIAGNOSTICS -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp)),
                            cornerRadius = 10.dp,
                            insideMargin = PaddingValues(0.dp),
                            colors = CardDefaults.defaultColors(
                                color = colors.cardBg,
                                contentColor = colors.textPrimary
                            )
                        ) {
                            ArrowPreference(
                                title = stringResource(R.string.pclf_more_share_log),
                                summary = stringResource(R.string.pclf_more_share_log_desc),
                                onClick = onShareLatestLog
                            )
                            ArrowPreference(
                                title = stringResource(R.string.pclf_more_shell),
                                summary = stringResource(R.string.pclf_more_shell_desc),
                                onClick = onOpenShell
                            )
                            ArrowPreference(
                                title = stringResource(R.string.pclf_more_update),
                                summary = stringResource(R.string.pclf_more_update_desc),
                                onClick = onCheckUpdate
                            )
                        }
                    }

                    MoreSection.ABOUT -> {
                        PclCard(title = "ProCraftLauncherFold (v$versionName)") {
                            Text(
                                text = stringResource(R.string.pclf_about_body),
                                color = colors.textSecondary,
                                fontSize = 12.5.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = PclfTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
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
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
