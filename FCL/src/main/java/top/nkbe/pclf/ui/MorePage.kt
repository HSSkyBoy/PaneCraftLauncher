package top.nkbe.pclf.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mio.JavaManager
import com.mio.device.VulkanCheckManager
import com.mio.manager.RendererManager
import com.mio.plugin.DriverPlugin
import com.mio.ui.dialog.VulkanCheckDialog
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.game.JarExecutorHelper
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.util.Logging
import com.tungsten.fclcore.util.platform.MemoryUtils
import com.tungsten.fcllibrary.browser.FileBrowser
import com.tungsten.fcllibrary.browser.options.LibMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.SectionCard
import top.nkbe.pclf.ui.components.SidebarNavItem
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private enum class MoreSection {
    TOOLS,
    DIAGNOSTICS,
    HELP,
    ABOUT
}

/**
 * Rich PCL2-inspired More page:
 * - Tools & Toolbox (Controller, Account, Versions, JAR Executor, Game Dir Browser, Mic Permission)
 * - Diagnostics (Device & Runtime Overview, Logs Export/Share, Shell, Vulkan Check, Updates)
 * - Help & FAQ (Expandable practical troubleshooting guides)
 * - About & Credits (Version info, GPL-3.0 open-source & anti-resale notice, Credits)
 */
@Composable
fun MorePage(
    versionName: String,
    onOpenController: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenVersions: () -> Unit,
    onShareLatestLog: () -> Unit,
    onOpenShell: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    var section by remember { mutableStateOf(MoreSection.TOOLS) }

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
                label = stringResource(R.string.pclf_more_tools_title),
                selected = section == MoreSection.TOOLS,
                onClick = { section = MoreSection.TOOLS }
            )
            SidebarNavItem(
                label = stringResource(R.string.pclf_more_diag_title),
                selected = section == MoreSection.DIAGNOSTICS,
                onClick = { section = MoreSection.DIAGNOSTICS }
            )
            SidebarNavItem(
                label = stringResource(R.string.more_cat_help),
                selected = section == MoreSection.HELP,
                onClick = { section = MoreSection.HELP }
            )
            SidebarNavItem(
                label = stringResource(R.string.more_cat_about),
                selected = section == MoreSection.ABOUT,
                onClick = { section = MoreSection.ABOUT }
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
                    MoreSection.TOOLS -> MoreToolsSection(
                        onOpenController = onOpenController,
                        onOpenAccount = onOpenAccount,
                        onOpenVersions = onOpenVersions
                    )
                    MoreSection.DIAGNOSTICS -> MoreDiagnosticsSection(
                        onShareLatestLog = onShareLatestLog,
                        onOpenShell = onOpenShell,
                        onCheckUpdate = onCheckUpdate
                    )
                    MoreSection.HELP -> MoreHelpSection()
                    MoreSection.ABOUT -> MoreAboutSection(versionName = versionName)
                }
            }
        }
    }
}

