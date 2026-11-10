package com.tungsten.fcl.pclf.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mio.download.DownloadTaskInfo
import com.tungsten.fcl.pclf.theme.PclfTheme
import com.tungsten.fcl.pclf.ui.components.PclButton
import com.tungsten.fcl.pclf.ui.components.PclCard
import com.tungsten.fcl.pclf.ui.components.PclHintBanner
import com.tungsten.fcl.pclf.ui.components.PclLaunchButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import java.util.Calendar

@Immutable
data class LaunchUiState(
    val accountName: String = "未登录",
    val accountTypeBadge: String = "添加账号",
    val accountHint: String = "点击选择或添加账号",
    val accountAvatar: ImageBitmap? = null,
    val selectedVersionName: String = "未选择版本",
    val versionDetail: String = "请先安装或选择一个游戏版本",
    val versionTags: List<String> = emptyList(),
    val hasSelectedVersion: Boolean = false,
    val versionIcon: ImageBitmap? = null,
    val isVersionLoading: Boolean = false,
    val rendererName: String = "默认",
    val driverName: String = "系统驱动",
    val javaVersion: String = "自动",
    val memorySummary: String = "自动分配",
    val downloadTasks: List<DownloadTaskInfo> = emptyList(),
    val downloadProgress: Float = 0f
)

private val PCL_TIPS = listOf(
    "长按「启动游戏」大按钮，可以临时选择或切换当前图形渲染器。",
    "长按「版本选择」按钮，可以直接在主界面弹出版本快速切换列表。",
    "高通骁龙设备配合 Turnip 驱动 + Zink/MG 渲染器，通常可以获得更流畅的光影帧率。",
    "遇到游戏闪退时，可在「更多 → 分享最新游戏日志」快速导出 latest_game.log 排查报错 Mod。",
    "控制布局支持直接导入 ZalithLauncher 2 (ZL2) 控制方案并自动完成语义转换。",
    "本启动器完全免费开源，请勿向任何倒卖「PCL 手机版」的商家付费购买！"
)

/**
 * Authentic PCL2 `PageLaunchLeft` + `PageLaunchRight` adapted responsively for landscape & foldable screens.
 */
@Composable
fun PclfLaunchPage(
    state: LaunchUiState,
    onAccountClick: () -> Unit,
    onLaunchClick: () -> Unit,
    onLaunchLongClick: () -> Unit,
    onVersionSelectClick: () -> Unit,
    onVersionLongClick: () -> Unit,
    onVersionSettingClick: () -> Unit,
    onOpenModManage: () -> Unit,
    onOpenController: () -> Unit,
    onOpenDownloadPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PclfTheme.colors
    var showAntiResellHint by remember { mutableStateOf(true) }
    var tipIndex by remember { mutableIntStateOf(0) }
    val horizontalSafeInsets = WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 600.dp

        if (isExpanded) {
            // ==================== 横屏 / 折叠展开：经典 PCL2 左右双栏 ====================
            Row(modifier = Modifier.fillMaxSize()) {
                // 1. 贴边满高左侧栏 (PanMainLeft)
                BoxWithConstraints(
                    modifier = Modifier
                        .background(colors.sidebarBg)
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Left))
                        .width(292.dp)
                        .fillMaxHeight()
                ) {
                    val isShortLandscape = maxHeight < 390.dp
                    PclLeftLaunchPanel(
                        state = state,
                        compactHeight = isShortLandscape,
                        onAccountClick = onAccountClick,
                        onLaunchClick = onLaunchClick,
                        onLaunchLongClick = onLaunchLongClick,
                        onVersionSelectClick = onVersionSelectClick,
                        onVersionLongClick = onVersionLongClick,
                        onVersionSettingClick = onVersionSettingClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                horizontal = 18.dp,
                                vertical = if (isShortLandscape) 12.dp else 20.dp
                            )
                    )
                }

                // PCL 经典 4dp 垂直分割阴影线 (RectLeftShadow)
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Black.copy(alpha = if (colors.isDark) 0.28f else 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 2. 右侧开放式壁纸与卡片区 (PageLaunchRight)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PclRightInfoCards(
                        state = state,
                        showHint = showAntiResellHint,
                        currentTip = PCL_TIPS[tipIndex % PCL_TIPS.size],
                        onNextTip = { tipIndex = (tipIndex + 1) % PCL_TIPS.size },
                        onDismissHint = { showAntiResellHint = false },
                        onOpenDownloadPanel = onOpenDownloadPanel,
                        onChangeRenderer = onLaunchLongClick,
                        onOpenModManage = onOpenModManage,
                        onOpenController = onOpenController
                    )
                }
            }
        } else {
            // ==================== 紧凑视图：单手纵向流 ====================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(horizontalSafeInsets)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.sidebarBg)
                        .border(0.5.dp, colors.sidebarBorder)
                ) {
                    PclCompactLaunchPanel(
                        state = state,
                        onAccountClick = onAccountClick,
                        onLaunchClick = onLaunchClick,
                        onLaunchLongClick = onLaunchLongClick,
                        onVersionSelectClick = onVersionSelectClick,
                        onVersionLongClick = onVersionLongClick,
                        onVersionSettingClick = onVersionSettingClick,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PclRightInfoCards(
                        state = state,
                        showHint = showAntiResellHint,
                        currentTip = PCL_TIPS[tipIndex % PCL_TIPS.size],
                        onNextTip = { tipIndex = (tipIndex + 1) % PCL_TIPS.size },
                        onDismissHint = { showAntiResellHint = false },
                        onOpenDownloadPanel = onOpenDownloadPanel,
                        onChangeRenderer = onLaunchLongClick,
                        onOpenModManage = onOpenModManage,
                        onOpenController = onOpenController
                    )
                }
            }
        }
    }
}

