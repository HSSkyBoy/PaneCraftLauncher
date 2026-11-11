package top.nkbe.pclf.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialog
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import androidx.core.graphics.drawable.toBitmap
import com.mio.data.ModsChanged
import com.mio.datastore.ModSortField
import com.mio.datastore.ModSortManager
import com.mio.minecraft.ResourcePack
import com.mio.minecraft.ShaderPack
import com.mio.minecraft.listResourcePacks
import com.mio.minecraft.listShaderPacks
import com.mio.minecraft.readEnabledResourcePacks
import com.mio.minecraft.setResourcePackEnabled
import com.mio.util.getLocalizedText
import com.mio.util.openLink
import com.mio.util.showErrorDialog
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.addon.AddonProjectInfoRegistry
import com.tungsten.fcl.setting.DownloadProviders
import com.tungsten.fcl.setting.Profile
import com.tungsten.fcl.ui.InstallerItem
import com.tungsten.fcl.ui.TaskDialog
import com.tungsten.fcl.ui.download.DownloadUI
import com.tungsten.fcl.ui.download.version.InstallerListPage
import com.tungsten.fcl.ui.download.version.VersionInstallInfoPage
import com.tungsten.fcl.ui.manage.ManageUI
import com.tungsten.fcl.ui.manage.ModCheckUpdatesTask
import com.tungsten.fcl.util.TaskCancellationAction
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.download.ComponentRemoteVersion
import com.tungsten.fclcore.download.LibraryAnalyzer
import com.tungsten.fclcore.event.Event
import com.tungsten.fclcore.game.World
import com.tungsten.fclcore.game.WorldLockedException
import com.tungsten.fclcore.mod.LocalModFile
import com.tungsten.fclcore.mod.ModLoaderType
import com.tungsten.fclcore.mod.ModManager
import com.tungsten.fclcore.task.Schedulers
import com.tungsten.fclcore.task.Task
import com.tungsten.fclcore.task.TaskListener
import com.tungsten.fclcore.task.TaskExecutor
import com.tungsten.fclcore.util.Logging
import com.tungsten.fclcore.util.StringUtils
import com.tungsten.fclcore.util.io.CompressingUtils
import com.tungsten.fclcore.util.io.FileUtils
import com.tungsten.fclcore.util.versioning.GameVersionNumber
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog
import com.tungsten.fcllibrary.component.ui.FCLPage
import com.tungsten.fcllibrary.util.LocaleUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LauncherButton
import top.nkbe.pclf.ui.components.SectionCard
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.window.WindowDialog
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.text.Collator
import java.time.Instant
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.logging.Level
import java.util.regex.Pattern
import kotlin.io.path.pathString

// ==================== 1. Installer Management Section ====================

private data class InstallerRowState(
    val libraryId: String,
    val displayName: String,
    val iconBitmap: ImageBitmap?,
    val installedVersion: String?,
    val configurable: Boolean,
    val removable: Boolean
)

