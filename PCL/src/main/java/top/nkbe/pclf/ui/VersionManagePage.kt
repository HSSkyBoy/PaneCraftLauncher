package top.nkbe.pclf.ui

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import com.mio.device.VulkanCheckManager
import com.mio.manager.RendererManager
import com.mio.plugin.DriverPlugin
import com.mio.ui.dialog.JavaManageDialog
import com.mio.ui.dialog.RendererSelectDialog
import com.mio.ui.dialog.VulkanCheckDialog
import com.mio.util.isAdrenoGPU
import com.mio.util.openLink
import com.mio.util.showErrorDialog
import com.mio.util.showItemSelectionDialog
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.control.SelectControllerDialog
import com.tungsten.fcl.game.FCLGameRepository
import com.tungsten.fcl.setting.Controllers
import com.tungsten.fcl.setting.Profile
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fcl.ui.manage.ManageUI
import com.tungsten.fcl.ui.version.Versions
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.event.Event
import com.tungsten.fclcore.util.Logging
import com.tungsten.fclcore.util.io.FileUtils
import com.tungsten.fclcore.util.platform.MemoryUtils
import com.tungsten.fcllibrary.browser.FileBrowser
import com.tungsten.fcllibrary.browser.options.LibMode
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog
import com.tungsten.fcllibrary.component.dialog.FullEditDialog
import com.tungsten.fcllibrary.component.ui.FCLMultiPageUI
import com.tungsten.fcllibrary.util.uploadLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LauncherButton
import top.nkbe.pclf.ui.components.SectionCard
import top.nkbe.pclf.ui.components.SidebarNavItem
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.window.WindowDialog
import java.io.File
import java.io.IOException
import java.util.logging.Level

enum class ManageSection(val legacyTabIndex: Int?) {
    OVERVIEW(null),
    SETTINGS(null),
    INSTALLER(2),
    MODS(3),
    WORLDS(4),
    RESOURCE_PACKS(5),
    SHADER_PACKS(6)
}

/**
 * PCL2-style two-pane Version Management page.
 * Batch 1: Pure Compose + Miuix Left Sidebar, Overview (folders, operations, log upload),
 * and Version Settings (per-instance toggle, icon, Java/Renderer/Driver/Memory/Controller/Args),
 * while seamlessly hosting Installer / Mods / Worlds / ResourcePacks / ShaderPacks & TempPages.
 */
