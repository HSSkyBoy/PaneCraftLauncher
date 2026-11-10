package top.nkbe.pclf.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.mio.JavaManager
import com.mio.device.VulkanCheckManager
import com.mio.manager.RendererManager
import com.mio.plugin.DriverPlugin
import com.mio.plugin.MioLibPatcherManager
import com.mio.plugin.PluginManager
import com.mio.plugin.RendererPlugin
import com.mio.ui.dialog.RendererEnvDialog
import com.mio.ui.dialog.VulkanCheckDialog
import com.tungsten.fcl.R
import com.tungsten.fcl.setting.ConfigHolder
import com.tungsten.fcl.setting.DownloadSource
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fclcore.mod.RemoteModCache
import com.tungsten.fclcore.util.platform.MemoryUtils
import com.tungsten.fcllibrary.component.theme.ThemeData
import com.tungsten.fcllibrary.component.theme.ThemeEngine
import com.tungsten.fcllibrary.util.LocaleUtils
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
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

enum class SetupSection {
    LAUNCH,
    PERSONALIZATION,
    LAUNCHER,
    PLUGINS
}

/**
 * Pure Compose + Miuix two-pane Settings page following PCL2's category structure:
 * - Launch: version isolation, renderer/driver/Vulkan check, Java, Memory Allocation, JVM/MC args, checks
 * - Personalize: theme mode, backgrounds, fullscreen/skin preview/custom launcher name (theme palette reserved for future rewrite)
 * - Launcher: downloads, mod cache, TouchController, UI language, behavior & logs
 * - Plugins: MioLibPatcher toggles & installed renderer/driver plugins
 */
@Composable
fun SetupPage(
    onPickBackground: (isDark: Boolean) -> Unit,
    onShareLatestLog: () -> Unit,
    onCheckUpdate: () -> Unit,
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
                label = stringResource(R.string.setup_cat_launch),
                selected = section == SetupSection.LAUNCH,
                onClick = { section = SetupSection.LAUNCH }
            )
            SidebarNavItem(
                label = stringResource(R.string.setup_cat_personal),
                selected = section == SetupSection.PERSONALIZATION,
                onClick = { section = SetupSection.PERSONALIZATION }
            )
            SidebarNavItem(
                label = stringResource(R.string.setup_cat_launcher),
                selected = section == SetupSection.LAUNCHER,
                onClick = { section = SetupSection.LAUNCHER }
            )
            SidebarNavItem(
                label = stringResource(R.string.setup_cat_plugins),
                selected = section == SetupSection.PLUGINS,
                onClick = { section = SetupSection.PLUGINS }
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.widthIn(max = 660.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (section) {
                    SetupSection.LAUNCH -> SetupLaunchSection()
                    SetupSection.PERSONALIZATION -> SetupPersonalizationSection(
                        onPickBackground = onPickBackground
                    )
                    SetupSection.LAUNCHER -> SetupLauncherSection(
                        onShareLatestLog = onShareLatestLog,
                        onCheckUpdate = onCheckUpdate
                    )
                    SetupSection.PLUGINS -> SetupPluginsSection()
                }
            }
        }
    }
}

