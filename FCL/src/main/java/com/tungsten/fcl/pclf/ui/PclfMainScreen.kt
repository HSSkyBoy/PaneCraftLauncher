package com.tungsten.fcl.pclf.ui

import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mio.download.DownloadTaskInfo
import com.tungsten.fcl.pclf.theme.PclfTheme
import com.tungsten.fcl.pclf.ui.components.PclButton
import com.tungsten.fcl.pclf.ui.components.PclMainTab
import com.tungsten.fcl.pclf.ui.components.PclTopBar
import com.tungsten.fcllibrary.component.ui.FCLCommonUI
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text

enum class PclSubpage(val title: String, val uiIndex: Int) {
    MANAGE("版本管理", 1),
    CONTROLLER("虚拟按键与控制器", 3),
    ACCOUNT("账号管理", 6),
    VERSION("版本选择", 7)
}

/**
 * Root PCLF Screen hosting:
 * - Launcher Wallpaper + PCL tint overlay
 * - Authentic PCL Top Bar (`PclTopBar`)
 * - Animated page content (`启动` / `下载` / `联机` / `设置` / `更多` + subpages)
 * - Download Task overlay sheet
 */
@Composable
fun PclfMainScreen(
    selectedTab: PclMainTab,
    activeSubpage: PclSubpage?,
    launchState: LaunchUiState,
    backgroundDrawable: BitmapDrawable?,
    showDownloadPanel: Boolean,
    appVersionName: String,
    legacyUiProvider: (Int) -> FCLCommonUI,
    onSelectTab: (PclMainTab) -> Unit,
    onOpenSubpage: (PclSubpage) -> Unit,
    onBackFromSubpage: () -> Unit,
    onAccountClick: () -> Unit,
    onLaunchClick: () -> Unit,
    onLaunchLongClick: () -> Unit,
    onVersionLongClick: () -> Unit,
    onVersionSettingClick: () -> Unit,
    onOpenModManage: () -> Unit,
    onToggleDownloadPanel: () -> Unit,
    onCancelTask: (DownloadTaskInfo) -> Unit,
    onShareLatestLog: () -> Unit,
    onOpenShell: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PclfTheme.colors

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Wallpaper / Theme Background
        val bgBitmap = remember(backgroundDrawable) {
            backgroundDrawable?.bitmap?.asImageBitmap()
        }
        if (bgBitmap != null) {
            Image(
                bitmap = bgBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (colors.isDark) Color(0x9910141B)
                        else Color(0x66EEF4FB)
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            if (colors.isDark) {
                                listOf(Color(0xFF131720), Color(0xFF0E1117))
                            } else {
                                listOf(Color(0xFFEAF2FC), Color(0xFFF6F9FD))
                            }
                        )
                    )
            )
        }

        // 2. Main Vertical Scaffold (PCL TitleBar + Content)
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            PclTopBar(
                selectedTab = selectedTab,
                subpageTitle = activeSubpage?.title,
                hasDownloadTasks = launchState.downloadTasks.isNotEmpty(),
                downloadProgress = launchState.downloadProgress,
                onSelectTab = onSelectTab,
                onBackFromSubpage = onBackFromSubpage,
                onToggleDownloadPanel = onToggleDownloadPanel
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val routeKey: Any = activeSubpage ?: selectedTab
                AnimatedContent(
                    targetState = routeKey,
                    transitionSpec = {
                        (fadeIn(tween(180)) + slideInVertically(tween(200)) { it / 28 }) togetherWith
                            fadeOut(tween(140))
                    },
                    label = "PclfPageTransition"
                ) { target ->
                    when (target) {
                        PclMainTab.LAUNCH -> PclfLaunchPage(
                            state = launchState,
                            onAccountClick = onAccountClick,
                            onLaunchClick = onLaunchClick,
                            onLaunchLongClick = onLaunchLongClick,
                            onVersionSelectClick = { onOpenSubpage(PclSubpage.VERSION) },
                            onVersionLongClick = onVersionLongClick,
                            onVersionSettingClick = onVersionSettingClick,
                            onOpenModManage = onOpenModManage,
                            onOpenController = { onOpenSubpage(PclSubpage.CONTROLLER) },
                            onOpenDownloadPanel = onToggleDownloadPanel
                        )

                        PclMainTab.DOWNLOAD -> LegacyFclUiHost(
                            ui = remember(legacyUiProvider) { legacyUiProvider(2) }
                        )

                        PclMainTab.LINK -> LegacyFclUiHost(
                            ui = remember(legacyUiProvider) { legacyUiProvider(4) }
                        )

                        PclMainTab.SETUP -> LegacyFclUiHost(
                            ui = remember(legacyUiProvider) { legacyUiProvider(5) }
                        )

                        PclMainTab.MORE -> PclfMorePage(
                            versionName = appVersionName,
                            onOpenController = { onOpenSubpage(PclSubpage.CONTROLLER) },
                            onOpenAccount = { onOpenSubpage(PclSubpage.ACCOUNT) },
                            onOpenVersions = { onOpenSubpage(PclSubpage.VERSION) },
                            onShareLatestLog = onShareLatestLog,
                            onOpenShell = onOpenShell,
                            onCheckUpdate = onCheckUpdate
                        )

                        is PclSubpage -> LegacyFclUiHost(
                            ui = remember(target, legacyUiProvider) { legacyUiProvider(target.uiIndex) }
                        )
                    }
                }
            }
        }

        // 3. Miuix Floating Download Panel Overlay
        AnimatedVisibility(
            visible = showDownloadPanel && launchState.downloadTasks.isNotEmpty(),
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 6 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { -it / 6 },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 12.dp, start = 12.dp)
        ) {
            Card(
                modifier = Modifier
                    .width(340.dp)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp)),
                cornerRadius = 14.dp,
                insideMargin = PaddingValues(14.dp),
                colors = CardDefaults.defaultColors(
                    color = colors.cardBg,
                    contentColor = colors.textPrimary
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "下载任务 (${launchState.downloadTasks.size})",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "收起 ✕",
                        color = colors.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onToggleDownloadPanel)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = colors.cardBorder)
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(launchState.downloadTasks, key = { it.id }) { info ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.accent.copy(alpha = 0.08f))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = info.title,
                                    color = colors.textPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                if (info.ready && info.installAction != null) {
                                    PclButton(
                                        text = "安装",
                                        onClick = { info.installAction.run() },
                                        height = 26.dp
                                    )
                                } else {
                                    Text(
                                        text = "取消",
                                        color = colors.accent,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier
                                            .clickable { onCancelTask(info) }
                                            .padding(4.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = info.task.progressProperty().get().toFloat().coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Safely embeds existing battle-tested FCL XML-based [FCLCommonUI] modules
 * inside the PCLF Compose shell until each subpage is rewritten in Miuix.
 */
@Composable
private fun LegacyFclUiHost(
    ui: FCLCommonUI,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { container ->
            val contentView: View = ui.contentView
            if (contentView.parent !== container) {
                (contentView.parent as? ViewGroup)?.removeView(contentView)
                container.removeAllViews()
                container.addView(
                    contentView,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
