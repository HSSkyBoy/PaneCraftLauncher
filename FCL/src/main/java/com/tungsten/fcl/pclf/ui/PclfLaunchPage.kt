package com.tungsten.fcl.pclf.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mio.download.DownloadTaskInfo
import com.tungsten.fcl.R
import com.tungsten.fcl.pclf.theme.PclfTheme
import com.tungsten.fcl.pclf.ui.components.PclButton
import com.tungsten.fcl.pclf.ui.components.PclCard
import com.tungsten.fcl.pclf.ui.components.PclLaunchButton
import com.tungsten.fclcore.auth.Account
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.window.WindowListPopup

enum class PclAccountTab {
    MICROSOFT,
    OFFLINE,
    EXTERNAL
}

@Immutable
data class LaunchUiState(
    val activeAccountTab: PclAccountTab = PclAccountTab.MICROSOFT,
    val selectedAccount: Account? = null,
    val microsoftAccounts: List<Account> = emptyList(),
    val offlineAccounts: List<Account> = emptyList(),
    val externalAccounts: List<Account> = emptyList(),
    val accountAvatar: ImageBitmap? = null,
    val selectedVersionName: String = "",
    val hasSelectedVersion: Boolean = false,
    val isVersionLoading: Boolean = false,
    val isLaunching: Boolean = false,
    val downloadTasks: List<DownloadTaskInfo> = emptyList(),
    val downloadProgress: Float = 0f
)

/**
 * 1:1 PCL2 `PageLaunchLeft` (with inline PCL account switcher/creator) + clean open `PageLaunchRight`.
 */