@Composable
private fun SetupLaunchSection() {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()
    val setting = remember { Profiles.getSelectedProfile().versionSetting }

    var isolateGameDir by remember { mutableStateOf(setting.isIsolateGameDir) }
    var autoMemory by remember { mutableStateOf(setting.isAutoMemory) }
    var maxMemory by remember { mutableIntStateOf(setting.maxMemory) }
    var isSystemDriver by remember { mutableStateOf(setting.isVKDriverSystem) }
    var notCheckGame by remember { mutableStateOf(setting.isNotCheckGame) }
    var notCheckJvm by remember { mutableStateOf(setting.isNotCheckJVM) }
    var notCheckVulkan by remember { mutableStateOf(setting.isNotCheckVulkan) }
    var debugLog by remember { mutableStateOf(setting.isDebugLog) }
    var javaArgs by remember { mutableStateOf(setting.javaArgs) }
    var minecraftArgs by remember { mutableStateOf(setting.minecraftArgs) }

    val backends = listOf("default", "opengl", "vulkan")
    var backendIdx by remember {
        mutableIntStateOf(backends.indexOf(setting.graphicsBackend).coerceAtLeast(0))
    }

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

    var vulkanSummary by remember { mutableStateOf(context.getString(R.string.settings_fcl_vulkan_check_not_checked)) }
    LaunchedEffect(Unit) {
        val record = VulkanCheckManager.loadRecord()
        if (!record?.apiVersion.isNullOrEmpty()) {
            vulkanSummary = record.apiVersion
        }
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
            WindowDropdownPreference(
                items = backends,
                selectedIndex = backendIdx,
                title = stringResource(R.string.settings_fcl_graphics_backend),
                summary = stringResource(R.string.settings_fcl_graphics_backend_desc),
                onSelectedIndexChange = { idx ->
                    backendIdx = idx
                    setting.graphicsBackend = backends[idx]
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_fcl_vulkan_driver_system),
                summary = stringResource(R.string.settings_fcl_vulkan_driver_system_desc),
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
            ArrowPreference(
                title = stringResource(R.string.settings_fcl_vulkan_check),
                summary = vulkanSummary,
                onClick = {
                    Toast.makeText(context, R.string.vulkan_check_running, Toast.LENGTH_SHORT).show()
                    scope.launch {
                        val useCustom = !setting.isVKDriverSystem
                        val pickedDriver = if (useCustom) {
                            drivers.find { it.driver == setting.driver } ?: drivers.firstOrNull()
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
        TextField(
            value = javaArgs,
            onValueChange = {
                javaArgs = it
                setting.javaArgs = it
            },
            label = stringResource(R.string.settings_advanced_jvm_args),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        TextField(
            value = minecraftArgs,
            onValueChange = {
                minecraftArgs = it
                setting.minecraftArgs = it
            },
            label = stringResource(R.string.settings_advanced_minecraft_arguments),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
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
                summary = stringResource(R.string.settings_advanced_dont_check_game_completeness_desc),
                checked = notCheckGame,
                onCheckedChange = {
                    notCheckGame = it
                    setting.isNotCheckGame = it
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_advanced_dont_check_jvm_validity),
                summary = stringResource(R.string.settings_advanced_dont_check_jvm_validity_desc),
                checked = notCheckJvm,
                onCheckedChange = {
                    notCheckJvm = it
                    setting.isNotCheckJVM = it
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_fcl_vulkan_check_disable),
                summary = stringResource(R.string.settings_fcl_vulkan_check_disable_desc),
                checked = notCheckVulkan,
                onCheckedChange = {
                    notCheckVulkan = it
                    setting.isNotCheckVulkan = it
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_advanced_debug_log),
                summary = stringResource(R.string.settings_advanced_debug_log_desc),
                checked = debugLog,
                onCheckedChange = {
                    debugLog = it
                    setting.isDebugLog = it
                }
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
    var closeSkinModel by remember { mutableStateOf(ThemeEngine.getInstance().getTheme().closeSkinModel) }
    var animSpeed by remember { mutableIntStateOf(ThemeEngine.getInstance().getTheme().animationSpeed) }
    var customLauncherName by remember {
        mutableStateOf(prefs.getString("custom_launcher_name", context.getString(R.string.app_name)) ?: "")
    }

    // Reserve theme palette section for future authentic PCL theme customization
    SectionCard(title = stringResource(R.string.pclf_setup_theme_colors)) {
        Text(
            text = stringResource(R.string.setup_theme_future_note),
            color = colors.textSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 8.dp,
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(
                color = Color.Transparent,
                contentColor = colors.textPrimary
            )
        ) {
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
            SwitchPreference(
                title = stringResource(R.string.settings_launcher_close_skin_view),
                summary = stringResource(R.string.settings_launcher_close_skin_view_desc),
                checked = closeSkinModel,
                onCheckedChange = { checked ->
                    closeSkinModel = checked
                    ThemeEngine.getInstance().setCloseSkinModel(checked)
                    ThemeData.saveTheme(context, ThemeEngine.getInstance().getTheme())
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.settings_launcher_custom_launcher_name_desc),
            color = colors.textSecondary,
            fontSize = 11.5.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(6.dp))
        TextField(
            value = customLauncherName,
            onValueChange = {
                customLauncherName = it
                prefs.edit { putString("custom_launcher_name", it) }
            },
            label = "${stringResource(R.string.settings_launcher_custom_launcher_name)} (e.g. Pane Craft Launcher/\${launcher_version})",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
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
private fun SetupLauncherSection(
    onShareLatestLog: () -> Unit,
    onCheckUpdate: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()
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
    var versionSourceIdx by remember { mutableIntStateOf(sourceNameToIdx(config.versionListSource)) }
    var fileSourceIdx by remember { mutableIntStateOf(sourceNameToIdx(config.fileDownloadSource)) }
    var autoThreads by remember { mutableStateOf(config.autoDownloadThreads) }
    var downloadThreads by remember { mutableIntStateOf(config.downloadThreads) }

    var vibrationMs by remember { mutableIntStateOf(prefs.getInt("vibrationDuration", 100)) }
    var disableFullscreenInput by remember {
        mutableStateOf(prefs.getBoolean("disableFullscreenInput", true))
    }
    var autoExit by remember { mutableStateOf(prefs.getBoolean("autoExitLauncher", false)) }
    var allowScreenshots by remember { mutableStateOf(prefs.getBoolean("allowScreenshots", false)) }

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
        ArrowPreference(
            title = stringResource(R.string.settings_launcher_mod_cache),
            summary = stringResource(R.string.settings_launcher_mod_cache_desc),
            onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) { RemoteModCache.clear() }
                    Toast.makeText(context, R.string.settings_launcher_mod_cache_cleared, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    SectionCard(title = stringResource(R.string.setup_section_touch)) {
        Text(
            text = "${stringResource(R.string.settings_launcher_vibrate_duration)}: $vibrationMs ms",
            color = colors.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Slider(
            value = vibrationMs.toFloat().coerceIn(20f, 500f),
            onValueChange = { v ->
                val ms = v.toInt().coerceIn(20, 500)
                vibrationMs = ms
                prefs.edit { putInt("vibrationDuration", ms) }
            },
            valueRange = 20f..500f,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        SwitchPreference(
            title = stringResource(R.string.settings_disable_fullscreen_input),
            summary = stringResource(R.string.settings_disable_fullscreen_input_desc),
            checked = disableFullscreenInput,
            onCheckedChange = {
                disableFullscreenInput = it
                prefs.edit { putBoolean("disableFullscreenInput", it) }
            }
        )
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
        SwitchPreference(
            title = stringResource(R.string.settings_launcher_allow_screenshot),
            summary = stringResource(R.string.settings_launcher_allow_screenshot_desc),
            checked = allowScreenshots,
            onCheckedChange = {
                allowScreenshots = it
                prefs.edit { putBoolean("allowScreenshots", it) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupPluginsSection() {
    val context = LocalContext.current
    val colors = LauncherTheme.colors

    var patcherEnabled by remember { mutableStateOf(MioLibPatcherManager.isEnabled()) }
    var alc10Enabled by remember { mutableStateOf(MioLibPatcherManager.isAlc10()) }
    var sableEnabled by remember { mutableStateOf(MioLibPatcherManager.isSablerapier()) }
    var asmEnabled by remember { mutableStateOf(MioLibPatcherManager.isAsmBackport()) }

    var pluginApps by remember { mutableStateOf(PluginManager.allApps(context)) }
    var refreshNonce by remember { mutableIntStateOf(0) }

    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val before = pluginApps
                    PluginManager.invalidate()
                    val after = PluginManager.allApps(context)
                    if (before.map { it.packageName to it.lastUpdateTime } != after.map { it.packageName to it.lastUpdateTime }) {
                        PluginManager.refreshAll(context)
                    }
                    pluginApps = after
                    refreshNonce++
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    SectionCard(title = stringResource(R.string.plugin_miolibpatcher_name)) {
        Text(
            text = stringResource(R.string.plugin_miolibpatcher_desc),
            color = colors.textSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(6.dp))
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
                title = stringResource(R.string.plugin_enable),
                checked = patcherEnabled,
                onCheckedChange = {
                    patcherEnabled = it
                    MioLibPatcherManager.setEnabled(it)
                }
            )
            if (patcherEnabled) {
                SwitchPreference(
                    title = stringResource(R.string.plugin_miolibpatcher_alc10),
                    summary = stringResource(R.string.plugin_miolibpatcher_alc10_desc),
                    checked = alc10Enabled,
                    onCheckedChange = {
                        alc10Enabled = it
                        MioLibPatcherManager.setAlc10(it)
                    }
                )
                SwitchPreference(
                    title = stringResource(R.string.plugin_miolibpatcher_sable),
                    summary = stringResource(R.string.plugin_miolibpatcher_sable_desc),
                    checked = sableEnabled,
                    onCheckedChange = {
                        sableEnabled = it
                        MioLibPatcherManager.setSablerapier(it)
                    }
                )
                SwitchPreference(
                    title = stringResource(R.string.plugin_miolibpatcher_asm),
                    summary = stringResource(R.string.plugin_miolibpatcher_asm_desc),
                    checked = asmEnabled,
                    onCheckedChange = {
                        asmEnabled = it
                        MioLibPatcherManager.setAsmBackport(it)
                    }
                )
            }
        }
    }

    SectionCard(title = stringResource(R.string.setup_section_installed_plugins)) {
        if (pluginApps.isEmpty()) {
            Text(
                text = stringResource(R.string.plugin_empty),
                color = colors.textSecondary,
                fontSize = 12.5.sp
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                pluginApps.forEach { app ->
                    val enabled = remember(app.packageName, refreshNonce) {
                        PluginManager.isEnabled(context, app.packageName)
                    }
                    val configurableSpecs = remember(app.packageName, refreshNonce) {
                        RendererPlugin.getConfigurableEnvs(app.packageName)
                    }
                    val iconBmp = remember(app.packageName) {
                        runCatching { app.icon?.toBitmap()?.asImageBitmap() }.getOrNull()
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (iconBmp != null) {
                                Image(
                                    bitmap = iconBmp,
                                    contentDescription = app.label,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (!app.versionName.isNullOrBlank()) {
                                        "${app.label} (${app.versionName})"
                                    } else {
                                        app.label
                                    },
                                    color = colors.textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    color = colors.textSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = { checked ->
                                    PluginManager.setEnabled(context, app.packageName, checked)
                                    PluginManager.refreshAll(context)
                                    refreshNonce++
                                }
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            app.types.forEach { t ->
                                val tagLabel = when (t) {
                                    PluginManager.PluginType.RENDERER -> stringResource(R.string.plugin_type_renderer)
                                    PluginManager.PluginType.DRIVER -> stringResource(R.string.plugin_type_driver)
                                    PluginManager.PluginType.NATIVE_LIB -> stringResource(R.string.plugin_type_native)
                                    PluginManager.PluginType.FFMPEG -> stringResource(R.string.setup_plugin_ffmpeg)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.accent.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tagLabel,
                                        color = colors.accent,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (configurableSpecs.isNotEmpty()) {
                                LauncherButton(
                                    text = stringResource(R.string.plugin_env_config),
                                    height = 30.dp,
                                    onClick = {
                                        RendererEnvDialog(context, app.label, configurableSpecs) { values ->
                                            RendererPlugin.updateEnvConfigs(context, app.packageName, values)
                                        }.show()
                                    }
                                )
                            }
                            LauncherButton(
                                text = stringResource(R.string.plugin_uninstall),
                                height = 30.dp,
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
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
