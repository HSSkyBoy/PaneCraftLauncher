package top.nkbe.pclf.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import com.mio.JavaManager
import com.mio.manager.RendererManager
import com.mio.plugin.DriverPlugin
import com.tungsten.fcl.R
import com.tungsten.fcl.setting.ConfigHolder
import com.tungsten.fcl.setting.DownloadSource
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fcl.ui.setting.SettingUI
import com.tungsten.fclcore.util.platform.MemoryUtils
import com.tungsten.fcllibrary.component.theme.ThemeData
import com.tungsten.fcllibrary.component.theme.ThemeEngine
import com.tungsten.fcllibrary.util.LocaleUtils
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.SectionCard
import top.nkbe.pclf.ui.components.SidebarNavItem
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

private enum class SetupSection {
    LAUNCH,
    PERSONALIZATION,
    PLUGINS,
    OTHER
}

/**
 * Two-pane Settings page powered by Miuix 0.9.4:
 * - Left white flush sidebar (`172.dp`) with 3.dp vertical accent indicator bar
 * - Right soft-tinted canvas with crisp cards directly backed by FCL settings
 */
@Composable
fun SetupPage(
    legacySettingUiProvider: () -> SettingUI,
    onPickBackground: (isDark: Boolean) -> Unit,
    onShareLatestLog: () -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenFullLegacyGameSetting: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    var section by remember { mutableStateOf(SetupSection.LAUNCH) }

    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .background(colors.sidebarBg)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Left))
                .width(172.dp)
                .fillMaxHeight()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SidebarNavItem(
                label = stringResource(R.string.pclf_setup_launch),
                selected = section == SetupSection.LAUNCH,
                onClick = { section = SetupSection.LAUNCH }
            )
            SidebarNavItem(
                label = stringResource(R.string.pclf_setup_personalization),
                selected = section == SetupSection.PERSONALIZATION,
                onClick = { section = SetupSection.PERSONALIZATION }
            )
            SidebarNavItem(
                label = stringResource(R.string.pclf_setup_plugins),
                selected = section == SetupSection.PLUGINS,
                onClick = { section = SetupSection.PLUGINS }
            )
            SidebarNavItem(
                label = stringResource(R.string.pclf_setup_other),
                selected = section == SetupSection.OTHER,
                onClick = { section = SetupSection.OTHER }
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

        if (section == SetupSection.PLUGINS) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
            ) {
                val pluginPage = remember(legacySettingUiProvider) {
                    legacySettingUiProvider().createPage(2)
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
                        val view: View = pluginPage.contentView
                        if (view.parent !== container) {
                            (view.parent as? ViewGroup)?.removeView(view)
                            container.removeAllViews()
                            container.addView(
                                view,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
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
                    modifier = Modifier.widthIn(max = 660.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (section) {
                        SetupSection.LAUNCH -> SetupLaunchSection(
                            onOpenFullLegacyGameSetting = onOpenFullLegacyGameSetting
                        )
                        SetupSection.PERSONALIZATION -> SetupPersonalizationSection(
                            onPickBackground = onPickBackground
                        )
                        SetupSection.OTHER -> SetupOtherSection(
                            onShareLatestLog = onShareLatestLog,
                            onCheckUpdate = onCheckUpdate
                        )
                        SetupSection.PLUGINS -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupLaunchSection(
    onOpenFullLegacyGameSetting: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val setting = remember { Profiles.getSelectedProfile().versionSetting }

    var isolateGameDir by remember { mutableStateOf(setting.isIsolateGameDir) }
    var autoMemory by remember { mutableStateOf(setting.isAutoMemory) }
    var maxMemory by remember { mutableIntStateOf(setting.maxMemory) }
    var isSystemDriver by remember { mutableStateOf(setting.isVKDriverSystem) }
    var notCheckGame by remember { mutableStateOf(setting.isNotCheckGame) }
    var notCheckJvm by remember { mutableStateOf(setting.isNotCheckJVM) }
    var notCheckVulkan by remember { mutableStateOf(setting.isNotCheckVulkan) }
    var debugLog by remember { mutableStateOf(setting.isDebugLog) }

    val renderers = remember { RendererManager.rendererList.toList() }
    val rendererNames = remember(renderers) { renderers.map { it.des } }
    var selectedRendererIdx by remember {
        mutableIntStateOf(renderers.indexOfFirst { it.id == setting.renderer }.coerceAtLeast(0))
    }

    val drivers = remember { DriverPlugin.driverList.toList() }
    val driverNames = remember(drivers) { drivers.map { it.driver } }
    var selectedDriverIdx by remember {
        mutableIntStateOf(drivers.indexOfFirst { it.driver == setting.driver }.coerceAtLeast(0))
    }

    val javas = remember { JavaManager.javaList.toList() }
    val javaNames = remember(javas) { javas.map { it.name } }
    var selectedJavaIdx by remember {
        mutableIntStateOf(javas.indexOfFirst { it.name == setting.java }.coerceAtLeast(0))
    }

    val totalDeviceRamMb = remember { MemoryUtils.getTotalDeviceMemory(context).coerceAtLeast(2048) }
    val maxAllocatableMb = remember(totalDeviceRamMb) { (totalDeviceRamMb * 0.8f).toInt().coerceAtLeast(1024) }

    SectionCard(title = stringResource(R.string.pclf_setup_launch_options)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 8.dp,
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(
                color = Color.Transparent,
                contentColor = colors.textPrimary
            )
        ) {
            SwitchPreference(
                title = stringResource(R.string.settings_game_working_directory),
                summary = stringResource(R.string.settings_game_working_directory_desc),
                checked = isolateGameDir,
                onCheckedChange = {
                    isolateGameDir = it
                    setting.isIsolateGameDir = it
                }
            )
            if (rendererNames.isNotEmpty()) {
                WindowDropdownPreference(
                    items = rendererNames,
                    selectedIndex = selectedRendererIdx,
                    title = stringResource(R.string.settings_fcl_renderer),
                    onSelectedIndexChange = { idx ->
                        selectedRendererIdx = idx
                        setting.renderer = renderers[idx].id
                    }
                )
            }
            SwitchPreference(
                title = stringResource(R.string.settings_fcl_vulkan_driver_system),
                checked = isSystemDriver,
                onCheckedChange = {
                    isSystemDriver = it
                    setting.isVKDriverSystem = it
                }
            )
            if (!isSystemDriver && driverNames.isNotEmpty()) {
                WindowDropdownPreference(
                    items = driverNames,
                    selectedIndex = selectedDriverIdx,
                    title = stringResource(R.string.pclf_env_driver),
                    onSelectedIndexChange = { idx ->
                        selectedDriverIdx = idx
                        setting.driver = drivers[idx].driver
                    }
                )
            }
            if (javaNames.isNotEmpty()) {
                WindowDropdownPreference(
                    items = javaNames,
                    selectedIndex = selectedJavaIdx,
                    title = stringResource(R.string.settings_game_java_version),
                    onSelectedIndexChange = { idx ->
                        selectedJavaIdx = idx
                        setting.java = javas[idx].name
                    }
                )
            }
        }
    }

    SectionCard(title = stringResource(R.string.pclf_setup_memory_alloc)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MemoryModeRadioPill(
                label = stringResource(R.string.pclf_setup_memory_auto),
                selected = autoMemory,
                onClick = {
                    autoMemory = true
                    setting.isAutoMemory = true
                    val best = MemoryUtils.findBestRAMAllocation(context)
                    maxMemory = best
                    setting.maxMemory = best
                }
            )
            MemoryModeRadioPill(
                label = stringResource(R.string.pclf_setup_memory_custom),
                selected = !autoMemory,
                onClick = {
                    autoMemory = false
                    setting.isAutoMemory = false
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        Slider(
            value = maxMemory.toFloat().coerceIn(512f, maxAllocatableMb.toFloat()),
            onValueChange = { v ->
                autoMemory = false
                setting.isAutoMemory = false
                val rounded = ((v / 128f).toInt() * 128).coerceIn(512, maxAllocatableMb)
                maxMemory = rounded
                setting.maxMemory = rounded
            },
            valueRange = 512f..maxAllocatableMb.toFloat(),
            hapticEffect = SliderDefaults.DefaultHapticEffect
        )
        Spacer(Modifier.height(8.dp))
        val ratio = (maxMemory.toFloat() / totalDeviceRamMb.toFloat()).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = ratio,
            modifier = Modifier.fillMaxWidth()
        )
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

    SectionCard(title = stringResource(R.string.pclf_setup_advanced)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 8.dp,
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(
                color = Color.Transparent,
                contentColor = colors.textPrimary
            )
        ) {
            SwitchPreference(
                title = stringResource(R.string.settings_advanced_dont_check_game_completeness),
                checked = notCheckGame,
                onCheckedChange = {
                    notCheckGame = it
                    setting.isNotCheckGame = it
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_advanced_dont_check_jvm_validity),
                checked = notCheckJvm,
                onCheckedChange = {
                    notCheckJvm = it
                    setting.isNotCheckJVM = it
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_fcl_vulkan_check_disable),
                checked = notCheckVulkan,
                onCheckedChange = {
                    notCheckVulkan = it
                    setting.isNotCheckVulkan = it
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_advanced_debug_log),
                checked = debugLog,
                onCheckedChange = {
                    debugLog = it
                    setting.isDebugLog = it
                }
            )
            ArrowPreference(
                title = stringResource(R.string.pclf_setup_legacy_more),
                onClick = onOpenFullLegacyGameSetting
            )
        }
    }
}

@Composable
private fun SetupPersonalizationSection(
    onPickBackground: (isDark: Boolean) -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val prefs = remember { context.getSharedPreferences("launcher", Context.MODE_PRIVATE) }
    var themeModeIdx by remember { mutableIntStateOf(prefs.getInt("themeMode", 0)) }
    val themeModeItems = listOf(
        stringResource(R.string.settings_launcher_theme_mode_follow),
        stringResource(R.string.settings_launcher_theme_mode_light),
        stringResource(R.string.settings_launcher_theme_mode_dark)
    )
    var fullscreen by remember { mutableStateOf(ThemeEngine.getInstance().getTheme().fullscreen) }
    var animSpeed by remember { mutableIntStateOf(ThemeEngine.getInstance().getTheme().animationSpeed) }

    val presetColors = listOf(
        0xFF1576D8.toInt(),
        0xFFE86328.toInt(),
        0xFF2E9E4B.toInt(),
        0xFF8B3DFF.toInt(),
        0xFFE04F80.toInt()
    )

    SectionCard(title = stringResource(R.string.pclf_setup_theme_colors)) {
        WindowDropdownPreference(
            items = themeModeItems,
            selectedIndex = themeModeIdx,
            title = stringResource(R.string.settings_launcher_theme_mode),
            summary = stringResource(R.string.settings_launcher_theme_mode_desc),
            onSelectedIndexChange = { pos ->
                if (pos == themeModeIdx) return@WindowDropdownPreference
                themeModeIdx = pos
                prefs.edit { putInt("themeMode", pos) }
                val mode = when (pos) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
                ThemeEngine.getInstance().refreshTheme()
            }
        )
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_launcher_theme),
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            presetColors.forEach { argb ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        .clickable {
                            ThemeEngine.getInstance().applyAndSave(context, argb)
                            ThemeEngine.getInstance().applyAndSaveDark(context, argb)
                        }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        SwitchPreference(
            title = stringResource(R.string.settings_launcher_ignore_notch),
            summary = stringResource(R.string.settings_launcher_ignore_notch_desc),
            checked = fullscreen,
            onCheckedChange = { checked ->
                fullscreen = checked
                val window = (context as? AppCompatActivity)?.window
                window?.let { ThemeEngine.getInstance().applyAndSave(context, it, checked) }
                window?.setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                )
            }
        )
    }

    SectionCard(title = stringResource(R.string.pclf_setup_bg_anim)) {
        ArrowPreference(
            title = stringResource(R.string.pclf_setup_pick_bg_lt),
            onClick = { onPickBackground(false) }
        )
        ArrowPreference(
            title = stringResource(R.string.pclf_setup_pick_bg_dk),
            onClick = { onPickBackground(true) }
        )
        ArrowPreference(
            title = stringResource(R.string.pclf_setup_reset_bg),
            onClick = {
                ThemeEngine.getInstance().applyAndSave(context, null, null, null)
                Toast.makeText(context, R.string.pclf_setup_reset_bg, Toast.LENGTH_SHORT).show()
            }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${stringResource(R.string.settings_launcher_animation_speed)}: $animSpeed",
            color = colors.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Slider(
            value = animSpeed.toFloat().coerceIn(0f, 10f),
            onValueChange = { v ->
                val speed = v.toInt().coerceIn(0, 10)
                animSpeed = speed
                ThemeEngine.getInstance().setAnimationSpeed(speed)
                ThemeData.saveTheme(context, ThemeEngine.getInstance().getTheme())
            },
            valueRange = 0f..10f,
            steps = 9,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun SetupOtherSection(
    onShareLatestLog: () -> Unit,
    onCheckUpdate: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val prefs = remember { context.getSharedPreferences("launcher", Context.MODE_PRIVATE) }
    val config = remember { ConfigHolder.config() }

    val langNames = listOf(
        stringResource(R.string.settings_launcher_language_system),
        stringResource(R.string.settings_launcher_language_english),
        stringResource(R.string.settings_launcher_language_simplified_chinese),
        stringResource(R.string.settings_launcher_language_traditional_chinese_tw),
        stringResource(R.string.settings_launcher_language_traditional_chinese_hk),
        stringResource(R.string.settings_launcher_language_vietnamese),
        stringResource(R.string.settings_launcher_language_russian),
        stringResource(R.string.settings_launcher_language_ukrainian),
        stringResource(R.string.settings_launcher_language_german),
        stringResource(R.string.settings_launcher_language_brazilian_portuguese),
        stringResource(R.string.settings_launcher_language_persian)
    )
    val selectedLangIdx = remember { LocaleUtils.getLanguageSpinnerIndex(context) }

    val sourceNames = listOf(
        stringResource(R.string.download_source_default),
        stringResource(R.string.download_source_official),
        stringResource(R.string.download_source_mirror)
    )
    var versionSourceIdx by remember {
        mutableIntStateOf(sourceNameToIdx(config.versionListSource))
    }
    var fileSourceIdx by remember {
        mutableIntStateOf(sourceNameToIdx(config.fileDownloadSource))
    }
    var autoThreads by remember { mutableStateOf(config.autoDownloadThreads) }
    var downloadThreads by remember { mutableIntStateOf(config.downloadThreads) }
    var autoExit by remember { mutableStateOf(prefs.getBoolean("autoExitLauncher", false)) }

    SectionCard(title = stringResource(R.string.pclf_setup_lang_download)) {
        WindowDropdownPreference(
            items = langNames,
            selectedIndex = selectedLangIdx,
            title = stringResource(R.string.settings_launcher_language),
            summary = stringResource(R.string.settings_launcher_language_desc),
            onSelectedIndexChange = { pos ->
                val targetId = LocaleUtils.SUPPORTED_LANG_IDS[pos]
                if (targetId != LocaleUtils.getLanguage(context)) {
                    LocaleUtils.changeLanguage(context, targetId)
                    (context as? AppCompatActivity)?.recreate()
                }
            }
        )
        WindowDropdownPreference(
            items = sourceNames,
            selectedIndex = versionSourceIdx,
            title = stringResource(R.string.settings_launcher_download_source_version_list),
            onSelectedIndexChange = { idx ->
                versionSourceIdx = idx
                config.versionListSource = idxToSourceName(idx)
            }
        )
        WindowDropdownPreference(
            items = sourceNames,
            selectedIndex = fileSourceIdx,
            title = stringResource(R.string.settings_launcher_download_source_file),
            onSelectedIndexChange = { idx ->
                fileSourceIdx = idx
                config.fileDownloadSource = idxToSourceName(idx)
            }
        )
        SwitchPreference(
            title = stringResource(R.string.settings_launcher_download_threads_auto),
            checked = autoThreads,
            onCheckedChange = {
                autoThreads = it
                config.autoDownloadThreads = it
            }
        )
        if (!autoThreads) {
            Text(
                text = "${stringResource(R.string.settings_launcher_download_threads)}: $downloadThreads",
                color = colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Slider(
                value = downloadThreads.toFloat().coerceIn(1f, 64f),
                onValueChange = { v ->
                    val threads = v.toInt().coerceIn(1, 64)
                    downloadThreads = threads
                    config.downloadThreads = threads
                },
                valueRange = 1f..64f,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }

    SectionCard(title = stringResource(R.string.pclf_setup_behavior_logs)) {
        SwitchPreference(
            title = stringResource(R.string.settings_launcher_exit_after_launching),
            summary = stringResource(R.string.settings_launcher_exit_after_launching_desc),
            checked = autoExit,
            onCheckedChange = {
                autoExit = it
                prefs.edit { putBoolean("autoExitLauncher", it) }
            }
        )
        ArrowPreference(
            title = stringResource(R.string.pclf_more_share_log),
            summary = stringResource(R.string.pclf_more_share_log_desc),
            onClick = onShareLatestLog
        )
        ArrowPreference(
            title = stringResource(R.string.pclf_more_update),
            summary = stringResource(R.string.pclf_more_update_desc),
            onClick = onCheckUpdate
        )
    }
}

private fun sourceNameToIdx(name: String?): Int = when (name) {
    DownloadSource.OFFICIAL.name -> 1
    DownloadSource.MIRROR.name -> 2
    else -> 0
}

private fun idxToSourceName(idx: Int): String = when (idx) {
    1 -> DownloadSource.OFFICIAL.name
    2 -> DownloadSource.MIRROR.name
    else -> DownloadSource.DEFAULT.name
}

@Composable
private fun MemoryModeRadioPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LauncherTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.accent.copy(alpha = 0.14f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) colors.accent else colors.buttonBorderNeutral.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (selected) colors.accent else Color.Transparent)
                .border(1.5.dp, if (selected) colors.accent else colors.textSecondary, CircleShape)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            color = if (selected) colors.accent else colors.textPrimary,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