@Composable
private fun MoreToolsSection(
    onOpenController: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenVersions: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors

    SectionCard(title = stringResource(R.string.pclf_more_tools_title)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 8.dp,
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(
                color = Color.Transparent,
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

    SectionCard(title = stringResource(R.string.more_toolbox_title)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 8.dp,
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(
                color = Color.Transparent,
                contentColor = colors.textPrimary
            )
        ) {
            ArrowPreference(
                title = stringResource(R.string.more_jar_exec),
                summary = stringResource(R.string.more_jar_exec_desc),
                onClick = {
                    val activity = MainActivity.getInstance()
                    JarExecutorHelper.start(activity)
                }
            )
            ArrowPreference(
                title = stringResource(R.string.more_browse_dir),
                summary = stringResource(R.string.more_browse_dir_desc),
                onClick = {
                    val activity = MainActivity.getInstance()
                    val profile = Profiles.getSelectedProfile()
                    val ver = profile.selectedVersion
                    val dir = if (ver != null) {
                        profile.repository.getRunDirectory(ver)
                    } else {
                        profile.repository.baseDirectory
                    }
                    if (!dir.exists()) dir.mkdirs()
                    FileBrowser.Builder(context)
                        .setInitDir(dir.absolutePath)
                        .setLibMode(LibMode.FILE_BROWSER)
                        .create()
                        .browse(activity)
                }
            )
            ArrowPreference(
                title = stringResource(R.string.settings_launcher_request_recording_permission),
                summary = stringResource(R.string.settings_launcher_request_recording_permission_desc),
                onClick = {
                    val activity = MainActivity.getInstance()
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)) {
                            activity.permissionResultLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                )
                            }
                        }
                    } else {
                        Toast.makeText(context, R.string.dialog_positive, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun MoreDiagnosticsSection(
    onShareLatestLog: () -> Unit,
    onOpenShell: () -> Unit,
    onCheckUpdate: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()

    val totalRamMb = remember { MemoryUtils.getTotalDeviceMemory(context) }
    val primaryAbi = remember { Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown" }
    val androidVersion = remember { "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})" }
    val javaCount = remember { JavaManager.javaList.size }
    val activeRenderer = remember {
        val setting = Profiles.getSelectedProfile().versionSetting
        RendererManager.getRenderer(setting.renderer).des
    }

    SectionCard(title = stringResource(R.string.more_device_info_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoRow("Android", androidVersion)
            InfoRow("CPU ABI", primaryAbi)
            InfoRow(
                stringResource(R.string.pclf_env_memory),
                String.format("%.1f GB (%d MB)", totalRamMb / 1024f, totalRamMb)
            )
            InfoRow(stringResource(R.string.pclf_env_renderer), activeRenderer)
            InfoRow("Java", "$javaCount runtimes")
        }
    }

    SectionCard(title = stringResource(R.string.pclf_more_diag_title)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 8.dp,
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(
                color = Color.Transparent,
                contentColor = colors.textPrimary
            )
        ) {
            ArrowPreference(
                title = stringResource(R.string.pclf_more_share_log),
                summary = stringResource(R.string.pclf_more_share_log_desc),
                onClick = onShareLatestLog
            )
            ArrowPreference(
                title = stringResource(R.string.settings_launcher_launcher_log_export),
                summary = stringResource(R.string.settings_launcher_debug_desc),
                onClick = {
                    scope.launch {
                        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss"))
                        val logFile = File(File(FCLPath.SHARED_COMMON_DIR).parent, "fcl-exported-logs-$stamp.log")
                        val res = withContext(Dispatchers.IO) {
                            runCatching {
                                logFile.parentFile?.mkdirs()
                                Files.write(logFile.toPath().toAbsolutePath(), Logging.getRawLogs())
                            }
                        }
                        res.onSuccess {
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_launcher_launcher_log_export_success, logFile.absolutePath),
                                Toast.LENGTH_LONG
                            ).show()
                        }.onFailure {
                            Toast.makeText(
                                context,
                                R.string.settings_launcher_launcher_log_export_failed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
            ArrowPreference(
                title = stringResource(R.string.settings_fcl_vulkan_check),
                summary = stringResource(R.string.settings_fcl_vulkan_check_desc),
                onClick = {
                    Toast.makeText(context, R.string.vulkan_check_running, Toast.LENGTH_SHORT).show()
                    scope.launch {
                        val setting = Profiles.getSelectedProfile().versionSetting
                        val useCustom = !setting.isVKDriverSystem
                        val drivers = DriverPlugin.driverList
                        val pickedDriver = if (useCustom) {
                            drivers.find { it.driver == setting.driver } ?: drivers.firstOrNull()
                        } else null
                        val caps = VulkanCheckManager.check(useCustom, pickedDriver)
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
}

@Composable
private fun MoreHelpSection() {
    val colors = LauncherTheme.colors
    val faqs = listOf(
        stringResource(R.string.more_faq_1_q) to stringResource(R.string.more_faq_1_a),
        stringResource(R.string.more_faq_2_q) to stringResource(R.string.more_faq_2_a),
        stringResource(R.string.more_faq_3_q) to stringResource(R.string.more_faq_3_a),
        stringResource(R.string.more_faq_4_q) to stringResource(R.string.more_faq_4_a)
    )
    var expandedIdx by remember { mutableIntStateOf(0) }

    SectionCard(title = stringResource(R.string.more_cat_help)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            faqs.forEachIndexed { idx, (q, a) ->
                val expanded = expandedIdx == idx
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            if (expanded) colors.accent.copy(alpha = 0.5f) else colors.cardBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { expandedIdx = if (expanded) -1 else idx }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = q,
                            color = if (expanded) colors.accent else colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (expanded) "▴" else "▾",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    AnimatedVisibility(visible = expanded) {
                        Column {
                            Spacer(Modifier.height(6.dp))
                            HorizontalDivider(color = colors.cardBorder)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = a,
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreAboutSection(versionName: String) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors

    SectionCard(title = "Pane Craft Launcher (v$versionName · ${context.packageName})") {
        Text(
            text = stringResource(R.string.pclf_about_body),
            color = colors.textSecondary,
            fontSize = 12.5.sp,
            lineHeight = 19.sp
        )
    }

    SectionCard(title = stringResource(R.string.more_about_credits_title)) {
        Text(
            text = stringResource(R.string.more_about_credits_body),
            color = colors.textSecondary,
            fontSize = 12.5.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = LauncherTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = colors.textSecondary, fontSize = 12.sp)
        Text(text = value, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
