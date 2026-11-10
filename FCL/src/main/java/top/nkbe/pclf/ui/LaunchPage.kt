package top.nkbe.pclf.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mio.download.DownloadTaskInfo
import com.tungsten.fcl.R
import com.tungsten.fclcore.auth.Account
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LaunchActionButton
import top.nkbe.pclf.ui.components.LauncherButton
import top.nkbe.pclf.ui.components.SectionCard
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.window.WindowListPopup

enum class AccountTab {
    MICROSOFT,
    OFFLINE,
    EXTERNAL
}

@Immutable
data class LaunchUiState(
    val activeAccountTab: AccountTab = AccountTab.MICROSOFT,
    val selectedAccount: Account? = null,
    val microsoftAccounts: List<Account> = emptyList(),
    val offlineAccounts: List<Account> = emptyList(),
    val externalAccounts: List<Account> = emptyList(),
    val accountAvatar: ImageBitmap? = null,
    val selectedVersionName: String = "",
    val versionTags: List<String> = emptyList(),
    val versionIcon: ImageBitmap? = null,
    val hasSelectedVersion: Boolean = false,
    val isVersionLoading: Boolean = false,
    val isLaunching: Boolean = false,
    val rendererName: String = "",
    val driverName: String = "",
    val javaVersion: String = "",
    val memorySummary: String = "",
    val downloadTasks: List<DownloadTaskInfo> = emptyList(),
    val downloadProgress: Float = 0f
)

/**
 * Two-column Launch home:
 * - Left white flush sidebar (`256.dp`) with account tabs/switcher + primary launch controls
 * - Right soft-tinted canvas with environment overview & tips carousel constrained to `460.dp`
 */