/**
 * 严格对应 PCL2 `PageLaunchLeft.xaml` 结构：
 * 1. 顶部账号类型胶囊 (`PanTypeOne`)
 * 2. 中间玩家皮肤头像与名称 (`PanLogin`)
 * 3. 下方 `BtnLaunch`（启动游戏 + 版本名）在上方
 * 4. 最底部 `BtnVersion`（版本选择）+ `BtnMore`（版本设置）并排
 */
@Composable
private fun PclLeftLaunchPanel(
    state: LaunchUiState,
    compactHeight: Boolean,
    onAccountClick: () -> Unit,
    onLaunchClick: () -> Unit,
    onLaunchLongClick: () -> Unit,
    onVersionSelectClick: () -> Unit,
    onVersionLongClick: () -> Unit,
    onVersionSettingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PclfTheme.colors
    val avatarSize = if (compactHeight) 56.dp else 72.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 顶部：登录方式胶囊 (`PanTypeOne`)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(colors.accent.copy(alpha = 0.12f))
                .border(1.dp, colors.accent.copy(alpha = 0.24f), RoundedCornerShape(14.dp))
                .clickable(onClick = onAccountClick)
                .padding(horizontal = 12.dp, vertical = if (compactHeight) 4.dp else 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = state.accountTypeBadge,
                    color = colors.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 中间：Minecraft 像素风头像与账号信息 (`PanLogin`)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAccountClick)
                .padding(vertical = if (compactHeight) 6.dp else 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(avatarSize)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.cardBg)
                    .border(1.5.dp, colors.accent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            ) {
                if (state.accountAvatar != null) {
                    Image(
                        bitmap = state.accountAvatar,
                        contentDescription = state.accountName,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = state.accountName.take(1).uppercase(),
                        color = colors.accent,
                        fontSize = if (compactHeight) 22.sp else 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(if (compactHeight) 6.dp else 9.dp))
            Text(
                text = state.accountName,
                color = colors.textPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!compactHeight) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = state.accountHint,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 底部：`BtnLaunch` 在上，`BtnVersion` + `BtnMore` 在下
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (compactHeight) 8.dp else 10.dp)
        ) {
            PclLaunchButton(
                title = "启动游戏",
                versionSubtitle = state.selectedVersionName,
                isLoadingVersion = state.isVersionLoading,
                onClick = onLaunchClick,
                onLongClick = onLaunchLongClick
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PclButton(
                    text = "版本选择",
                    onClick = onVersionSelectClick,
                    onLongClick = onVersionLongClick,
                    modifier = Modifier.weight(1.15f),
                    height = 35.dp
                )
                PclButton(
                    text = "版本设置",
                    onClick = onVersionSettingClick,
                    modifier = Modifier.weight(1f),
                    height = 35.dp
                )
            }
        }
    }
}