@Composable
fun VersionManagePage(
    manageUI: ManageUI,
    initialSection: ManageSection,
    sectionRequestSeq: Int,
    onOpenGlobalSetup: () -> Unit,
    onOpenControllerPage: () -> Unit,
    onVersionDisplayChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors

    val activeProfile by Profiles.selectedProfile.collectAsState()
    val profile = activeProfile ?: remember { Profiles.getSelectedProfile() }
    val selectedVersionFlow by Profiles.selectedVersion.collectAsState()
    val version = selectedVersionFlow ?: profile.selectedVersion ?: manageUI.version

    var section by remember { mutableStateOf(initialSection) }
    var tempPageCount by remember { mutableIntStateOf(if (manageUI.hasTempPages()) 1 else 0) }
    var iconVersionNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(sectionRequestSeq) {
        section = initialSection
    }

    LaunchedEffect(profile, version) {
        if (!version.isNullOrBlank()) {
            manageUI.setVersion(version, profile)
        }
    }

    DisposableEffect(manageUI) {
        manageUI.setTabBarVisible(false)
        manageUI.setTempPageListener(FCLMultiPageUI.TempPageListener { count ->
            tempPageCount = count
        })
        onDispose {
            manageUI.setTempPageListener(null)
        }
    }

    val versionIconBitmap = remember(profile, version, iconVersionNonce) {
        if (version.isNullOrBlank()) null
        else runCatching {
            profile.repository.getVersionIconImage(version).toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    val gameVersionText = remember(profile, version) {
        if (version.isNullOrBlank()) ""
        else runCatching { profile.repository.getGameVersion(version).orElse("") }.getOrDefault("")
    }
    val isModpack = remember(profile, version) {
        !version.isNullOrBlank() && runCatching { profile.repository.isModpack(version) }.getOrDefault(false)
    }

    Row(modifier = modifier.fillMaxSize()) {
        // Left navigation sidebar (fixed 210.dp, matching SetupPage / AccountPage)
        Column(
            modifier = Modifier
                .width(210.dp)
                .fillMaxHeight()
                .background(colors.sidebarBg)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Left))
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            VersionHeaderBadge(
                versionName = version ?: stringResource(R.string.version_no_version),
                gameVersion = gameVersionText,
                isModpack = isModpack,
                iconBitmap = versionIconBitmap,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(6.dp))

            SidebarNavItem(
                label = stringResource(R.string.manage),
                selected = section == ManageSection.OVERVIEW,
                onClick = {
                    manageUI.dismissAllTempPages()
                    section = ManageSection.OVERVIEW
                }
            )
            SidebarNavItem(
                label = stringResource(R.string.settings_game),
                selected = section == ManageSection.SETTINGS,
                onClick = {
                    manageUI.dismissAllTempPages()
                    section = ManageSection.SETTINGS
                }
            )
            SidebarNavItem(
                label = stringResource(R.string.settings_tabs_installers),
                selected = section == ManageSection.INSTALLER,
                onClick = {
                    manageUI.dismissAllTempPages()
                    section = ManageSection.INSTALLER
                }
            )
            SidebarNavItem(
                label = stringResource(R.string.mods_manage),
                selected = section == ManageSection.MODS,
                onClick = {
                    manageUI.dismissAllTempPages()
                    section = ManageSection.MODS
                }
            )
            SidebarNavItem(
                label = stringResource(R.string.world_manage),
                selected = section == ManageSection.WORLDS,
                onClick = {
                    manageUI.dismissAllTempPages()
                    section = ManageSection.WORLDS
                }
            )
            SidebarNavItem(
                label = stringResource(R.string.resourcepack_manage),
                selected = section == ManageSection.RESOURCE_PACKS,
                onClick = {
                    manageUI.dismissAllTempPages()
                    section = ManageSection.RESOURCE_PACKS
                }
            )
            SidebarNavItem(
                label = stringResource(R.string.shaderpack_manage),
                selected = section == ManageSection.SHADER_PACKS,
                onClick = {
                    manageUI.dismissAllTempPages()
                    section = ManageSection.SHADER_PACKS
                }
            )
        }

        // Right-edge shadow divider
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

        // Right detail canvas (all 7 sections are pure Compose; legacy host shown only for active TempPages)
        if (tempPageCount > 0) {
            LaunchedEffect(profile, version) {
                manageUI.setTabBarVisible(false)
                if (!version.isNullOrBlank()) {
                    manageUI.setVersion(version, profile)
                }
            }
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { container ->
                    val view: View = manageUI.contentView
                    if (view.parent !== container) {
                        (view.parent as? ViewGroup)?.removeView(view)
                        container.removeAllViews()
                        container.addView(
                            view,
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                    manageUI.setTabBarVisible(false)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
            )
        } else if (version.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.version_no_version),
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            }
        } else when (section) {
            ManageSection.OVERVIEW, ManageSection.SETTINGS -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 680.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (section == ManageSection.OVERVIEW) {
                            ManageOverviewSection(
                                profile = profile,
                                version = version,
                                isModpack = isModpack,
                                manageUI = manageUI,
                                onNavigateToSection = { section = it }
                            )
                        } else {
                            ManageSettingsSection(
                                profile = profile,
                                version = version,
                                isModpack = isModpack,
                                manageUI = manageUI,
                                iconBitmap = versionIconBitmap,
                                onIconUpdated = {
                                    iconVersionNonce++
                                    onVersionDisplayChanged()
                                },
                                onOpenGlobalSetup = onOpenGlobalSetup,
                                onOpenControllerPage = onOpenControllerPage
                            )
                        }
                    }
                }
            }

            ManageSection.INSTALLER -> ManageInstallersSection(
                profile = profile,
                version = version,
                manageUI = manageUI,
                onVersionChanged = {
                    iconVersionNonce++
                    onVersionDisplayChanged()
                },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            ManageSection.MODS -> ManageModsSection(
                profile = profile,
                version = version,
                manageUI = manageUI,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            ManageSection.WORLDS -> ManageWorldsSection(
                profile = profile,
                version = version,
                manageUI = manageUI,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            ManageSection.RESOURCE_PACKS -> ManageResourcePacksSection(
                profile = profile,
                version = version,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            ManageSection.SHADER_PACKS -> ManageShaderPacksSection(
                profile = profile,
                version = version,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun VersionHeaderBadge(
    versionName: String,
    gameVersion: String,
    isModpack: Boolean,
    iconBitmap: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = versionName,
                    modifier = Modifier.size(30.dp)
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.img_grass),
                    contentDescription = versionName,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = versionName,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val sub = buildString {
                if (gameVersion.isNotBlank()) append(gameVersion)
                if (isModpack) {
                    if (isNotEmpty()) append(" · ")
                    append("Modpack")
                }
            }
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    color = colors.accent,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private enum class CleanConfirmTarget { LIBRARIES, LOGS }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManageOverviewSection(
    profile: Profile,
    version: String,
    isModpack: Boolean,
    manageUI: ManageUI,
    onNavigateToSection: (ManageSection) -> Unit
) {
    val context = LocalContext.current
    val activity = MainActivity.getInstance()
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()

    var confirmTarget by remember { mutableStateOf<CleanConfirmTarget?>(null) }
    var isCleaning by remember { mutableStateOf(false) }

    fun browseDirectory(subPath: String) {
        val root = when {
            subPath.startsWith("/") -> File(subPath)
            subPath.isEmpty() -> profile.repository.getRunDirectory(version)
            else -> File(profile.repository.getRunDirectory(version), subPath)
        }
        if (!root.exists()) {
            root.mkdirs()
        }
        FileBrowser.Builder(context)
            .setInitDir(root.absolutePath)
            .setLibMode(LibMode.FILE_BROWSER)
            .create()
            .browse(activity)
    }

    fun uploadLatestGameLog() {
        val logFile = FCLPath.getLatestGameLog()
        if (!logFile.exists()) {
            showErrorDialog(context, R.string.log_not_found)
            return
        }
        try {
            if (logFile.length() > 5 * 1024 * 1024) {
                showErrorDialog(context, R.string.log_too_large)
                return
            }
            val logs = FileUtils.readText(logFile)
            uploadLog(activity, logs)
        } catch (e: Exception) {
            showErrorDialog(context, "Failed to read log: ${e.message}")
        }
    }

    SectionCard(title = stringResource(R.string.manage)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LauncherButton(
                text = stringResource(R.string.version_manage_rename),
                onClick = {
                    Versions.renameVersion(context, profile, version)
                        .thenApply { newName ->
                            manageUI.preferredVersionName = newName
                        }
                },
                height = 34.dp
            )
            LauncherButton(
                text = stringResource(R.string.version_manage_duplicate),
                onClick = { Versions.duplicateVersion(context, profile, version) },
                height = 34.dp
            )
            LauncherButton(
                text = stringResource(R.string.modpack_export),
                onClick = { Versions.exportVersion(context, profile, version) },
                height = 34.dp
            )
            if (isModpack) {
                LauncherButton(
                    text = stringResource(R.string.version_update),
                    accentStyle = true,
                    onClick = { Versions.updateVersion(context, profile, version) },
                    height = 34.dp
                )
            }
            LauncherButton(
                text = stringResource(R.string.version_manage_redownload_assets_index),
                onClick = { Versions.updateGameAssets(context, profile, version) },
                height = 34.dp
            )
            LauncherButton(
                text = stringResource(R.string.version_manage_remove_libraries),
                enabled = !isCleaning,
                onClick = { confirmTarget = CleanConfirmTarget.LIBRARIES },
                height = 34.dp
            )
            LauncherButton(
                text = stringResource(R.string.version_manage_clean),
                enabled = !isCleaning,
                onClick = { confirmTarget = CleanConfirmTarget.LOGS },
                height = 34.dp
            )
            LauncherButton(
                text = stringResource(R.string.upload_log),
                onClick = { uploadLatestGameLog() },
                height = 34.dp
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LauncherButton(
                text = stringResource(R.string.settings_game),
                accentStyle = true,
                onClick = { onNavigateToSection(ManageSection.SETTINGS) },
                modifier = Modifier.weight(1f),
                height = 34.dp
            )
            LauncherButton(
                text = stringResource(R.string.mods_manage),
                onClick = { onNavigateToSection(ManageSection.MODS) },
                modifier = Modifier.weight(1f),
                height = 34.dp
            )
            LauncherButton(
                text = stringResource(R.string.world_manage),
                onClick = { onNavigateToSection(ManageSection.WORLDS) },
                modifier = Modifier.weight(1f),
                height = 34.dp
            )
        }
    }

    SectionCard(title = stringResource(R.string.folder_game)) {
        val folderEntries = remember {
            listOf(
                R.string.folder_game to "",
                R.string.folder_mod to "mods",
                R.string.folder_config to "config",
                R.string.folder_saves to "saves",
                R.string.folder_resourcepacks to "resourcepacks",
                R.string.folder_shaderpacks to "shaderpacks",
                R.string.folder_screenshots to "screenshots",
                R.string.folder_fcl_log to FCLPath.LOG_DIR
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            folderEntries.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { (labelRes, path) ->
                        LauncherButton(
                            text = stringResource(labelRes),
                            onClick = { browseDirectory(path) },
                            modifier = Modifier.weight(1f),
                            height = 34.dp
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }

    val target = confirmTarget
    WindowDialog(
        show = target != null,
        title = when (target) {
            CleanConfirmTarget.LIBRARIES -> stringResource(R.string.version_manage_remove_libraries)
            CleanConfirmTarget.LOGS -> stringResource(R.string.version_manage_clean)
            null -> ""
        },
        summary = when (target) {
            CleanConfirmTarget.LIBRARIES -> stringResource(R.string.version_manage_remove_confirm, "libraries")
            CleanConfirmTarget.LOGS -> stringResource(R.string.version_manage_remove_confirm, "logs")
            null -> ""
        },
        onDismissRequest = { confirmTarget = null }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(
                text = stringResource(R.string.dialog_negative),
                onClick = { confirmTarget = null },
                modifier = Modifier.weight(1f)
            )
            TextButton(
                text = stringResource(R.string.dialog_positive),
                onClick = {
                    val currentTarget = target
                    confirmTarget = null
                    isCleaning = true
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            when (currentTarget) {
                                CleanConfirmTarget.LIBRARIES -> FileUtils.deleteDirectoryQuietly(
                                    File(profile.repository.baseDirectory, "libraries")
                                )
                                CleanConfirmTarget.LOGS -> Versions.cleanVersion(profile, version)
                                null -> Unit
                            }
                        }
                        isCleaning = false
                        Toast.makeText(context, R.string.message_success, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ManageSettingsSection(
    profile: Profile,
    version: String,
    isModpack: Boolean,
    manageUI: ManageUI,
    iconBitmap: ImageBitmap?,
    onIconUpdated: () -> Unit,
    onOpenGlobalSetup: () -> Unit,
    onOpenControllerPage: () -> Unit
) {
    val context = LocalContext.current
    val activity = MainActivity.getInstance()
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()

    var reloadTrigger by remember { mutableIntStateOf(0) }
    val versionSetting = remember(profile, version, reloadTrigger) {
        profile.getVersionSetting(version).also { it.checkController() }
    }

    var enableSpecific by remember(versionSetting) { mutableStateOf(!versionSetting.isUsesGlobal) }
    var isolateGameDir by remember(versionSetting, isModpack) {
        mutableStateOf(isModpack || versionSetting.isIsolateGameDir)
    }
    var autoMemory by remember(versionSetting) { mutableStateOf(versionSetting.isAutoMemory) }
    var maxMemory by remember(versionSetting) { mutableIntStateOf(versionSetting.maxMemory) }
    var serverIp by remember(versionSetting) { mutableStateOf(versionSetting.serverIp) }
    var javaName by remember(versionSetting) { mutableStateOf(versionSetting.java) }
    var controllerId by remember(versionSetting) { mutableStateOf(versionSetting.controller) }
    var rendererId by remember(versionSetting) { mutableStateOf(versionSetting.renderer) }
    var driverName by remember(versionSetting) { mutableStateOf(versionSetting.driver) }
    var isSystemDriver by remember(versionSetting) { mutableStateOf(versionSetting.isVKDriverSystem) }
    var notCheckVulkan by remember(versionSetting) { mutableStateOf(versionSetting.isNotCheckVulkan) }
    var notCheckGame by remember(versionSetting) { mutableStateOf(versionSetting.isNotCheckGame) }
    var notRestoreModpack by remember(versionSetting) { mutableStateOf(versionSetting.isNotRestoreModpack) }
    var notCheckJvm by remember(versionSetting) { mutableStateOf(versionSetting.isNotCheckJVM) }
    var notCheckMod by remember(versionSetting) { mutableStateOf(versionSetting.isNotCheckMod) }
    var debugLog by remember(versionSetting) { mutableStateOf(versionSetting.isDebugLog) }
    var minecraftArgs by remember(versionSetting) { mutableStateOf(versionSetting.minecraftArgs) }
    var javaArgs by remember(versionSetting) { mutableStateOf(versionSetting.javaArgs) }

    val backends = remember { listOf("default", "opengl", "vulkan") }
    var backendIdx by remember(versionSetting) {
        mutableIntStateOf(backends.indexOf(versionSetting.graphicsBackend).coerceAtLeast(0))
    }

    val drivers = remember { DriverPlugin.driverList.toList() }
    val driverNames = remember(drivers) { drivers.map { it.driver } }
    var selectedDriverIdx by remember(versionSetting) {
        mutableIntStateOf(drivers.indexOfFirst { it.driver == driverName }.coerceAtLeast(0))
    }

    var vulkanSummary by remember {
        mutableStateOf(context.getString(R.string.settings_fcl_vulkan_check_not_checked))
    }
    LaunchedEffect(Unit) {
        val record = VulkanCheckManager.loadRecord()
        if (!record?.apiVersion.isNullOrEmpty()) {
            vulkanSummary = record.apiVersion
        }
    }

    val totalDeviceRamMb = remember { MemoryUtils.getTotalDeviceMemory(context).coerceAtLeast(2048) }
    val maxAllocatableMb = remember(totalDeviceRamMb) { (totalDeviceRamMb * 0.8f).toInt().coerceAtLeast(1024) }

    fun openPluginInstallDialog(githubUrl: String, netdiskUrl: String) {
        showItemSelectionDialog(
            context,
            context.getString(R.string.message_install_plugin),
            listOf("Github", context.getString(R.string.settings_download_netdisk))
        ) { pos, _ ->
            openLink(
                context,
                when (pos) {
                    0 -> githubUrl
                    1 -> netdiskUrl
                    else -> return@showItemSelectionDialog
                }
            )
        }
    }

    fun pickVersionIcon() {
        activity.fileLauncher.launchSingleSelection(null, listOf(".png")) { result ->
            val selected = result?.firstOrNull() ?: return@launchSingleSelection
            val selectedFile = selected.toFile(activity, File(FCLPath.CACHE_DIR))
            val iconFile = profile.repository.getVersionIconFile(version)
            try {
                FileUtils.copyFile(selectedFile, iconFile)
                profile.repository.onVersionIconChanged.fireEvent(Event(profile))
                onIconUpdated()
            } catch (e: IOException) {
                Logging.LOG.log(Level.SEVERE, "Failed to copy icon file from $selectedFile to $iconFile", e)
            }
        }
    }

    fun resetVersionIcon() {
        val iconFile = profile.repository.getVersionIconFile(version)
        if (iconFile.exists()) iconFile.delete()
        profile.repository.onVersionIconChanged.fireEvent(Event(profile))
        onIconUpdated()
    }

    SectionCard(title = stringResource(R.string.settings_game)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 8.dp,
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(color = Color.Transparent, contentColor = colors.textPrimary)
        ) {
            SwitchPreference(
                title = stringResource(R.string.settings_type_special_enable),
                summary = stringResource(R.string.settings_type_special_enable_desc),
                checked = enableSpecific,
                onCheckedChange = { checked ->
                    enableSpecific = checked
                    if (checked) {
                        profile.repository.specializeVersionSetting(version)
                    } else {
                        profile.repository.globalizeVersionSetting(version)
                    }
                    reloadTrigger++
                    manageUI.onRunDirectoryChange(profile, version)
                }
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.accent.copy(alpha = 0.08f))
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(34.dp))
                } else {
                    Image(
                        painter = painterResource(R.drawable.img_grass),
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_icon),
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.settings_icon_desc),
                    color = colors.textSecondary,
                    fontSize = 11.5.sp
                )
            }
            LauncherButton(
                text = stringResource(R.string.edit),
                onClick = { pickVersionIcon() },
                height = 32.dp
            )
            LauncherButton(
                text = stringResource(R.string.account_skin_reset),
                onClick = { resetVersionIcon() },
                height = 32.dp
            )
        }

        if (!enableSpecific) {
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.accent.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_type_special_enable_desc),
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                LauncherButton(
                    text = stringResource(R.string.settings_type_global_manage),
                    onClick = onOpenGlobalSetup,
                    height = 32.dp
                )
            }
        }
    }

    if (!enableSpecific) {
        return
    }

    SectionCard(title = stringResource(R.string.pclf_setup_launch_options)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 8.dp,
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(color = Color.Transparent, contentColor = colors.textPrimary)
        ) {
            val javaLabel = if (javaName == "Auto") {
                stringResource(R.string.settings_game_java_version_auto)
            } else javaName

            ArrowPreference(
                title = stringResource(R.string.settings_game_java_version),
                summary = javaLabel,
                onClick = {
                    JavaManageDialog(context, versionSetting.java) { picked ->
                        versionSetting.java = picked
                        javaName = picked
                    }.show()
                }
            )

            SwitchPreference(
                title = stringResource(R.string.settings_game_working_directory),
                summary = stringResource(R.string.settings_game_working_directory_desc),
                enabled = !isModpack,
                checked = isolateGameDir,
                onCheckedChange = { checked ->
                    if (!isModpack) {
                        isolateGameDir = checked
                        versionSetting.isIsolateGameDir = checked
                        manageUI.onRunDirectoryChange(profile, version)
                    }
                }
            )

            val controllerLabel = remember(controllerId) {
                if (Controllers.isInitialized()) Controllers.findControllerById(controllerId).name else "Default"
            }
            ArrowPreference(
                title = stringResource(R.string.settings_fcl_controller),
                summary = controllerLabel,
                onClick = {
                    if (Controllers.isInitialized()) {
                        SelectControllerDialog(context, versionSetting.controller) { ctrl ->
                            versionSetting.controller = ctrl.id
                            controllerId = ctrl.id
                        }.show()
                    } else {
                        Toast.makeText(context, R.string.message_data_is_loading, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LauncherButton(
                text = "Java +",
                onClick = {
                    openPluginInstallDialog(
                        "https://github.com/FCL-Team/FoldCraftLauncher/releases/tag/java",
                        "https://pan.quark.cn/s/a5f230c3da03"
                    )
                },
                height = 30.dp
            )
            LauncherButton(
                text = stringResource(R.string.settings_fcl_controller) + " +",
                onClick = onOpenControllerPage,
                height = 30.dp
            )
        }

        Spacer(Modifier.height(10.dp))
        TextField(
            value = serverIp,
            onValueChange = {
                serverIp = it
                versionSetting.serverIp = it
            },
            label = stringResource(R.string.settings_advanced_server_ip),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    SectionCard(title = stringResource(R.string.pclf_setup_memory_alloc)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ManageMemoryPill(
                label = stringResource(R.string.pclf_setup_memory_auto),
                selected = autoMemory,
                onClick = {
                    autoMemory = true
                    versionSetting.isAutoMemory = true
                    val best = MemoryUtils.findBestRAMAllocation(context)
                    maxMemory = best
                    versionSetting.maxMemory = best
                }
            )
            ManageMemoryPill(
                label = stringResource(R.string.pclf_setup_memory_custom),
                selected = !autoMemory,
                onClick = {
                    autoMemory = false
                    versionSetting.isAutoMemory = false
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        Slider(
            value = maxMemory.toFloat().coerceIn(512f, maxAllocatableMb.toFloat()),
            onValueChange = { v ->
                autoMemory = false
                versionSetting.isAutoMemory = false
                val rounded = ((v / 128f).toInt() * 128).coerceIn(512, maxAllocatableMb)
                maxMemory = rounded
                versionSetting.maxMemory = rounded
            },
            valueRange = 512f..maxAllocatableMb.toFloat(),
            hapticEffect = SliderDefaults.DefaultHapticEffect
        )
        Spacer(Modifier.height(8.dp))
        val ratio = (maxMemory.toFloat() / totalDeviceRamMb.toFloat()).coerceIn(0f, 1f)
        LinearProgressIndicator(progress = ratio, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.pclf_setup_memory_summary,
                String.format("%.1f GB", totalDeviceRamMb / 1024f),
                if (maxMemory >= 1024) String.format("%.1f GB", maxMemory / 1024f) else "$maxMemory MB"
            ),
            color = colors.textSecondary,
            fontSize = 12.sp
        )
    }

    SectionCard(title = stringResource(R.string.settings_fcl_renderer)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 8.dp,
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(color = Color.Transparent, contentColor = colors.textPrimary)
        ) {
            WindowDropdownPreference(
                items = backends,
                selectedIndex = backendIdx,
                title = stringResource(R.string.settings_fcl_graphics_backend),
                summary = stringResource(R.string.settings_fcl_graphics_backend_desc),
                onSelectedIndexChange = { idx ->
                    backendIdx = idx
                    versionSetting.graphicsBackend = backends[idx]
                }
            )
            ArrowPreference(
                title = stringResource(R.string.settings_fcl_renderer),
                summary = RendererManager.getRenderer(rendererId).des,
                onClick = {
                    val previousRenderer = versionSetting.renderer
                    RendererSelectDialog(context, false) { picked ->
                        rendererId = picked.id
                        val override = profile.getEffectiveRendererOverride(version)
                        if (override != null && override != picked.id) {
                            val builder = FCLAlertDialog.Builder(context)
                            builder.setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
                            builder.setMessage(
                                context.getString(
                                    R.string.message_warn_renderer_override_shadow,
                                    version,
                                    RendererManager.getRenderer(override).des
                                )
                            )
                            builder.setPositiveButton(context.getString(R.string.renderer_update_override)) {
                                profile.setRendererOverride(version, picked.id)
                            }
                            builder.setNeutralButton(context.getString(R.string.renderer_clear_override)) {
                                profile.setRendererOverride(version, null)
                            }
                            builder.setNegativeButton(context.getString(R.string.renderer_keep_override)) {
                                versionSetting.renderer = previousRenderer
                                rendererId = previousRenderer
                            }
                            builder.create().show()
                        }
                    }.show()
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_fcl_vulkan_driver_system),
                summary = stringResource(R.string.settings_fcl_vulkan_driver_system_desc),
                checked = isSystemDriver,
                onCheckedChange = { checked ->
                    isSystemDriver = checked
                    versionSetting.isVKDriverSystem = checked
                    if (checked && isAdrenoGPU()) {
                        FCLAlertDialog.Builder(context)
                            .setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
                            .setMessage(context.getString(R.string.message_vulkan_driver_system))
                            .setNegativeButton(context.getString(R.string.dialog_positive), null)
                            .create()
                            .show()
                    }
                }
            )
            if (!isSystemDriver && driverNames.isNotEmpty()) {
                WindowDropdownPreference(
                    items = driverNames,
                    selectedIndex = selectedDriverIdx,
                    title = stringResource(R.string.settings_fcl_driver),
                    summary = stringResource(R.string.settings_fcl_driver_desc),
                    onSelectedIndexChange = { idx ->
                        selectedDriverIdx = idx
                        val chosen = drivers[idx]
                        driverName = chosen.driver
                        versionSetting.driver = chosen.driver
                        DriverPlugin.selected = chosen
                    }
                )
            }
            ArrowPreference(
                title = stringResource(R.string.settings_fcl_vulkan_check),
                summary = vulkanSummary,
                onClick = {
                    Toast.makeText(context, R.string.vulkan_check_running, Toast.LENGTH_SHORT).show()
                    scope.launch {
                        val useCustom = !versionSetting.isVKDriverSystem
                        val pickedDriver = if (useCustom) {
                            drivers.find { it.driver == versionSetting.driver } ?: drivers.firstOrNull()
                        } else null
                        val caps = VulkanCheckManager.check(useCustom, pickedDriver)
                        vulkanSummary = caps?.versionString
                            ?: context.getString(R.string.vulkan_check_profile_unsupport)
                        val usedCustom = caps?.usedCustomDriver == true
                        VulkanCheckDialog(
                            context,
                            caps,
                            usedCustom,
                            if (usedCustom) pickedDriver?.driver else null
                        ).show()
                    }
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_fcl_vulkan_check_disable),
                summary = stringResource(R.string.settings_fcl_vulkan_check_disable_desc),
                checked = notCheckVulkan,
                onCheckedChange = {
                    notCheckVulkan = it
                    versionSetting.isNotCheckVulkan = it
                }
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LauncherButton(
                text = stringResource(R.string.settings_fcl_renderer) + " +",
                onClick = {
                    openPluginInstallDialog(
                        "https://github.com/ShirosakiMio/FCLRendererPlugin/releases/tag/Renderer",
                        "https://pan.quark.cn/s/a9f6e9d860d9"
                    )
                },
                height = 30.dp
            )
            LauncherButton(
                text = stringResource(R.string.pclf_env_driver) + " +",
                onClick = {
                    openPluginInstallDialog(
                        "https://github.com/FCL-Team/FCLDriverPlugin/releases/tag/Turnip",
                        "https://pan.quark.cn/s/d87c59695250"
                    )
                },
                height = 30.dp
            )
        }
    }

    SectionCard(title = stringResource(R.string.pclf_setup_advanced)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 8.dp,
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(color = Color.Transparent, contentColor = colors.textPrimary)
        ) {
            SwitchPreference(
                title = stringResource(R.string.settings_advanced_dont_check_game_completeness),
                summary = stringResource(R.string.settings_advanced_dont_check_game_completeness_desc),
                checked = notCheckGame,
                onCheckedChange = {
                    notCheckGame = it
                    versionSetting.isNotCheckGame = it
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_advanced_dont_restore_modpack),
                summary = stringResource(R.string.settings_advanced_dont_restore_modpack_desc),
                checked = notRestoreModpack,
                onCheckedChange = {
                    notRestoreModpack = it
                    versionSetting.isNotRestoreModpack = it
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_advanced_dont_check_jvm_validity),
                summary = stringResource(R.string.settings_advanced_dont_check_jvm_validity_desc),
                checked = notCheckJvm,
                onCheckedChange = {
                    notCheckJvm = it
                    versionSetting.isNotCheckJVM = it
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_advanced_dont_check_mod),
                summary = stringResource(R.string.settings_advanced_dont_check_mod_desc),
                checked = notCheckMod,
                onCheckedChange = {
                    notCheckMod = it
                    versionSetting.isNotCheckMod = it
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_advanced_debug_log),
                summary = stringResource(R.string.settings_advanced_debug_log_desc),
                checked = debugLog,
                onCheckedChange = {
                    debugLog = it
                    versionSetting.isDebugLog = it
                }
            )
            ArrowPreference(
                title = stringResource(R.string.settings_advanced_env),
                summary = stringResource(R.string.settings_advanced_env_desc),
                onClick = {
                    val preferences = context.getSharedPreferences("launcher", MODE_PRIVATE)
                    val dialog = FullEditDialog(context, true) { text ->
                        val parsed = parseEnvironmentVariables(text)
                        preferences.edit { putString("env", parsed.joinToString("\n")) }
                    }
                    dialog.binding.editText.setText(preferences.getString("env", ""))
                    dialog.show()
                }
            )
        }

        Spacer(Modifier.height(10.dp))
        TextField(
            value = minecraftArgs,
            onValueChange = {
                minecraftArgs = it
                versionSetting.minecraftArgs = it
            },
            label = stringResource(R.string.settings_advanced_minecraft_arguments),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        TextField(
            value = javaArgs,
            onValueChange = {
                javaArgs = it
                versionSetting.javaArgs = it
            },
            label = stringResource(R.string.settings_advanced_jvm_args),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun ManageMemoryPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LauncherTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) colors.accent.copy(alpha = 0.14f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) colors.accent else colors.cardBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) colors.accent else colors.textPrimary,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

private fun parseEnvironmentVariables(input: String): List<String> {
    val validNameRegex = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
    return input.trim().lines().mapNotNull { rawLine ->
        val line = rawLine.trim()
        val firstEq = line.indexOf('=')
        if (line.isEmpty() || firstEq == -1) return@mapNotNull null
        val name = line.substring(0, firstEq).trim()
        val value = line.substring(firstEq + 1).trim()
        if (name.isEmpty() || name.length > 255 || !validNameRegex.matches(name)) return@mapNotNull null
        "$name=$value"
    }
}