@Composable
fun LaunchPage(
    state: LaunchUiState,
    onSelectAccountTab: (AccountTab) -> Unit,
    onSelectAccount: (Account) -> Unit,
    onAddAccountForTab: (AccountTab) -> Unit,
    onManageAllAccounts: () -> Unit,
    onLaunchClick: () -> Unit,
    onLaunchLongClick: () -> Unit,
    onVersionSelectClick: () -> Unit,
    onVersionLongClick: () -> Unit,
    onVersionSettingClick: () -> Unit,
    onOpenDownloadPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    val displayVersionName = state.selectedVersionName.ifBlank { stringResource(R.string.version_no_version) }
    var tipIndex by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 560.dp

        if (isExpanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .background(colors.sidebarBg)
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Left))
                        .width(256.dp)
                        .fillMaxHeight()
                ) {
                    val isShortLandscape = maxHeight < 390.dp
                    LeftLaunchPanel(
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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    RightCardsColumn(
                        state = state,
                        displayVersionName = displayVersionName,
                        tipIndex = tipIndex,
                        onNextTip = { tipIndex = (tipIndex + 1) % 14 },
                        onChangeRenderer = onLaunchLongClick,
                        onOpenDownloadPanel = onOpenDownloadPanel,
                        modifier = Modifier.widthIn(max = 460.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.sidebarBg)
                        .border(0.5.dp, colors.sidebarBorder)
                ) {
                    LeftLaunchPanel(
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RightCardsColumn(
                        state = state,
                        displayVersionName = displayVersionName,
                        tipIndex = tipIndex,
                        onNextTip = { tipIndex = (tipIndex + 1) % 14 },
                        onChangeRenderer = onLaunchLongClick,
                        onOpenDownloadPanel = onOpenDownloadPanel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RightCardsColumn(
    state: LaunchUiState,
    displayVersionName: String,
    tipIndex: Int,
    onNextTip: () -> Unit,
    onChangeRenderer: () -> Unit,
    onOpenDownloadPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    val tips = listOf(
        stringResource(R.string.pclf_tip_1),
        stringResource(R.string.pclf_tip_2),
        stringResource(R.string.pclf_tip_3),
        stringResource(R.string.pclf_tip_4),
        stringResource(R.string.pclf_tip_5),
        stringResource(R.string.pclf_tip_6),
        stringResource(R.string.pclf_tip_7),
        stringResource(R.string.pclf_tip_8),
        stringResource(R.string.pclf_tip_9),
        stringResource(R.string.pclf_tip_10),
        stringResource(R.string.pclf_tip_11),
        stringResource(R.string.pclf_tip_12),
        stringResource(R.string.pclf_tip_13),
        stringResource(R.string.pclf_tip_14)
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.downloadTasks.isNotEmpty()) {
            SectionCard(
                title = stringResource(R.string.pclf_download_tasks, state.downloadTasks.size),
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

        SectionCard(
            title = stringResource(R.string.pclf_env_title),
            trailingAction = {
                Text(
                    text = stringResource(R.string.pclf_switch_renderer),
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
                        contentDescription = displayVersionName,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayVersionName,
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
                                        .background(colors.accent.copy(alpha = 0.11f))
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
                    } else if (!state.hasSelectedVersion) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.pclf_env_no_version_hint),
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EnvTag(
                    label = stringResource(R.string.pclf_env_renderer),
                    value = state.rendererName.ifBlank { stringResource(R.string.pclf_env_auto) },
                    onClick = onChangeRenderer,
                    modifier = Modifier.weight(1.2f)
                )
                EnvTag(
                    label = stringResource(R.string.pclf_env_driver),
                    value = state.driverName.ifBlank { stringResource(R.string.pclf_env_system_driver) },
                    modifier = Modifier.weight(1f)
                )
                EnvTag(
                    label = stringResource(R.string.pclf_env_java),
                    value = state.javaVersion.ifBlank { stringResource(R.string.pclf_env_auto) },
                    modifier = Modifier.weight(0.8f)
                )
                EnvTag(
                    label = stringResource(R.string.pclf_env_memory),
                    value = state.memorySummary.ifBlank { stringResource(R.string.pclf_env_auto) },
                    modifier = Modifier.weight(0.9f)
                )
            }
        }

        SectionCard(
            title = stringResource(R.string.pclf_tips_title),
            trailingAction = {
                Text(
                    text = stringResource(R.string.pclf_tips_next),
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
                targetState = tips[tipIndex % tips.size],
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TipAnim"
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
}

@Composable
private fun EnvTag(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = LauncherTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.accent.copy(alpha = if (colors.isDark) 0.14f else 0.07f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 7.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 10.sp
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LeftLaunchPanel(
    state: LaunchUiState,
    displayVersionName: String,
    compactHeight: Boolean,
    onSelectAccountTab: (AccountTab) -> Unit,
    onSelectAccount: (Account) -> Unit,
    onAddAccountForTab: (AccountTab) -> Unit,
    onManageAllAccounts: () -> Unit,
    onLaunchClick: () -> Unit,
    onLaunchLongClick: () -> Unit,
    onVersionSelectClick: () -> Unit,
    onVersionLongClick: () -> Unit,
    onVersionSettingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    val avatarSize = if (compactHeight) 56.dp else 66.dp
    val tabAccounts = when (state.activeAccountTab) {
        AccountTab.MICROSOFT -> state.microsoftAccounts
        AccountTab.OFFLINE -> state.offlineAccounts
        AccountTab.EXTERNAL -> state.externalAccounts
    }
    var showAccountMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LoginModePill(
                label = stringResource(R.string.pclf_login_official),
                selected = state.activeAccountTab == AccountTab.MICROSOFT,
                accentColor = colors.accent,
                inactiveTextColor = colors.accent,
                onClick = { onSelectAccountTab(AccountTab.MICROSOFT) }
            )
            LoginModePill(
                label = stringResource(R.string.pclf_login_offline),
                selected = state.activeAccountTab == AccountTab.OFFLINE,
                accentColor = colors.accent,
                inactiveTextColor = colors.accent,
                onClick = { onSelectAccountTab(AccountTab.OFFLINE) }
            )
            if (state.externalAccounts.isNotEmpty()) {
                LoginModePill(
                    label = stringResource(R.string.pclf_login_external),
                    selected = state.activeAccountTab == AccountTab.EXTERNAL,
                    accentColor = colors.accent,
                    inactiveTextColor = colors.accent,
                    onClick = { onSelectAccountTab(AccountTab.EXTERNAL) }
                )
            }
        }

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

                val addLabelText = "+ " + when (state.activeAccountTab) {
                    AccountTab.MICROSOFT -> stringResource(R.string.pclf_add_microsoft)
                    AccountTab.OFFLINE -> stringResource(R.string.pclf_add_offline)
                    AccountTab.EXTERNAL -> stringResource(R.string.pclf_manage_accounts)
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (compactHeight) 8.dp else 14.dp)
            ) {
                val addLabel = when (state.activeAccountTab) {
                    AccountTab.MICROSOFT -> stringResource(R.string.pclf_add_microsoft)
                    AccountTab.OFFLINE -> stringResource(R.string.pclf_add_offline)
                    AccountTab.EXTERNAL -> stringResource(R.string.pclf_manage_accounts)
                }
                LauncherButton(
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

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LaunchActionButton(
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
                LauncherButton(
                    text = stringResource(R.string.pclf_btn_version_select),
                    onClick = onVersionSelectClick,
                    onLongClick = onVersionLongClick,
                    modifier = Modifier.weight(1f),
                    height = 34.dp
                )
                LauncherButton(
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
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