@Composable
private fun PclCompactLaunchPanel(
    state: LaunchUiState,
    onAccountClick: () -> Unit,
    onLaunchClick: () -> Unit,
    onLaunchLongClick: () -> Unit,
    onVersionSelectClick: () -> Unit,
    onVersionLongClick: () -> Unit,
    onVersionSettingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PclfTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAccountClick)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.cardBg)
                    .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            ) {
                if (state.accountAvatar != null) {
                    Image(
                        bitmap = state.accountAvatar,
                        contentDescription = state.accountName,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = state.accountName.take(1).uppercase(),
                        color = colors.accent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.accountName,
                    color = colors.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.accountHint,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.accent.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = state.accountTypeBadge,
                    color = colors.accent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        PclLaunchButton(
            title = "启动游戏",
            versionSubtitle = state.selectedVersionName,
            isLoadingVersion = state.isVersionLoading,
            onClick = onLaunchClick,
            onLongClick = onLaunchLongClick
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PclButton(
                text = "版本选择",
                onClick = onVersionSelectClick,
                onLongClick = onVersionLongClick,
                modifier = Modifier.weight(1.15f),
                height = 35.dp
            )
            PclButton(
                text = "版本设置",
                onClick = onVersionSettingClick,
                modifier = Modifier.weight(1f),
                height = 35.dp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PclRightInfoCards(
    state: LaunchUiState,
    showHint: Boolean,
    currentTip: String,
    onNextTip: () -> Unit,
    onDismissHint: () -> Unit,
    onOpenDownloadPanel: () -> Unit,
    onChangeRenderer: () -> Unit,
    onOpenModManage: () -> Unit,
    onOpenController: () -> Unit
) {
    val colors = PclfTheme.colors
    val greeting = remember(state.accountName) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val period = when (hour) {
            in 5..11 -> "早上好"
            in 12..17 -> "下午好"
            else -> "晚上好"
        }
        "$period，${state.accountName}"
    }

    // 顶部欢迎问候与快速操作条
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = greeting,
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.hasSelectedVersion) {
                PclButton(
                    text = "模组管理",
                    onClick = onOpenModManage,
                    height = 28.dp
                )
            }
            PclButton(
                text = "按键布局",
                onClick = onOpenController,
                height = 28.dp
            )
        }
    }

    if (showHint) {
        PclHintBanner(
            text = "ProCraftLauncherFold (PCLF) 完全免费开源，融合 PCL2 界面风格与 FCL 核心引擎。请勿向任何倒卖商付费购买！",
            onDismiss = onDismissHint
        )
    }

    // 如果有正在进行的下载任务，优先显示在右侧顶部
    if (state.downloadTasks.isNotEmpty()) {
        PclCard(
            title = "正在进行下载 (${state.downloadTasks.size})",
            trailingAction = {
                Text(
                    text = "查看详情 →",
                    color = colors.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onOpenDownloadPanel)
                )
            }
        ) {
            LinearProgressIndicator(
                progress = state.downloadProgress.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "总进度 ${(state.downloadProgress * 100).toInt()}% • 点击右上角徽标或此处管理下载任务",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
    }

    // 当前版本环境摘要卡片
    PclCard(
        title = "当前游戏环境",
        trailingAction = {
            Text(
                text = "切换渲染器",
                color = colors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onChangeRenderer)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.versionIcon != null) {
                Image(
                    bitmap = state.versionIcon,
                    contentDescription = state.selectedVersionName,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.selectedVersionName,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.versionTags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.versionTags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.accent.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag,
                                    color = colors.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = state.versionDetail,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EnvTag(
                label = "渲染器",
                value = state.rendererName,
                onClick = onChangeRenderer,
                modifier = Modifier.weight(1.2f)
            )
            EnvTag(label = "驱动", value = state.driverName, modifier = Modifier.weight(1f))
            EnvTag(label = "Java", value = state.javaVersion, modifier = Modifier.weight(0.8f))
            EnvTag(label = "内存", value = state.memorySummary, modifier = Modifier.weight(0.9f))
        }
    }

    // PCL 经典「你知道吗」小贴士轮播卡片
    PclCard(
        title = "你知道吗",
        trailingAction = {
            Text(
                text = "换一条 ↻",
                color = colors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onNextTip)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    ) {
        AnimatedContent(
            targetState = currentTip,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "TipTransition"
        ) { tip ->
            Text(
                text = tip,
                color = colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun EnvTag(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = PclfTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.accent.copy(alpha = if (colors.isDark) 0.14f else 0.07f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 10.5.sp
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