@Composable
fun ManageInstallersSection(
    profile: Profile,
    version: String,
    manageUI: ManageUI,
    onVersionChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = MainActivity.getInstance()
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()

    var reloadNonce by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var gameVersion by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<InstallerRowState>>(emptyList()) }
    var confirmSwitch by remember { mutableStateOf<Pair<InstallerRowState, ComponentRemoteVersion>?>(null) }
    var confirmRemove by remember { mutableStateOf<InstallerRowState?>(null) }

    LaunchedEffect(profile, version, reloadNonce) {
        isLoading = true
        val (gv, analyzed) = withContext(Dispatchers.IO) {
            val repo = profile.repository
            val v = repo.getVersion(version)
            val resolvedGameVersion = repo.getGameVersion(v).orElse(null)
            val analyzer = LibraryAnalyzer.analyze(
                repo.getResolvedPreservingPatchesVersion(version),
                resolvedGameVersion
            )
            val group = InstallerItem.InstallerItemGroup(context, resolvedGameVersion)
            val list = group.libraries.mapNotNull { item ->
                val id = item.libraryId
                if (id.contains("fabric-api") || id.contains("quilt-api")) return@mapNotNull null
                val installed = analyzer.getVersion(id).orElse(null)
                val configurable = installed != null &&
                    analyzer.getLibraryStatus(id) == LibraryAnalyzer.LibraryMark.LibraryStatus.CLEAR
                val removable = LibraryAnalyzer.LibraryType.MINECRAFT.patchId != id && configurable
                val bmp = runCatching { item.icon.toBitmap(64, 64).asImageBitmap() }.getOrNull()
                InstallerRowState(
                    libraryId = id,
                    displayName = item.name,
                    iconBitmap = bmp,
                    installedVersion = installed,
                    configurable = configurable,
                    removable = removable
                )
            }
            resolvedGameVersion to list
        }
        gameVersion = gv
        items = analyzed
        isLoading = false
    }

    fun installRemoteComponent(remoteVersion: ComponentRemoteVersion) {
        val currentVersion = profile.repository.getVersion(version)
        val task = Task.runAsync {
            val updated = profile.getDependency(DownloadProviders.getDownloadProvider())
                .installLibraryAsync(currentVersion, remoteVersion)
                .run()
            profile.repository.saveAsync(updated).run()
            profile.repository.refreshVersionsAsync().run()
        }.withStagesHint(listOf("fcl.install.${remoteVersion.componentType.patchId}:${remoteVersion.selfVersion}"))

        val dialog = TaskDialog(context, TaskCancellationAction(AppCompatDialog::dismiss))
        dialog.setTitle(context.getString(R.string.install_change_version))
        val executor = task.executor(object : TaskListener() {
            override fun onStop(success: Boolean, executor: TaskExecutor) {
                Schedulers.androidUIThread().execute {
                    if (success) {
                        FCLAlertDialog.Builder(context)
                            .setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
                            .setCancelable(false)
                            .setMessage(context.getString(R.string.install_success))
                            .setNegativeButton(context.getString(R.string.dialog_positive)) {
                                manageUI.dismissCurrentTempPage()
                                profile.repository.onVersionIconChanged.fireEvent(Event(profile))
                                onVersionChanged()
                            }
                            .create()
                            .show()
                    } else if (executor.exception != null) {
                        VersionInstallInfoPage.alertFailureMessage(context, executor.exception) {}
                    }
                    reloadNonce++
                }
            }
        })
        dialog.setExecutor(executor)
        dialog.show()
        executor.start()
    }

    fun installOfflineFile() {
        activity.fileLauncher.launchSingleSelection(null, listOf(".jar")) { files ->
            val picked = files?.firstOrNull() ?: return@launchSingleSelection
            val file = picked.toFile(activity, File(FCLPath.CACHE_DIR))
            if (!file.exists()) return@launchSingleSelection
            val currentVersion = profile.repository.getVersion(version)
            val task = Task.runAsync {
                val updated = profile.getDependency().installLibraryAsync(currentVersion, file.toPath()).run()
                profile.repository.saveAsync(updated).run()
                profile.repository.refreshVersionsAsync().run()
            }
            task.setName(context.getString(R.string.install_installer_install_offline))
            val dialog = TaskDialog(context, TaskCancellationAction.NORMAL)
            dialog.setTitle(context.getString(R.string.install_installer_install_offline))
            val executor = task.executor(object : TaskListener() {
                override fun onStop(success: Boolean, executor: TaskExecutor) {
                    Schedulers.androidUIThread().execute {
                        if (success) {
                            FCLAlertDialog.Builder(context)
                                .setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
                                .setCancelable(false)
                                .setMessage(context.getString(R.string.install_success))
                                .setNegativeButton(context.getString(R.string.dialog_positive)) {
                                    profile.repository.onVersionIconChanged.fireEvent(Event(profile))
                                    onVersionChanged()
                                }
                                .create()
                                .show()
                        } else if (executor.exception != null) {
                            VersionInstallInfoPage.alertFailureMessage(context, executor.exception) {}
                        }
                        reloadNonce++
                    }
                }
            })
            dialog.setExecutor(executor)
            dialog.show()
            executor.start()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(
            title = stringResource(R.string.settings_tabs_installers),
            trailingAction = {
                LauncherButton(
                    text = stringResource(R.string.install_installer_install_offline),
                    onClick = { installOfflineFile() },
                    height = 30.dp
                )
            }
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { row ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp)),
                        cornerRadius = 8.dp,
                        insideMargin = PaddingValues(12.dp),
                        colors = CardDefaults.defaultColors(color = colors.cardBg, contentColor = colors.textPrimary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (row.iconBitmap != null) {
                                Image(
                                    bitmap = row.iconBitmap,
                                    contentDescription = row.displayName,
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.img_grass),
                                    contentDescription = row.displayName,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.displayName,
                                    color = colors.textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = row.installedVersion ?: stringResource(R.string.install_installer_not_installed),
                                    color = if (row.installedVersion != null) colors.accent else colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            if (row.removable) {
                                LauncherButton(
                                    text = stringResource(R.string.button_remove),
                                    onClick = { confirmRemove = row },
                                    height = 30.dp
                                )
                            }
                            LauncherButton(
                                text = if (row.installedVersion == null) stringResource(R.string.pclf_install) else stringResource(R.string.install_change_version),
                                accentStyle = row.installedVersion != null,
                                onClick = {
                                    val gv = gameVersion ?: return@LauncherButton
                                    val page = InstallerListPage(context, FCLPage.PAGE_ID_TEMP, gv, row.libraryId) { remote ->
                                        if (row.installedVersion == null) {
                                            installRemoteComponent(remote)
                                        } else {
                                            confirmSwitch = row to remote
                                        }
                                    }
                                    manageUI.showTempPage(page)
                                },
                                height = 30.dp
                            )
                        }
                    }
                }
            }
        }
    }

    val pendingSwitch = confirmSwitch
    WindowDialog(
        show = pendingSwitch != null,
        title = stringResource(R.string.install_change_version),
        summary = pendingSwitch?.let { (row, remote) ->
            stringResource(
                R.string.install_change_version_confirm,
                row.displayName,
                row.installedVersion ?: "",
                remote.selfVersion
            )
        },
        onDismissRequest = { confirmSwitch = null }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                text = stringResource(R.string.button_cancel),
                onClick = { confirmSwitch = null },
                modifier = Modifier.weight(1f)
            )
            TextButton(
                text = stringResource(R.string.dialog_positive),
                onClick = {
                    val remote = pendingSwitch?.second
                    confirmSwitch = null
                    if (remote != null) installRemoteComponent(remote)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    val pendingRemove = confirmRemove
    WindowDialog(
        show = pendingRemove != null,
        title = stringResource(R.string.button_remove),
        summary = pendingRemove?.let { "${it.displayName} (${it.installedVersion ?: ""})?" },
        onDismissRequest = { confirmRemove = null }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                text = stringResource(R.string.button_cancel),
                onClick = { confirmRemove = null },
                modifier = Modifier.weight(1f)
            )
            TextButton(
                text = stringResource(R.string.button_remove),
                onClick = {
                    val libId = pendingRemove?.libraryId
                    confirmRemove = null
                    if (libId != null) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val currentVersion = profile.repository.getVersion(version)
                                val updated = profile.getDependency().removeLibraryAsync(currentVersion, libId).run()
                                profile.repository.saveAsync(updated).run()
                                profile.repository.refreshVersionsAsync().run()
                            }
                            profile.repository.onVersionIconChanged.fireEvent(Event(profile))
                            onVersionChanged()
                            reloadNonce++
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ==================== 2. Mods Management Section ====================

private enum class ModActiveFilter { ALL, ENABLED, DISABLED }

private data class ModRowItem(
    val modInfo: LocalModFile,
    val title: String,
    val subtitle: String,
    val loaderTag: String,
    val localizedName: String?,
    val isActive: Boolean,
    val hasOldFiles: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageModsSection(
    profile: Profile,
    version: String,
    onOpenUpdates: (ModManager, List<LocalModFile.ModUpdate>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = MainActivity.getInstance()
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()

    val modTick by ModsChanged.events.collectAsState(initial = ModsChanged.tick.value)
    var loadedVersion by remember { mutableStateOf<String?>(null) }
    var loadedTick by remember { mutableIntStateOf(-1) }
    var manualRefreshNonce by remember { mutableIntStateOf(0) }

    var isModded by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var modManager by remember { mutableStateOf<ModManager?>(null) }
    var allMods by remember { mutableStateOf<List<ModRowItem>>(emptyList()) }
    var brokenFiles by remember { mutableStateOf<List<Path>>(emptyList()) }

    var query by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(ModActiveFilter.ALL) }
    var sortField by remember { mutableStateOf(ModSortManager.getField()) }
    var sortAscending by remember { mutableStateOf(ModSortManager.isAscending()) }
    var showSortDialog by remember { mutableStateOf(false) }

    var selectedFileNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    var detailTarget by remember { mutableStateOf<ModRowItem?>(null) }
    var rollbackTarget by remember { mutableStateOf<ModRowItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBrokenConfirm by remember { mutableStateOf(false) }

    fun modLoaderLabel(type: ModLoaderType): String = when (type) {
        ModLoaderType.FORGE -> context.getString(R.string.install_installer_forge)
        ModLoaderType.NEO_FORGED -> context.getString(R.string.install_installer_neoforge)
        ModLoaderType.FABRIC -> context.getString(R.string.install_installer_fabric)
        ModLoaderType.LITE_LOADER -> context.getString(R.string.install_installer_liteloader)
        ModLoaderType.QUILT -> context.getString(R.string.install_installer_quilt)
        else -> ""
    }

    fun buildModRow(file: LocalModFile): ModRowItem {
        val title = buildString {
            append(file.name)
            if (StringUtils.isNotBlank(file.version)) append(" ").append(file.version)
        }
        val subtitle = buildString {
            append(file.fileName)
            if (StringUtils.isNotBlank(file.gameVersion)) {
                append(", ").append(context.getString(R.string.archive_game_version)).append(": ").append(file.gameVersion)
            }
            if (StringUtils.isNotBlank(file.authors)) {
                append(", ").append(context.getString(R.string.archive_author)).append(": ").append(file.authors)
            }
        }
        val projInfo = AddonProjectInfoRegistry.MOD.getInfo(file.id, file.name)
        val zhName = if (projInfo != null && LocaleUtils.isChinese(context)) projInfo.displayName() else null
        return ModRowItem(
            modInfo = file,
            title = title,
            subtitle = subtitle,
            loaderTag = modLoaderLabel(file.modLoaderType),
            localizedName = zhName,
            isActive = file.isActive,
            hasOldFiles = file.mod.oldFiles.isNotEmpty()
        )
    }

    fun sortRows(list: List<ModRowItem>, field: ModSortField, asc: Boolean): List<ModRowItem> {
        if (field == ModSortField.DEFAULT) return list
        val collator = Collator.getInstance()
        val cmp = when (field) {
            ModSortField.NAME -> Comparator<ModRowItem> { a, b -> collator.compare(a.modInfo.name ?: "", b.modInfo.name ?: "") }
            ModSortField.FILE_NAME -> Comparator<ModRowItem> { a, b -> collator.compare(a.modInfo.fileName ?: "", b.modInfo.fileName ?: "") }
            ModSortField.SIZE -> {
                val sizes = HashMap<Path, Long>()
                Comparator<ModRowItem> { a, b ->
                    val sa = sizes.getOrPut(a.modInfo.file) { runCatching { Files.size(a.modInfo.file) }.getOrDefault(0L) }
                    val sb = sizes.getOrPut(b.modInfo.file) { runCatching { Files.size(b.modInfo.file) }.getOrDefault(0L) }
                    sa.compareTo(sb)
                }
            }
            ModSortField.DATE -> {
                val times = HashMap<Path, Long>()
                Comparator<ModRowItem> { a, b ->
                    val ta = times.getOrPut(a.modInfo.file) { runCatching { Files.getLastModifiedTime(a.modInfo.file).toMillis() }.getOrDefault(0L) }
                    val tb = times.getOrPut(b.modInfo.file) { runCatching { Files.getLastModifiedTime(b.modInfo.file).toMillis() }.getOrDefault(0L) }
                    ta.compareTo(tb)
                }
            }
            ModSortField.DEFAULT -> return list
        }
        return list.sortedWith(if (asc) cmp else cmp.reversed())
    }

    val versionKey = "${profile.name}:$version"
    LaunchedEffect(versionKey, modTick, manualRefreshNonce) {
        if (loadedVersion == versionKey && loadedTick == modTick && manualRefreshNonce == 0 && modManager != null) {
            return@LaunchedEffect
        }
        isLoading = true
        val tickSnapshot = ModsChanged.tick.value
        val result = withContext(Dispatchers.IO) {
            val repo = profile.repository
            val resolved = repo.getResolvedPreservingPatchesVersion(version)
            val analyzer = LibraryAnalyzer.analyze(resolved, repo.getGameVersion(resolved).orElse(null))
            val modded = analyzer.hasModLoader()
            val mgr = repo.getModManager(version)
            val parsed = mutableListOf<ModRowItem>()
            mgr.refreshMods { mod -> parsed.add(buildModRow(mod)) }
            val sorted = sortRows(parsed, sortField, sortAscending)
            val broken = mgr.brokenFiles.toList()
            Triple(modded, mgr, sorted to broken)
        }
        isModded = result.first
        modManager = result.second
        allMods = result.third.first
        brokenFiles = result.third.second
        selectedFileNames = emptySet()
        loadedVersion = versionKey
        loadedTick = tickSnapshot
        isLoading = false
        if (brokenFiles.isNotEmpty()) {
            showBrokenConfirm = true
        }
    }

    LaunchedEffect(sortField, sortAscending) {
        if (allMods.isNotEmpty()) {
            val sorted = withContext(Dispatchers.IO) { sortRows(allMods, sortField, sortAscending) }
            allMods = sorted
        }
    }

    val filteredMods = remember(allMods, filterMode, query) {
        val byActive = when (filterMode) {
            ModActiveFilter.ALL -> allMods
            ModActiveFilter.ENABLED -> allMods.filter { it.isActive }
            ModActiveFilter.DISABLED -> allMods.filter { !it.isActive }
        }
        val q = query.trim()
        if (q.isEmpty()) byActive
        else if (q.startsWith("regex:")) {
            val pattern = runCatching { Pattern.compile(q.removePrefix("regex:")) }.getOrNull()
            if (pattern == null) byActive else byActive.filter {
                pattern.matcher(it.modInfo.fileName).find() ||
                    pattern.matcher(it.title).find() ||
                    (it.localizedName != null && pattern.matcher(it.localizedName).find())
            }
        } else {
            byActive.filter {
                it.modInfo.fileName.contains(q, ignoreCase = true) ||
                    it.title.contains(q, ignoreCase = true) ||
                    (it.localizedName != null && it.localizedName.contains(q, ignoreCase = true))
            }
        }
    }

    fun checkUpdates(onlySelected: Boolean) {
        val mgr = modManager ?: return
        val targets = if (onlySelected) {
            allMods.filter { it.modInfo.fileName in selectedFileNames }.map { it.modInfo }
        } else {
            mgr.mods.toList()
        }
        if (targets.isEmpty()) return

        val runCheck = Runnable {
            val dialog = TaskDialog(context, TaskCancellationAction.NORMAL)
            dialog.setTitle(context.getString(R.string.update_checking))
            val task = Task.composeAsync {
                val gv = profile.repository.getGameVersion(version)
                if (gv.isPresent) ModCheckUpdatesTask(gv.get(), targets) else null
            }.whenComplete(Schedulers.androidUIThread()) { result, exception ->
                if (exception is CancellationException) return@whenComplete
                when {
                    exception != null || result == null -> FCLAlertDialog.Builder(context)
                        .setCancelable(false)
                        .setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
                        .setTitle(context.getString(R.string.message_failed))
                        .setMessage("Failed to check updates")
                        .setNegativeButton(context.getString(R.string.dialog_positive), null)
                        .create().show()

                    result.isEmpty() -> FCLAlertDialog.Builder(context)
                        .setCancelable(false)
                        .setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
                        .setMessage(context.getString(R.string.mods_check_updates_empty))
                        .setNegativeButton(context.getString(R.string.dialog_positive), null)
                        .create().show()

                    else -> {
                        onOpenUpdates(mgr, result)
                    }
                }
            }.withStagesHint(listOf("mods.check_updates"))
            val executor = task.executor()
            dialog.setExecutor(executor)
            dialog.show()
            executor.start()
        }

        if (profile.repository.isModpack(version)) {
            FCLAlertDialog.Builder(context)
                .setCancelable(false)
                .setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
                .setMessage(context.getString(R.string.mods_update_modpack_mod_warning))
                .setPositiveButton { runCheck.run() }
                .setNegativeButton(null)
                .create().show()
        } else {
            runCheck.run()
        }
    }

    fun addMods() {
        val mgr = modManager ?: return
        activity.fileLauncher.launchMultiSelection(null, listOf(".jar", ".zip", ".litemod")) { files ->
            if (files.isNullOrEmpty()) return@launchMultiSelection
            scope.launch {
                val (succeeded, failed) = withContext(Dispatchers.IO) {
                    val ok = mutableListOf<String>()
                    val bad = mutableListOf<String>()
                    for (file in files) {
                        val name = file.fileName(activity)
                        try {
                            if (file.isContent) {
                                mgr.addMod(activity, file.uri, name)
                            } else {
                                val localFile = file.file ?: throw IOException("Missing local file")
                                mgr.addMod(localFile.toPath())
                            }
                            ok.add(name)
                        } catch (_: Exception) {
                            bad.add(name)
                        }
                    }
                    ok to bad
                }
                val prompt = mutableListOf<String>()
                if (succeeded.isNotEmpty()) prompt += context.getString(R.string.mods_add_success, succeeded.joinToString(", "))
                if (failed.isNotEmpty()) prompt += context.getString(R.string.mods_add_failed, failed.joinToString(", "))
                if (prompt.isNotEmpty()) {
                    FCLAlertDialog.Builder(context)
                        .setAlertLevel(if (failed.isEmpty()) FCLAlertDialog.AlertLevel.INFO else FCLAlertDialog.AlertLevel.ALERT)
                        .setTitle(context.getString(R.string.mods_add))
                        .setMessage(prompt.joinToString("\n"))
                        .setPositiveButton(context.getString(R.string.dialog_positive), null)
                        .create().show()
                }
                manualRefreshNonce++
            }
        }
    }

    val activeCount = remember(allMods) { allMods.count { it.isActive } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!isModded) {
            SectionCard(title = stringResource(R.string.mods_manage)) {
                Text(
                    text = stringResource(R.string.mods_not_modded),
                    color = Color(0xFFE65100),
                    fontSize = 13.sp
                )
            }
            return@Column
        }

        SectionCard(
            title = "${stringResource(R.string.mods_manage)} (${allMods.size})",
            trailingAction = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LauncherButton(
                        text = stringResource(R.string.mods_sort),
                        onClick = { showSortDialog = true },
                        height = 28.dp
                    )
                    LauncherButton(
                        text = stringResource(R.string.action_refresh),
                        enabled = !isLoading,
                        onClick = { manualRefreshNonce++ },
                        height = 28.dp
                    )
                }
            }
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.search),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ManageMemoryPill(
                    label = "${stringResource(R.string.mods_sort_default)} (${allMods.size})",
                    selected = filterMode == ModActiveFilter.ALL,
                    onClick = { filterMode = ModActiveFilter.ALL }
                )
                ManageMemoryPill(
                    label = "${stringResource(R.string.enabled)} ($activeCount)",
                    selected = filterMode == ModActiveFilter.ENABLED,
                    onClick = { filterMode = ModActiveFilter.ENABLED }
                )
                ManageMemoryPill(
                    label = "${stringResource(R.string.disabled)} (${allMods.size - activeCount})",
                    selected = filterMode == ModActiveFilter.DISABLED,
                    onClick = { filterMode = ModActiveFilter.DISABLED }
                )
            }

            Spacer(Modifier.height(8.dp))
            if (selectedFileNames.isEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LauncherButton(
                        text = stringResource(R.string.mods_add),
                        accentStyle = true,
                        enabled = !isLoading,
                        onClick = { addMods() },
                        height = 30.dp
                    )
                    LauncherButton(
                        text = stringResource(R.string.download),
                        onClick = { activity.navigateToDownload(DownloadUI.PAGE_ID_DOWNLOAD_MOD) },
                        height = 30.dp
                    )
                    LauncherButton(
                        text = stringResource(R.string.mods_check_updates),
                        enabled = !isLoading && allMods.isNotEmpty(),
                        onClick = { checkUpdates(false) },
                        height = 30.dp
                    )
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LauncherButton(
                        text = stringResource(R.string.mods_check_updates),
                        accentStyle = true,
                        onClick = { checkUpdates(true) },
                        height = 30.dp
                    )
                    LauncherButton(
                        text = stringResource(R.string.button_remove),
                        onClick = { showDeleteConfirm = true },
                        height = 30.dp
                    )
                    LauncherButton(
                        text = stringResource(R.string.button_select_all),
                        onClick = { selectedFileNames = filteredMods.map { it.modInfo.fileName }.toSet() },
                        height = 30.dp
                    )
                    LauncherButton(
                        text = stringResource(R.string.button_select_invert),
                        onClick = {
                            val current = selectedFileNames
                            selectedFileNames = filteredMods.map { it.modInfo.fileName }.filter { it !in current }.toSet()
                        },
                        height = 30.dp
                    )
                    LauncherButton(
                        text = stringResource(R.string.button_cancel),
                        onClick = { selectedFileNames = emptySet() },
                        height = 30.dp
                    )
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredMods, key = { it.modInfo.fileName }) { item ->
                val isSelected = item.modInfo.fileName in selectedFileNames
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) colors.accent else colors.cardBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedFileNames = if (isSelected) {
                                selectedFileNames - item.modInfo.fileName
                            } else {
                                selectedFileNames + item.modInfo.fileName
                            }
                        },
                    cornerRadius = 8.dp,
                    insideMargin = PaddingValues(12.dp),
                    colors = CardDefaults.defaultColors(
                        color = if (isSelected) colors.accent.copy(alpha = 0.08f) else colors.cardBg,
                        contentColor = colors.textPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.localizedName?.let { "$it · ${item.title}" } ?: item.title,
                                    color = if (item.isActive) colors.textPrimary else colors.textSecondary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (item.loaderTag.isNotEmpty()) {
                                    Text(
                                        text = item.loaderTag,
                                        color = colors.accent,
                                        fontSize = 10.5.sp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.accent.copy(alpha = 0.12f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = item.subtitle,
                                color = colors.textSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (item.hasOldFiles) {
                            LauncherButton(
                                text = stringResource(R.string.mods_restore),
                                onClick = { rollbackTarget = item },
                                height = 28.dp
                            )
                        }
                        LauncherButton(
                            text = stringResource(R.string.dialog_info),
                            onClick = { detailTarget = item },
                            height = 28.dp
                        )
                        Switch(
                            checked = item.isActive,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        item.modInfo.isActive = checked
                                    }
                                    allMods = allMods.map {
                                        if (it.modInfo.fileName == item.modInfo.fileName) it.copy(isActive = item.modInfo.isActive) else it
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Sort dialog
    WindowDialog(
        show = showSortDialog,
        title = stringResource(R.string.mods_sort),
        onDismissRequest = { showSortDialog = false }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ModSortField.values().forEach { field ->
                val labelRes = when (field) {
                    ModSortField.NAME -> R.string.mods_sort_name
                    ModSortField.FILE_NAME -> R.string.mods_sort_file_name
                    ModSortField.SIZE -> R.string.mods_sort_size
                    ModSortField.DATE -> R.string.mods_sort_date
                    else -> R.string.mods_sort_default
                }
                val suffix = if (field == sortField && field != ModSortField.DEFAULT) {
                    if (sortAscending) " ↑" else " ↓"
                } else ""
                ManageMemoryPill(
                    label = stringResource(labelRes) + suffix,
                    selected = field == sortField,
                    onClick = {
                        if (field == sortField && field != ModSortField.DEFAULT) {
                            sortAscending = !sortAscending
                        } else {
                            sortField = field
                            sortAscending = true
                        }
                        ModSortManager.set(sortField, sortAscending)
                        showSortDialog = false
                    }
                )
            }
        }
    }

    // Mod Info dialog
    val infoMod = detailTarget
    var modLogoBitmap by remember(infoMod) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(infoMod) {
        modLogoBitmap = if (infoMod != null && StringUtils.isNotBlank(infoMod.modInfo.logoPath)) {
            withContext(Dispatchers.IO) {
                runCatching {
                    CompressingUtils.createReadOnlyZipFileSystem(infoMod.modInfo.file).use { fs ->
                        val iconPath = fs.getPath(infoMod.modInfo.logoPath)
                        if (Files.exists(iconPath)) {
                            val bytes = ByteArrayOutputStream().also { Files.copy(iconPath, it) }.toByteArray()
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        } else null
                    }
                }.getOrNull()
            }
        } else null
    }
    WindowDialog(
        show = infoMod != null,
        title = infoMod?.modInfo?.name ?: "",
        onDismissRequest = { detailTarget = null }
    ) {
        if (infoMod != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (modLogoBitmap != null) {
                        Image(bitmap = modLogoBitmap!!, contentDescription = null, modifier = Modifier.size(40.dp))
                    } else {
                        Image(painter = painterResource(R.drawable.img_command), contentDescription = null, modifier = Modifier.size(40.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${infoMod.loaderTag} ${infoMod.modInfo.version.orEmpty()}".trim(),
                            color = colors.accent,
                            fontSize = 12.sp
                        )
                        Text(
                            text = FileUtils.getName(infoMod.modInfo.file),
                            color = colors.textSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }
                val desc = infoMod.modInfo.description.toString()
                if (desc.isNotBlank()) {
                    Text(
                        text = desc,
                        color = colors.textPrimary,
                        fontSize = 12.5.sp,
                        modifier = Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (StringUtils.isNotBlank(infoMod.modInfo.url)) {
                        TextButton(
                            text = "URL",
                            onClick = { openLink(context, infoMod.modInfo.url) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    TextButton(
                        text = stringResource(R.string.dialog_positive),
                        onClick = { detailTarget = null },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Rollback old mod version dialog
    val rollItem = rollbackTarget
    WindowDialog(
        show = rollItem != null,
        title = rollItem?.title ?: "",
        onDismissRequest = { rollbackTarget = null }
    ) {
        if (rollItem != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rollItem.modInfo.mod.oldFiles.forEach { oldFile ->
                    LauncherButton(
                        text = oldFile.version ?: oldFile.fileName,
                        onClick = {
                            val mgr = modManager
                            rollbackTarget = null
                            if (mgr != null) {
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) {
                                        runCatching { mgr.rollback(rollItem.modInfo, oldFile); true }.getOrDefault(false)
                                    }
                                    if (!ok) {
                                        Toast.makeText(context, R.string.message_failed, Toast.LENGTH_SHORT).show()
                                    }
                                    manualRefreshNonce++
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 34.dp
                    )
                }
                TextButton(
                    text = stringResource(R.string.button_cancel),
                    onClick = { rollbackTarget = null },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Delete selected confirm
    WindowDialog(
        show = showDeleteConfirm,
        title = stringResource(R.string.button_remove),
        summary = stringResource(R.string.button_remove_confirm),
        onDismissRequest = { showDeleteConfirm = false }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                text = stringResource(R.string.button_cancel),
                onClick = { showDeleteConfirm = false },
                modifier = Modifier.weight(1f)
            )
            TextButton(
                text = stringResource(R.string.button_remove),
                onClick = {
                    val mgr = modManager
                    val toDelete = allMods.filter { it.modInfo.fileName in selectedFileNames }.map { it.modInfo }.toTypedArray()
                    showDeleteConfirm = false
                    if (mgr != null && toDelete.isNotEmpty()) {
                        scope.launch {
                            withContext(Dispatchers.IO) { runCatching { mgr.removeMods(*toDelete) } }
                            manualRefreshNonce++
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Broken mods confirm
    WindowDialog(
        show = showBrokenConfirm && brokenFiles.isNotEmpty(),
        title = stringResource(R.string.button_remove),
        summary = stringResource(R.string.message_broken_mods, brokenFiles.joinToString("\n") { FileUtils.getName(it) }),
        onDismissRequest = { showBrokenConfirm = false }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                text = stringResource(R.string.button_cancel),
                onClick = { showBrokenConfirm = false },
                modifier = Modifier.weight(1f)
            )
            TextButton(
                text = stringResource(R.string.button_remove),
                onClick = {
                    val paths = brokenFiles
                    showBrokenConfirm = false
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            paths.forEach { runCatching { Files.deleteIfExists(it) } }
                        }
                        manualRefreshNonce++
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ==================== 3. Worlds & Datapacks Management Section ====================

@Composable
fun ManageWorldsSection(
    profile: Profile,
    version: String,
    onOpenWorldInfo: (World) -> Unit,
    onOpenDatapacks: (World) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = MainActivity.getInstance()
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()

    var reloadNonce by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    var gameVersion by remember { mutableStateOf<String?>(null) }
    var worlds by remember { mutableStateOf<List<World>>(emptyList()) }

    var pendingInstallWorld by remember { mutableStateOf<World?>(null) }
    var pendingInstallName by remember { mutableStateOf("") }
    var duplicateTarget by remember { mutableStateOf<World?>(null) }
    var duplicateName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<World?>(null) }
    var exportTarget by remember { mutableStateOf<kotlin.Pair<World, String>?>(null) }
    var datapackUnsupportedNotice by remember { mutableStateOf(false) }

    val savesDir = remember(profile, version) {
        profile.repository.getRunDirectory(version).toPath().resolve("saves")
    }

    LaunchedEffect(profile, version, reloadNonce) {
        isLoading = true
        val (gv, loaded) = withContext(Dispatchers.IO) {
            val g = profile.repository.getGameVersion(version).orElse(null)
            val list = runCatching { World.getWorlds(savesDir) }.getOrElse { emptyList() }
            g to list
        }
        gameVersion = gv
        worlds = loaded
        isLoading = false
    }

    val filteredWorlds = remember(worlds, showAll, gameVersion) {
        val gvNumber = gameVersion?.let { GameVersionNumber.asGameVersion(it) }
        worlds.filter { w ->
            showAll || w.gameVersion == null || (gvNumber != null && w.gameVersion!!.compareTo(gvNumber) == 0)
        }
    }

    val needsPrivateFix = remember(savesDir, worlds) {
        worlds.isNotEmpty() && (
            savesDir.pathString.startsWith(FCLPath.PRIVATE_COMMON_DIR) ||
                savesDir.pathString.contains("/Android/data/${context.applicationInfo.packageName}/")
            )
    }

    fun importWorldZip() {
        activity.fileLauncher.launchSingleSelection(null, listOf(".zip")) { files ->
            val picked = files?.firstOrNull() ?: return@launchSingleSelection
            val zipFile = picked.toFile(activity, File(FCLPath.CACHE_DIR))
            scope.launch {
                val parsed = withContext(Dispatchers.IO) { runCatching { World(zipFile.toPath()) }.getOrNull() }
                if (parsed == null) {
                    FCLAlertDialog.Builder(context)
                        .setCancelable(false)
                        .setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
                        .setMessage(context.getString(R.string.world_import_invalid))
                        .setPositiveButton(context.getString(R.string.dialog_positive), null)
                        .create().show()
                } else {
                    pendingInstallName = parsed.worldName
                    pendingInstallWorld = parsed
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionCard(
            title = "${stringResource(R.string.world_manage)} (${filteredWorlds.size})",
            trailingAction = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LauncherButton(
                        text = stringResource(R.string.world_add),
                        accentStyle = true,
                        onClick = { importWorldZip() },
                        height = 28.dp
                    )
                    LauncherButton(
                        text = stringResource(R.string.action_refresh),
                        enabled = !isLoading,
                        onClick = { reloadNonce++ },
                        height = 28.dp
                    )
                }
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(checked = showAll, onCheckedChange = { showAll = it })
                    Text(
                        text = stringResource(R.string.world_show_all),
                        color = colors.textPrimary,
                        fontSize = 13.sp
                    )
                }
                if (needsPrivateFix) {
                    LauncherButton(
                        text = stringResource(R.string.world_permission_fix),
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    Files.walk(savesDir).use { stream ->
                                        stream.forEach { path -> Files.setAttribute(path, "unix:mode", 1535) }
                                    }
                                }
                                Toast.makeText(context, R.string.message_success, Toast.LENGTH_LONG).show()
                            }
                        },
                        height = 28.dp
                    )
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredWorlds, key = { it.file.toString() }) { world ->
                val iconBmp = remember(world) { world.icon?.asImageBitmap() }
                val sub = remember(world) {
                    context.getString(
                        R.string.world_description,
                        world.fileName,
                        LocaleUtils.formatDateTime(context, Instant.ofEpochMilli(world.lastPlayed)),
                        world.gameVersion?.toNormalizedString() ?: context.getString(R.string.message_unknown)
                    )
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp)),
                    cornerRadius = 8.dp,
                    insideMargin = PaddingValues(12.dp),
                    colors = CardDefaults.defaultColors(color = colors.cardBg, contentColor = colors.textPrimary)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (iconBmp != null) {
                                Image(
                                    bitmap = iconBmp,
                                    contentDescription = world.worldName,
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))
                                )
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.img_grass),
                                    contentDescription = world.worldName,
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = StringUtils.parseColorEscapes(world.worldName),
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = sub,
                                    color = colors.textSecondary,
                                    fontSize = 11.5.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LauncherButton(
                                text = stringResource(R.string.dialog_info),
                                onClick = { onOpenWorldInfo(world) },
                                height = 28.dp
                            )
                            LauncherButton(
                                text = stringResource(R.string.world_datapack),
                                onClick = {
                                    if (!world.supportDataPacks()) {
                                        datapackUnsupportedNotice = true
                                    } else {
                                        onOpenDatapacks(world)
                                    }
                                },
                                height = 28.dp
                            )
                            LauncherButton(
                                text = stringResource(R.string.world_export),
                                onClick = {
                                    activity.fileLauncher.launchSingleSelection(null, null, true) { files ->
                                        val dir = files?.firstOrNull()?.path ?: return@launchSingleSelection
                                        exportTarget = world to dir
                                    }
                                },
                                height = 28.dp
                            )
                            LauncherButton(
                                text = stringResource(R.string.world_duplicate),
                                onClick = {
                                    duplicateName = world.worldName
                                    duplicateTarget = world
                                },
                                height = 28.dp
                            )
                            LauncherButton(
                                text = stringResource(R.string.button_remove),
                                onClick = { deleteTarget = world },
                                height = 28.dp
                            )
                        }
                    }
                }
            }
        }
    }

    // Install world name dialog
    val installW = pendingInstallWorld
    WindowDialog(
        show = installW != null,
        title = stringResource(R.string.world_add),
        onDismissRequest = { pendingInstallWorld = null }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextField(
                value = pendingInstallName,
                onValueChange = { pendingInstallName = it },
                label = stringResource(R.string.world_name),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    text = stringResource(R.string.button_cancel),
                    onClick = { pendingInstallWorld = null },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = stringResource(R.string.dialog_positive),
                    onClick = {
                        val name = pendingInstallName.trim()
                        val target = installW
                        pendingInstallWorld = null
                        if (target != null && name.isNotEmpty()) {
                            scope.launch {
                                val err = withContext(Dispatchers.IO) {
                                    runCatching { target.install(savesDir, name) }.exceptionOrNull()
                                }
                                if (err == null) {
                                    reloadNonce++
                                } else {
                                    val msg = when (err) {
                                        is FileAlreadyExistsException -> context.getString(
                                            R.string.world_import_failed,
                                            context.getString(R.string.world_import_already_exists)
                                        )
                                        is IOException if err.cause is InvalidPathException ->
                                            getLocalizedText(context, context.getString(R.string.install_new_game_malformed))
                                        else -> getLocalizedText(context, "${err.javaClass.name}: ${err.localizedMessage}")
                                    }
                                    showErrorDialog(context, msg)
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Duplicate world dialog
    val dupW = duplicateTarget
    WindowDialog(
        show = dupW != null,
        title = stringResource(R.string.world_duplicate),
        onDismissRequest = { duplicateTarget = null }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextField(
                value = duplicateName,
                onValueChange = { duplicateName = it },
                label = stringResource(R.string.world_name),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    text = stringResource(R.string.button_cancel),
                    onClick = { duplicateTarget = null },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = stringResource(R.string.dialog_positive),
                    onClick = {
                        val name = duplicateName.trim()
                        val w = dupW
                        duplicateTarget = null
                        if (w != null && name.isNotEmpty()) {
                            if (Files.exists(w.file.resolveSibling(name))) {
                                showErrorDialog(context, context.getString(R.string.world_import_already_exists))
                            } else {
                                scope.launch {
                                    val err = withContext(Dispatchers.IO) { runCatching { w.copy(name) }.exceptionOrNull() }
                                    if (err == null) {
                                        Toast.makeText(context, R.string.message_success, Toast.LENGTH_SHORT).show()
                                        reloadNonce++
                                    } else if (err is WorldLockedException) {
                                        showErrorDialog(context, context.getString(R.string.world_locked_failed))
                                    } else {
                                        showErrorDialog(context, err.toString())
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Delete world confirm
    val delW = deleteTarget
    WindowDialog(
        show = delW != null,
        title = stringResource(R.string.button_remove),
        summary = delW?.let { stringResource(R.string.version_manage_remove_confirm, it.worldName) },
        onDismissRequest = { deleteTarget = null }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                text = stringResource(R.string.button_cancel),
                onClick = { deleteTarget = null },
                modifier = Modifier.weight(1f)
            )
            TextButton(
                text = stringResource(R.string.button_remove),
                onClick = {
                    val w = delW
                    deleteTarget = null
                    if (w != null) {
                        scope.launch {
                            val err = withContext(Dispatchers.IO) { runCatching { w.delete() }.exceptionOrNull() }
                            if (err == null) reloadNonce++
                            else if (err is WorldLockedException) showErrorDialog(context, context.getString(R.string.world_locked_failed))
                            else showErrorDialog(context, err.toString())
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    WindowDialog(
        show = datapackUnsupportedNotice,
        title = stringResource(R.string.world_datapack),
        summary = stringResource(R.string.world_datapack_1_13),
        onDismissRequest = { datapackUnsupportedNotice = false }
    ) {
        TextButton(
            text = stringResource(R.string.dialog_positive),
            onClick = { datapackUnsupportedNotice = false },
            modifier = Modifier.fillMaxWidth()
        )
    }

    exportTarget?.let { (world, dir) ->
        WorldExportComposeDialog(
            world = world,
            targetDirPath = dir,
            onDismiss = { exportTarget = null }
        )
    }
}

// ==================== 4. Resource Packs & 5. Shader Packs Sections ====================

@Composable
fun ManageResourcePacksSection(
    profile: Profile,
    version: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = MainActivity.getInstance()
    val scope = rememberCoroutineScope()

    val gameDir = remember(profile, version) { profile.repository.getRunDirectory(version) }
    val packsDir = remember(gameDir) { File(gameDir, "resourcepacks") }

    var reloadNonce by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var allPacks by remember { mutableStateOf<List<ResourcePack>>(emptyList()) }
    var renameTarget by remember { mutableStateOf<ResourcePack?>(null) }
    var infoTarget by remember { mutableStateOf<ResourcePack?>(null) }

    LaunchedEffect(profile, version, reloadNonce) {
        isLoading = true
        allPacks = withContext(Dispatchers.IO) {
            runCatching { listResourcePacks(packsDir, readEnabledResourcePacks(gameDir)) }.getOrElse { emptyList() }
        }
        isLoading = false
    }

    PackListScaffold(
        title = stringResource(R.string.resourcepack_manage),
        addLabel = stringResource(R.string.resourcepack_add),
        items = allPacks,
        itemKey = { it.file.absolutePath },
        itemName = { it.name },
        itemSearchText = { "${it.name} ${it.description.orEmpty()}" },
        isLoading = isLoading,
        onRefresh = { reloadNonce++ },
        onDownload = { activity.navigateToDownload(DownloadUI.PAGE_ID_DOWNLOAD_RESOURCE_PACK) },
        onAddZip = {
            importPackZips(
                activity = activity,
                context = context,
                targetDir = packsDir,
                successRes = R.string.resourcepack_add_success,
                failedRes = R.string.resourcepack_add_failed,
                onComplete = { reloadNonce++ }
            )
        },
        onDeleteBatch = { selected ->
            scope.launch {
                withContext(Dispatchers.IO) { selected.forEach { it.file.deleteRecursively() } }
                reloadNonce++
            }
        },
        onRenameClick = { renameTarget = it },
        onInfoClick = { infoTarget = it },
        itemSubtitle = { pack ->
            buildString {
                if (!pack.isValid) append("[Invalid] ")
                pack.fileSize?.let { append(formatPackFileSize(it)).append("  ") }
                pack.packFormat?.let { append("Format ").append(it).append("  ") }
                pack.description?.takeIf { it.isNotBlank() }?.let { append(it) }
            }
        },
        itemIcon = { it.icon },
        itemEnabled = { it.enabled },
        onToggleEnabled = { pack, checked ->
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching { setResourcePackEnabled(gameDir, pack.file.name, checked) }.getOrDefault(false)
                }
                if (ok) {
                    allPacks = allPacks.map { if (it.file == pack.file) it.copy(enabled = checked) else it }
                } else {
                    Toast.makeText(context, R.string.message_failed, Toast.LENGTH_SHORT).show()
                }
            }
        },
        modifier = modifier
    )

    PackRenameDialog(
        show = renameTarget != null,
        initialName = renameTarget?.name ?: "",
        onDismiss = { renameTarget = null },
        onConfirm = { trimmed ->
            val target = renameTarget ?: return@PackRenameDialog
            renameTarget = null
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    val parent = target.file.parentFile ?: return@withContext false
                    val dest = if (target.isDirectory) File(parent, trimmed)
                    else File(parent, "$trimmed.${target.file.extension}")
                    if (dest.exists()) false else target.file.renameTo(dest)
                }
                if (ok) reloadNonce++
                else Toast.makeText(context, R.string.resourcepack_rename_exists, Toast.LENGTH_SHORT).show()
            }
        }
    )

    val info = infoTarget
    WindowDialog(
        show = info != null,
        title = info?.name ?: "",
        summary = info?.let { pack ->
            buildString {
                append(
                    context.getString(
                        R.string.resourcepack_info_type,
                        context.getString(if (pack.isDirectory) R.string.resourcepack_info_type_folder else R.string.resourcepack_info_type_zip)
                    )
                )
                append("\n").append(context.getString(R.string.resourcepack_info_file_name, pack.file.name))
                pack.fileSize?.let { append("\n").append(context.getString(R.string.resourcepack_info_file_size, formatPackFileSize(it))) }
                pack.packFormat?.let { append("\n").append(context.getString(R.string.resourcepack_info_format, it)) }
                pack.description?.takeIf { it.isNotBlank() }?.let { append("\n").append(it) }
            }
        },
        onDismissRequest = { infoTarget = null }
    ) {
        TextButton(
            text = stringResource(R.string.dialog_positive),
            onClick = { infoTarget = null },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ManageShaderPacksSection(
    profile: Profile,
    version: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = MainActivity.getInstance()
    val scope = rememberCoroutineScope()

    val packsDir = remember(profile, version) { File(profile.repository.getRunDirectory(version), "shaderpacks") }

    var reloadNonce by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var allPacks by remember { mutableStateOf<List<ShaderPack>>(emptyList()) }
    var renameTarget by remember { mutableStateOf<ShaderPack?>(null) }
    var infoTarget by remember { mutableStateOf<ShaderPack?>(null) }

    LaunchedEffect(profile, version, reloadNonce) {
        isLoading = true
        allPacks = withContext(Dispatchers.IO) {
            runCatching { listShaderPacks(packsDir) }.getOrElse { emptyList() }
        }
        isLoading = false
    }

    PackListScaffold(
        title = stringResource(R.string.shaderpack_manage),
        addLabel = stringResource(R.string.shaderpack_add),
        items = allPacks,
        itemKey = { it.file.absolutePath },
        itemName = { it.name },
        itemSearchText = { "${it.name} ${it.file.name}" },
        isLoading = isLoading,
        onRefresh = { reloadNonce++ },
        onDownload = { activity.navigateToDownload(DownloadUI.PAGE_ID_DOWNLOAD_SHADER_PACK) },
        onAddZip = {
            importPackZips(
                activity = activity,
                context = context,
                targetDir = packsDir,
                successRes = R.string.shaderpack_add_success,
                failedRes = R.string.shaderpack_add_failed,
                onComplete = { reloadNonce++ }
            )
        },
        onDeleteBatch = { selected ->
            scope.launch {
                withContext(Dispatchers.IO) { selected.forEach { it.file.deleteRecursively() } }
                reloadNonce++
            }
        },
        onRenameClick = { renameTarget = it },
        onInfoClick = { infoTarget = it },
        itemSubtitle = { pack ->
            buildString {
                if (!pack.isValid) append("[Invalid] ")
                append(pack.file.name)
                pack.fileSize?.let { append(" · ").append(formatPackFileSize(it)) }
            }
        },
        itemIcon = { null },
        itemEnabled = null,
        onToggleEnabled = null,
        modifier = modifier
    )

    PackRenameDialog(
        show = renameTarget != null,
        initialName = renameTarget?.name ?: "",
        onDismiss = { renameTarget = null },
        onConfirm = { trimmed ->
            val target = renameTarget ?: return@PackRenameDialog
            renameTarget = null
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    val parent = target.file.parentFile ?: return@withContext false
                    val dest = if (target.isDirectory) File(parent, trimmed)
                    else File(parent, "$trimmed.${target.file.extension}")
                    if (dest.exists()) false else target.file.renameTo(dest)
                }
                if (ok) reloadNonce++
                else Toast.makeText(context, R.string.shaderpack_rename_exists, Toast.LENGTH_SHORT).show()
            }
        }
    )

    val info = infoTarget
    WindowDialog(
        show = info != null,
        title = info?.name ?: "",
        summary = info?.let { pack ->
            buildString {
                append(
                    context.getString(
                        R.string.shaderpack_info_type,
                        context.getString(if (pack.isDirectory) R.string.shaderpack_info_type_folder else R.string.shaderpack_info_type_zip)
                    )
                )
                append("\n").append(context.getString(R.string.shaderpack_info_file_name, pack.file.name))
                pack.fileSize?.let { append("\n").append(context.getString(R.string.shaderpack_info_file_size, formatPackFileSize(it))) }
            }
        },
        onDismissRequest = { infoTarget = null }
    ) {
        TextButton(
            text = stringResource(R.string.dialog_positive),
            onClick = { infoTarget = null },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==================== Shared Pack UI Helpers ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> PackListScaffold(
    title: String,
    addLabel: String,
    items: List<T>,
    itemKey: (T) -> String,
    itemName: (T) -> String,
    itemSearchText: (T) -> String,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onDownload: () -> Unit,
    onAddZip: () -> Unit,
    onDeleteBatch: (List<T>) -> Unit,
    onRenameClick: (T) -> Unit,
    onInfoClick: (T) -> Unit,
    itemSubtitle: (T) -> String,
    itemIcon: (T) -> Bitmap?,
    itemEnabled: ((T) -> Boolean)?,
    onToggleEnabled: ((T, Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    var query by remember { mutableStateOf("") }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val filtered = remember(items, query) {
        val q = query.trim()
        if (q.isEmpty()) items else items.filter { itemSearchText(it).contains(q, ignoreCase = true) }
    }

    LaunchedEffect(items) {
        val valid = items.map(itemKey).toSet()
        selectedKeys = selectedKeys.intersect(valid)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionCard(
            title = "$title (${items.size})",
            trailingAction = {
                LauncherButton(
                    text = stringResource(R.string.action_refresh),
                    enabled = !isLoading,
                    onClick = onRefresh,
                    height = 28.dp
                )
            }
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.search),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            if (selectedKeys.isEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LauncherButton(
                        text = addLabel,
                        accentStyle = true,
                        enabled = !isLoading,
                        onClick = onAddZip,
                        height = 30.dp
                    )
                    LauncherButton(
                        text = stringResource(R.string.download),
                        onClick = onDownload,
                        height = 30.dp
                    )
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LauncherButton(
                        text = stringResource(R.string.button_remove),
                        onClick = { showDeleteConfirm = true },
                        height = 30.dp
                    )
                    LauncherButton(
                        text = stringResource(R.string.button_select_all),
                        onClick = { selectedKeys = filtered.map(itemKey).toSet() },
                        height = 30.dp
                    )
                    LauncherButton(
                        text = stringResource(R.string.button_select_invert),
                        onClick = {
                            val current = selectedKeys
                            selectedKeys = filtered.map(itemKey).filter { it !in current }.toSet()
                        },
                        height = 30.dp
                    )
                    LauncherButton(
                        text = stringResource(R.string.button_cancel),
                        onClick = { selectedKeys = emptySet() },
                        height = 30.dp
                    )
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = itemKey) { item ->
                val key = itemKey(item)
                val isSelected = key in selectedKeys
                val iconBmp = remember(item) { itemIcon(item)?.asImageBitmap() }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) colors.accent else colors.cardBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedKeys = if (isSelected) selectedKeys - key else selectedKeys + key
                        },
                    cornerRadius = 8.dp,
                    insideMargin = PaddingValues(12.dp),
                    colors = CardDefaults.defaultColors(
                        color = if (isSelected) colors.accent.copy(alpha = 0.08f) else colors.cardBg,
                        contentColor = colors.textPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (iconBmp != null) {
                            Image(
                                bitmap = iconBmp,
                                contentDescription = itemName(item),
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = itemName(item),
                                color = colors.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sub = itemSubtitle(item)
                            if (sub.isNotBlank()) {
                                Text(
                                    text = sub,
                                    color = colors.textSecondary,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        LauncherButton(
                            text = stringResource(R.string.edit),
                            onClick = { onRenameClick(item) },
                            height = 28.dp
                        )
                        LauncherButton(
                            text = stringResource(R.string.dialog_info),
                            onClick = { onInfoClick(item) },
                            height = 28.dp
                        )
                        if (itemEnabled != null && onToggleEnabled != null) {
                            Switch(
                                checked = itemEnabled(item),
                                onCheckedChange = { onToggleEnabled(item, it) }
                            )
                        }
                    }
                }
            }
        }
    }

    WindowDialog(
        show = showDeleteConfirm,
        title = stringResource(R.string.button_remove),
        summary = stringResource(R.string.button_remove_confirm),
        onDismissRequest = { showDeleteConfirm = false }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                text = stringResource(R.string.button_cancel),
                onClick = { showDeleteConfirm = false },
                modifier = Modifier.weight(1f)
            )
            TextButton(
                text = stringResource(R.string.button_remove),
                onClick = {
                    val picked = items.filter { itemKey(it) in selectedKeys }
                    showDeleteConfirm = false
                    selectedKeys = emptySet()
                    if (picked.isNotEmpty()) onDeleteBatch(picked)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PackRenameDialog(
    show: Boolean,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var draft by remember(initialName, show) { mutableStateOf(initialName) }
    WindowDialog(
        show = show,
        title = stringResource(R.string.version_manage_rename),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextField(
                value = draft,
                onValueChange = { draft = it },
                label = stringResource(R.string.version_manage_rename_new),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    text = stringResource(R.string.button_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = stringResource(R.string.dialog_positive),
                    onClick = {
                        val trimmed = draft.trim()
                        if (trimmed.isNotEmpty()) onConfirm(trimmed)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun importPackZips(
    activity: MainActivity,
    context: Context,
    targetDir: File,
    successRes: Int,
    failedRes: Int,
    onComplete: () -> Unit
) {
    activity.fileLauncher.launchMultiSelection(null, listOf(".zip")) { files ->
        if (files.isNullOrEmpty()) return@launchMultiSelection
        Task.supplyAsync<Pair<List<String>, List<String>>> {
            val ok = mutableListOf<String>()
            val bad = mutableListOf<String>()
            targetDir.mkdirs()
            for (file in files) {
                val name = file.fileName(activity)
                try {
                    val input = file.openInputStream(activity) ?: throw IOException()
                    val dest = uniquePackFile(targetDir, name)
                    dest.outputStream().use { out -> input.copyTo(out) }
                    ok += name
                } catch (_: Exception) {
                    bad += name
                }
            }
            ok to bad
        }.whenComplete(Schedulers.androidUIThread()) { pair, _ ->
            val succeeded = pair?.first ?: emptyList()
            val failed = pair?.second ?: emptyList()
            val prompt = mutableListOf<String>()
            if (succeeded.isNotEmpty()) prompt += context.getString(successRes, succeeded.joinToString(", "))
            if (failed.isNotEmpty()) prompt += context.getString(failedRes, failed.joinToString(", "))
            if (prompt.isNotEmpty()) {
                FCLAlertDialog.Builder(context)
                    .setAlertLevel(if (failed.isEmpty()) FCLAlertDialog.AlertLevel.INFO else FCLAlertDialog.AlertLevel.ALERT)
                    .setMessage(prompt.joinToString("\n"))
                    .setPositiveButton(context.getString(R.string.dialog_positive), null)
                    .create().show()
            }
            onComplete()
        }.start()
    }
}

private fun uniquePackFile(dir: File, fileName: String): File {
    val base = fileName.substringBeforeLast('.')
    val ext = fileName.substringAfterLast('.', "")
    var index = 0
    while (true) {
        val name = if (index == 0) fileName else if (ext.isEmpty()) "$base ($index)" else "$base ($index).$ext"
        val target = File(dir, name)
        if (!target.exists()) return target
        index++
    }
}

private fun formatPackFileSize(size: Long): String = when {
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", size / 1024.0)
    size < 1024L * 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", size / 1024.0 / 1024.0)
    else -> String.format(Locale.getDefault(), "%.1f GB", size / 1024.0 / 1024.0 / 1024.0)
}