@Composable
fun PclfLaunchPage(
    state: LaunchUiState,
    onSelectAccountTab: (PclAccountTab) -> Unit,
    onSelectAccount: (Account) -> Unit,
    onAddAccountForTab: (PclAccountTab) -> Unit,
    onManageAllAccounts: () -> Unit,
    onLaunchClick: () -> Unit,
    onLaunchLongClick: () -> Unit,
    onVersionSelectClick: () -> Unit,
    onVersionLongClick: () -> Unit,
    onVersionSettingClick: () -> Unit,
    onOpenDownloadPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PclfTheme.colors
    val displayVersionName = state.selectedVersionName.ifBlank { stringResource(R.string.version_no_version) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 560.dp

        if (isExpanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 1. 贴边满高纯白左侧栏 (`256.dp`)
                BoxWithConstraints(
                    modifier = Modifier
                        .background(colors.sidebarBg)
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Left))
                        .width(256.dp)
                        .fillMaxHeight()
                ) {
                    val isShortLandscape = maxHeight < 390.dp
                    PclLeftLaunchPanel(
                        state = state,
                        displayVersionName = displayVersionName,
                        compactHeight = isShortLandscape,
                        onSelectAccountTab = onSelectAccountTab,
                        onSelectAccount = onSelectAccount,
                        onAddAccountForTab = onAddAccountForTab,
                        onManageAllAccounts = onManageAllAccounts,
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
                                vertical = if (isShortLandscape) 12.dp else 18.dp
                            )
                    )
                }

                // PCL 4.dp 垂直分割阴影线 (`RectLeftShadow`)
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

                // 2. 右侧开放式干净背景留白 (`PageLaunchRight`)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
                        .padding(20.dp)
                ) {
                    if (state.downloadTasks.isNotEmpty()) {
                        PclCard(
                            title = stringResource(R.string.pclf_download_tasks, state.downloadTasks.size),
                            modifier = Modifier.widthIn(max = 420.dp),
                            trailingAction = {
                                Text(
                                    text = "→",
                                    color = colors.accent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
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
                                text = "${(state.downloadProgress * 100).toInt()}%",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.sidebarBg)
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                PclLeftLaunchPanel(
                    state = state,
                    displayVersionName = displayVersionName,
                    compactHeight = false,
                    onSelectAccountTab = onSelectAccountTab,
                    onSelectAccount = onSelectAccount,
                    onAddAccountForTab = onAddAccountForTab,
                    onManageAllAccounts = onManageAllAccounts,
                    onLaunchClick = onLaunchClick,
                    onLaunchLongClick = onLaunchLongClick,
                    onVersionSelectClick = onVersionSelectClick,
                    onVersionLongClick = onVersionLongClick,
                    onVersionSettingClick = onVersionSettingClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PclLeftLaunchPanel(
    state: LaunchUiState,
    displayVersionName: String,
    compactHeight: Boolean,
    onSelectAccountTab: (PclAccountTab) -> Unit,
    onSelectAccount: (Account) -> Unit,
    onAddAccountForTab: (PclAccountTab) -> Unit,
    onManageAllAccounts: () -> Unit,
    onLaunchClick: () -> Unit,
    onLaunchLongClick: () -> Unit,
    onVersionSelectClick: () -> Unit,
    onVersionLongClick: () -> Unit,
    onVersionSettingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PclfTheme.colors
    val avatarSize = if (compactHeight) 56.dp else 66.dp
    val tabAccounts = when (state.activeAccountTab) {
        PclAccountTab.MICROSOFT -> state.microsoftAccounts
        PclAccountTab.OFFLINE -> state.offlineAccounts
        PclAccountTab.EXTERNAL -> state.externalAccounts
    }
    var showAccountMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 顶部：PCL2 经典账号类型切换胶囊 (`正版` / `离线` / 可选 `外置`)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LoginModePill(
                label = stringResource(R.string.pclf_login_official),
                selected = state.activeAccountTab == PclAccountTab.MICROSOFT,
                accentColor = colors.accent,
                inactiveTextColor = colors.accent,
                onClick = { onSelectAccountTab(PclAccountTab.MICROSOFT) }
            )
            LoginModePill(
                label = stringResource(R.string.pclf_login_offline),
                selected = state.activeAccountTab == PclAccountTab.OFFLINE,
                accentColor = colors.accent,
                inactiveTextColor = colors.accent,
                onClick = { onSelectAccountTab(PclAccountTab.OFFLINE) }
            )
            if (state.externalAccounts.isNotEmpty()) {
                LoginModePill(
                    label = stringResource(R.string.pclf_login_external),
                    selected = state.activeAccountTab == PclAccountTab.EXTERNAL,
                    accentColor = colors.accent,
                    inactiveTextColor = colors.accent,
                    onClick = { onSelectAccountTab(PclAccountTab.EXTERNAL) }
                )
            }
        }

        // 中间：PCL 内联账号区（已登录显示像素头像+名字+切换弹窗；未登录当前类型显示登录按钮）
        val activeForTab = state.selectedAccount?.takeIf { it in tabAccounts } ?: tabAccounts.firstOrNull()
        if (activeForTab != null) {
            Box(contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showAccountMenu = true }
                        .padding(horizontal = 16.dp, vertical = if (compactHeight) 6.dp else 10.dp)
                ) {
                    if (state.accountAvatar != null) {
                        Image(
                            bitmap = state.accountAvatar,
                            contentDescription = activeForTab.character,
                            contentScale = ContentScale.Crop,
                            filterQuality = FilterQuality.None,
                            modifier = Modifier.size(avatarSize)
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(avatarSize)
                                .background(colors.accent.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = activeForTab.character.take(1).uppercase(),
                                color = colors.accent,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(if (compactHeight) 6.dp else 9.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeForTab.character,
                            color = colors.textPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "▾",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // PCL 风格原位账号切换 / 添加下拉菜单 (Miuix WindowListPopup)
                val addLabelText = "+ " + when (state.activeAccountTab) {
                    PclAccountTab.MICROSOFT -> stringResource(R.string.pclf_add_microsoft)
                    PclAccountTab.OFFLINE -> stringResource(R.string.pclf_add_offline)
                    PclAccountTab.EXTERNAL -> stringResource(R.string.pclf_manage_accounts)
                }
                val manageAllText = stringResource(R.string.pclf_manage_accounts)
                WindowListPopup(
                    show = showAccountMenu,
                    popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                    onDismissRequest = { showAccountMenu = false }
                ) {
                    ListPopupColumn {
                        tabAccounts.forEachIndexed { idx, acc ->
                            DropdownImpl(
                                text = acc.character,
                                optionSize = tabAccounts.size + 2,
                                isSelected = acc == state.selectedAccount,
                                index = idx,
                                onSelectedIndexChange = {
                                    showAccountMenu = false
                                    onSelectAccount(acc)
                                }
                            )
                        }
                        DropdownImpl(
                            text = addLabelText,
                            optionSize = tabAccounts.size + 2,
                            isSelected = false,
                            index = tabAccounts.size,
                            onSelectedIndexChange = {
                                showAccountMenu = false
                                onAddAccountForTab(state.activeAccountTab)
                            }
                        )
                        DropdownImpl(
                            text = manageAllText,
                            optionSize = tabAccounts.size + 2,
                            isSelected = false,
                            index = tabAccounts.size + 1,
                            onSelectedIndexChange = {
                                showAccountMenu = false
                                onManageAllAccounts()
                            }
                        )
                    }
                }
            }
        } else {
            // 当前账号类型尚无账号：直接原位显示 PCL 登录 / 创建按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (compactHeight) 8.dp else 14.dp)
            ) {
                val addLabel = when (state.activeAccountTab) {
                    PclAccountTab.MICROSOFT -> stringResource(R.string.pclf_add_microsoft)
                    PclAccountTab.OFFLINE -> stringResource(R.string.pclf_add_offline)
                    PclAccountTab.EXTERNAL -> stringResource(R.string.pclf_manage_accounts)
                }
                PclButton(
                    text = addLabel,
                    onClick = { onAddAccountForTab(state.activeAccountTab) },
                    modifier = Modifier.fillMaxWidth(0.85f),
                    height = 36.dp
                )
                Text(
                    text = stringResource(R.string.pclf_manage_accounts),
                    color = colors.textSecondary,
                    fontSize = 11.5.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onManageAllAccounts)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // 底部：`BtnLaunch` (平时白底蓝框，按下/启动中实心蓝底) + `BtnVersion` / `BtnMore`
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PclLaunchButton(
                title = stringResource(R.string.pclf_btn_launch),
                versionSubtitle = displayVersionName,
                isLoadingVersion = state.isVersionLoading,
                isActive = state.isLaunching,
                onClick = onLaunchClick,
                onLongClick = onLaunchLongClick
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PclButton(
                    text = stringResource(R.string.pclf_btn_version_select),
                    onClick = onVersionSelectClick,
                    onLongClick = onVersionLongClick,
                    modifier = Modifier.weight(1f),
                    height = 34.dp
                )
                PclButton(
                    text = stringResource(R.string.pclf_btn_version_setting),
                    onClick = onVersionSettingClick,
                    modifier = Modifier.weight(1f),
                    height = 34.dp
                )
            }
        }
    }
}

@Composable
private fun LoginModePill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    inactiveTextColor: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(27.dp)
            .clip(RoundedCornerShape(13.5.dp))
            .background(if (selected) accentColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else inactiveTextColor,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
