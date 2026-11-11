package top.nkbe.pclf.ui

import android.content.Context
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.ViewModelProvider
import com.mio.util.getLocalizedText
import com.mio.util.hasStringId
import com.mio.util.openLink
import com.mio.util.pixelAwareIcon
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.game.FCLGameRepository
import com.tungsten.fcl.setting.DownloadProviders
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fcl.ui.TaskDialog
import com.tungsten.fcl.ui.download.version.DownloadListViewModel
import com.tungsten.fcl.util.TaskCancellationAction
import com.tungsten.fclcore.download.ArtifactMalformedException
import com.tungsten.fclcore.download.ComponentRemoteVersion
import com.tungsten.fclcore.download.DefaultDependencyManager
import com.tungsten.fclcore.download.LibraryAnalyzer
import com.tungsten.fclcore.download.UnsupportedInstallationException
import com.tungsten.fclcore.download.VersionMismatchException
import com.tungsten.fclcore.download.fabric.FabricAPIRemoteVersion
import com.tungsten.fclcore.download.fabric.FabricRemoteVersion
import com.tungsten.fclcore.download.forge.ForgeRemoteVersion
import com.tungsten.fclcore.download.game.GameAssetIndexDownloadTask
import com.tungsten.fclcore.download.game.GameRemoteVersion
import com.tungsten.fclcore.download.game.LibraryDownloadException
import com.tungsten.fclcore.download.legacyfabric.LegacyFabricAPIRemoteVersion
import com.tungsten.fclcore.download.legacyfabric.LegacyFabricRemoteVersion
import com.tungsten.fclcore.download.liteloader.LiteLoaderRemoteVersion
import com.tungsten.fclcore.download.neoforge.NeoForgeRemoteVersion
import com.tungsten.fclcore.download.optifine.OptiFineRemoteVersion
import com.tungsten.fclcore.download.quilt.QuiltAPIRemoteVersion
import com.tungsten.fclcore.download.quilt.QuiltRemoteVersion
import com.tungsten.fclcore.game.GameComponentType
import com.tungsten.fclcore.task.DownloadException
import com.tungsten.fclcore.task.Schedulers
import com.tungsten.fclcore.task.Task
import com.tungsten.fclcore.task.TaskExecutor
import com.tungsten.fclcore.task.TaskListener
import com.tungsten.fclcore.util.Logging
import com.tungsten.fclcore.util.StringUtils
import com.tungsten.fclcore.util.function.ExceptionalRunnable
import com.tungsten.fclcore.util.io.ResponseCodeException
import com.tungsten.fclcore.util.versioning.GameVersionNumber
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog
import com.tungsten.fcllibrary.util.LocaleUtils
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LauncherButton
import top.nkbe.pclf.ui.components.SectionCard
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.window.WindowDialog
import java.io.File
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException
import java.util.logging.Level
import java.util.zip.ZipException


@Composable
private fun DownloadCardBlock(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LauncherTheme.colors
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp)),
        cornerRadius = 8.dp,
        insideMargin = PaddingValues(14.dp),
        colors = CardDefaults.defaultColors(
            color = colors.cardBg,
            contentColor = colors.textPrimary
        ),
        content = content
    )
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LauncherTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) colors.accent.copy(alpha = 0.14f) else colors.buttonBg)
            .border(
                width = 1.dp,
                color = if (selected) colors.accent else colors.buttonBorderNeutral,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = label,
            color = if (selected) colors.accent else colors.textPrimary,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@DrawableRes
private fun componentIconRes(type: LibraryAnalyzer.LibraryType): Int = when (type) {
    LibraryAnalyzer.LibraryType.FORGE -> R.drawable.img_forge
    LibraryAnalyzer.LibraryType.CLEANROOM -> R.drawable.img_cleanroom
    LibraryAnalyzer.LibraryType.NEO_FORGE -> R.drawable.img_neoforge
    LibraryAnalyzer.LibraryType.LITELOADER -> R.drawable.img_chicken
    LibraryAnalyzer.LibraryType.OPTIFINE -> R.drawable.img_optifine
    LibraryAnalyzer.LibraryType.FABRIC, LibraryAnalyzer.LibraryType.FABRIC_API -> R.drawable.img_fabric
    LibraryAnalyzer.LibraryType.LEGACY_FABRIC, LibraryAnalyzer.LibraryType.LEGACY_FABRIC_API -> R.drawable.img_legacyfabric
    LibraryAnalyzer.LibraryType.QUILT, LibraryAnalyzer.LibraryType.QUILT_API -> R.drawable.img_quilt
    else -> R.drawable.img_grass
}

@DrawableRes
private fun remoteVersionIconRes(remote: ComponentRemoteVersion): Int = when (remote) {
    is LiteLoaderRemoteVersion -> R.drawable.img_chicken
    is OptiFineRemoteVersion -> R.drawable.img_optifine
    is ForgeRemoteVersion -> R.drawable.img_forge
    is NeoForgeRemoteVersion -> R.drawable.img_neoforge
    is FabricRemoteVersion, is FabricAPIRemoteVersion -> R.drawable.img_fabric
    is LegacyFabricRemoteVersion, is LegacyFabricAPIRemoteVersion -> R.drawable.img_legacyfabric
    is QuiltRemoteVersion, is QuiltAPIRemoteVersion -> R.drawable.img_quilt
    is GameRemoteVersion -> when (remote.versionType) {
        ComponentRemoteVersion.Type.RELEASE -> R.drawable.img_grass
        ComponentRemoteVersion.Type.PENDING,
        ComponentRemoteVersion.Type.UNOBFUSCATED,
        ComponentRemoteVersion.Type.SNAPSHOT -> {
            if (GameVersionNumber.asGameVersion(remote.gameVersion).isAprilFools) R.drawable.april_fools
            else R.drawable.img_command
        }
        else -> R.drawable.img_craft_table
    }
    else -> R.drawable.img_grass
}

@Composable
private fun rememberPixelIconBitmap(@DrawableRes resId: Int): ImageBitmap? {
    val context = LocalContext.current
    return remember(resId) {
        runCatching {
            pixelAwareIcon(context, resId)?.toBitmap()?.asImageBitmap()
        }.getOrNull()
    }
}

private fun gameWikiUrlSuffix(context: Context, gameVersion: String): String {
    val id = gameVersion.lowercase()
    when (id) {
        "0.30-1", "0.30-2", "c0.30_01c" -> return context.getString(R.string.wiki_game_search, "Classic_0.30")
        "in-20100206-2103" -> return context.getString(R.string.wiki_game_search, "Indev_20100206")
        "inf-20100630-1" -> return context.getString(R.string.wiki_game_search, "Infdev_20100630")
        "inf-20100630-2" -> return context.getString(R.string.wiki_game_search, "Alpha_v1.0.0")
        "1.19_deep_dark_experimental_snapshot-1" -> return "1.19-exp1"
        "in-20100130" -> return context.getString(R.string.wiki_game_search, "Indev_0.31_20100130")
        "b1.6-tb3" -> return context.getString(R.string.wiki_game_search, "Beta_1.6_Test_Build_3")
    }
    if (id.startsWith("1.0.0-rc2")) return "RC2"
    if (id.startsWith("2.0")) return context.getString(R.string.wiki_game_search, "2.0")
    if (id.startsWith("b1.8-pre1")) return "Beta_1.8-pre1"
    if (id.startsWith("b1.1-")) return context.getString(R.string.wiki_game_search, "Beta_1.1")
    if (id.startsWith("a1.1.0")) return "Alpha_v1.1.0"
    if (id.startsWith("a1.0.14")) return "Alpha_v1.0.14"
    if (id.startsWith("a1.0.13_01")) return "Alpha_v1.0.13_01"
    if (id.startsWith("in-20100214")) return context.getString(R.string.wiki_game_search, "Indev_20100214")
    if (id.contains("experimental-snapshot")) return id.replace("-experimental-snapshot", "-exp")
    if (id.startsWith("inf-")) return id.replace("inf-", "Infdev_")
    if (id.startsWith("in-")) return id.replace("in-", "Indev_")
    if (id.startsWith("rd-")) return "pre-Classic_$id"
    if (id.startsWith("b")) return id.replace("b", "Beta_")
    if (id.startsWith("a")) return id.replace("a", "Alpha_v")
    if (id.startsWith("c")) return id.replace("c", "Classic_").replace("st", "SURVIVAL_TEST")
    return id
}

fun alertInstallFailureMessage(context: Context, exception: Exception?, onPositive: () -> Unit = {}) {
    if (exception == null || exception is CancellationException) return

    val title: String
    val msg: String
    when (exception) {
        is LibraryDownloadException -> {
            var body = context.getString(R.string.launch_failed_download_library, exception.library.name) + "\n"
            val cause = exception.cause
            body += if (cause is ResponseCodeException) {
                if (cause.responseCode == 404) {
                    context.getString(R.string.download_code_404, cause.url)
                } else {
                    context.getString(R.string.download_failed, cause.url, cause.responseCode)
                }
            } else {
                StringUtils.getStackTrace(cause)
            }
            title = context.getString(R.string.install_failed_downloading)
            msg = body
        }

        is DownloadException -> {
            val url = exception.url
            val cause = exception.cause
            title = context.getString(R.string.install_failed_downloading)
            msg = when (cause) {
                is SocketTimeoutException -> context.getString(R.string.install_failed_downloading_timeout, url)
                is ResponseCodeException -> {
                    val key = "download_code_${cause.responseCode}"
                    if (hasStringId(context, key)) getLocalizedText(context, key, url)
                    else context.getString(R.string.install_failed_downloading_detail, url)
                }
                else -> context.getString(R.string.install_failed_downloading_detail, url) + "\n" + StringUtils.getStackTrace(cause)
            }
        }

        is UnsupportedInstallationException -> {
            title = context.getString(R.string.install_failed)
            msg = if (exception.reason == UnsupportedInstallationException.FORGE_1_17_OPTIFINE_H1_PRE2) {
                context.getString(R.string.install_failed_optifine_forge_1_17)
            } else {
                context.getString(R.string.install_failed_optifine_conflict)
            }
        }

        is DefaultDependencyManager.UnsupportedLibraryInstallerException -> {
            title = context.getString(R.string.install_failed)
            msg = context.getString(R.string.install_failed_install_online)
        }

        is ArtifactMalformedException, is ZipException -> {
            title = context.getString(R.string.install_failed)
            msg = context.getString(R.string.install_failed_malformed)
        }

        is GameAssetIndexDownloadTask.GameAssetIndexMalformedException -> {
            title = context.getString(R.string.install_failed)
            msg = context.getString(R.string.assets_index_malformed)
        }

        is VersionMismatchException -> {
            title = context.getString(R.string.install_failed)
            msg = context.getString(R.string.install_failed_version_mismatch, exception.expect, exception.actual)
        }

        else -> {
            title = context.getString(R.string.install_failed)
            msg = StringUtils.getStackTrace(exception)
        }
    }

    FCLAlertDialog.Builder(context)
        .setCancelable(false)
        .setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
        .setTitle(title)
        .setMessage(msg)
        .setNegativeButton(context.getString(R.string.dialog_positive)) { onPositive() }
        .create()
        .show()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameVersionInstallTab(
    onSelectGameVersion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = MainActivity.getInstance()
    val colors = LauncherTheme.colors

    val viewModel = remember(activity) {
        ViewModelProvider(activity)["download_list_game", DownloadListViewModel::class.java]
    }
    val state by viewModel.state.collectAsState()

    var showRelease by remember { mutableStateOf(true) }
    var showSnapshot by remember { mutableStateOf(false) }
    var showOld by remember { mutableStateOf(false) }
    var showAprilFools by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(viewModel) {
        viewModel.load("", GameComponentType.GAME, false)
    }

    val allVersions = remember(state) {
        when (val s = state) {
            is DownloadListViewModel.State.Loaded -> s.versions
            is DownloadListViewModel.State.Failed -> s.lastList
            else -> emptyList()
        }
    }

    LaunchedEffect(allVersions) {
        if (allVersions.isNotEmpty() && !showRelease && !showSnapshot && !showOld && !showAprilFools) {
            showRelease = true
            showSnapshot = true
            showOld = true
        }
    }

    val filteredVersions = remember(allVersions, showRelease, showSnapshot, showOld, showAprilFools, searchText) {
        val q = searchText.trim()
        allVersions
            .filter { item ->
                val matchesType = when (item.versionType) {
                    ComponentRemoteVersion.Type.RELEASE -> showRelease
                    ComponentRemoteVersion.Type.PENDING,
                    ComponentRemoteVersion.Type.UNOBFUSCATED,
                    ComponentRemoteVersion.Type.SNAPSHOT -> {
                        showSnapshot || (showAprilFools && GameVersionNumber.asGameVersion(item.gameVersion).isAprilFools)
                    }
                    ComponentRemoteVersion.Type.OLD -> {
                        showOld || (showAprilFools && GameVersionNumber.asGameVersion(item.gameVersion).isAprilFools)
                    }
                    else -> true
                }
                matchesType && (q.isEmpty() || item.gameVersion.contains(q, ignoreCase = true))
            }
            .sorted()
    }

    val isLoading = state is DownloadListViewModel.State.Idle || state is DownloadListViewModel.State.Loading
    val failedState = state as? DownloadListViewModel.State.Failed

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DownloadCardBlock {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = stringResource(R.string.search),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                LauncherButton(
                    text = stringResource(R.string.action_refresh),
                    enabled = !isLoading,
                    onClick = {
                        searchText = ""
                        viewModel.load("", GameComponentType.GAME, true)
                    },
                    height = 34.dp
                )
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipPill(
                    label = stringResource(R.string.version_game_release),
                    selected = showRelease,
                    onClick = { showRelease = !showRelease }
                )
                FilterChipPill(
                    label = stringResource(R.string.version_game_snapshot),
                    selected = showSnapshot,
                    onClick = { showSnapshot = !showSnapshot }
                )
                FilterChipPill(
                    label = stringResource(R.string.version_game_old),
                    selected = showOld,
                    onClick = { showOld = !showOld }
                )
                FilterChipPill(
                    label = stringResource(R.string.version_game_april_fools),
                    selected = showAprilFools,
                    onClick = { showAprilFools = !showAprilFools }
                )
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (failedState != null) {
            DownloadCardBlock {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = failedState.error.localizedMessage ?: failedState.error.javaClass.simpleName,
                        color = Color(0xFFD32F2F),
                        fontSize = 12.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    LauncherButton(
                        text = stringResource(R.string.action_refresh),
                        accentStyle = true,
                        onClick = { viewModel.load("", GameComponentType.GAME, true) },
                        height = 30.dp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredVersions, key = { "${it.versionType}_${it.gameVersion}_${it.selfVersion}" }) { remote ->
                RemoteVersionCardRow(
                    remote = remote,
                    onSelect = { onSelectGameVersion(remote.gameVersion) },
                    onOpenWiki = if (remote is GameRemoteVersion &&
                        (remote.versionType == ComponentRemoteVersion.Type.RELEASE ||
                            remote.versionType == ComponentRemoteVersion.Type.SNAPSHOT ||
                            remote.versionType == ComponentRemoteVersion.Type.UNOBFUSCATED)
                    ) {
                        {
                            val suffix = gameWikiUrlSuffix(context, remote.gameVersion)
                            openLink(context, context.getString(R.string.wiki_game, suffix))
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun RemoteVersionCardRow(
    remote: ComponentRemoteVersion,
    onSelect: () -> Unit,
    onOpenWiki: (() -> Unit)? = null,
    onOpenUrls: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val iconRes = remember(remote) { remoteVersionIconRes(remote) }
    val iconBmp = rememberPixelIconBitmap(iconRes)

    val tagText = remember(remote) {
        if (remote is GameRemoteVersion) {
            when (remote.versionType) {
                ComponentRemoteVersion.Type.RELEASE -> context.getString(R.string.version_game_release)
                ComponentRemoteVersion.Type.UNOBFUSCATED,
                ComponentRemoteVersion.Type.PENDING,
                ComponentRemoteVersion.Type.SNAPSHOT -> context.getString(R.string.version_game_snapshot)
                else -> context.getString(R.string.version_game_old)
            }
        } else {
            remote.gameVersion
        }
    }

    val dateText = remember(remote.releaseDate) {
        remote.releaseDate?.let { LocaleUtils.formatDateTime(context, it) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect),
        cornerRadius = 8.dp,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        colors = CardDefaults.defaultColors(
            color = colors.cardBg,
            contentColor = colors.textPrimary
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (iconBmp != null) {
                Image(
                    bitmap = iconBmp,
                    contentDescription = remote.selfVersion,
                    modifier = Modifier.size(30.dp)
                )
            } else {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = remote.selfVersion,
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = remote.selfVersion,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!tagText.isNullOrBlank()) {
                        Text(
                            text = tagText,
                            color = colors.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.accent.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (!dateText.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = dateText,
                        color = colors.textSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onOpenWiki != null) {
                LauncherButton(
                    text = "Wiki",
                    onClick = onOpenWiki,
                    height = 28.dp
                )
            }
            if (onOpenUrls != null) {
                LauncherButton(
                    text = stringResource(R.string.button_download),
                    onClick = onOpenUrls,
                    height = 28.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentVersionPickerOverlay(
    gameVersion: String,
    libraryId: String,
    onSelect: (ComponentRemoteVersion) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = MainActivity.getInstance()
    val colors = LauncherTheme.colors

    val componentType = remember(libraryId) { GameComponentType.fromPatchId(libraryId) }
    val displayTitle = remember(libraryId) {
        getLocalizedText(context, "install_installer_${libraryId.replace('.', '_').replace('-', '_')}")
    }
    val viewModel = remember(activity, libraryId) {
        ViewModelProvider(activity)["download_list_$libraryId", DownloadListViewModel::class.java]
    }
    val hasTypeFilter = remember(libraryId) {
        runCatching {
            DownloadProviders.getDownloadProvider().getVersionList(componentType).hasType()
        }.getOrDefault(false)
    }

    val state by viewModel.state.collectAsState()
    var showRelease by remember(libraryId) { mutableStateOf(true) }
    var showSnapshot by remember(libraryId) { mutableStateOf(false) }
    var showOld by remember(libraryId) { mutableStateOf(false) }
    var urlChooserUrls by remember { mutableStateOf<List<String>?>(null) }

    LaunchedEffect(gameVersion, libraryId) {
        viewModel.load(gameVersion, componentType, false)
    }

    val allVersions = remember(state) {
        when (val s = state) {
            is DownloadListViewModel.State.Loaded -> s.versions
            is DownloadListViewModel.State.Failed -> s.lastList
            else -> emptyList()
        }
    }

    LaunchedEffect(allVersions, state) {
        if (state is DownloadListViewModel.State.Loaded && allVersions.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.download_failed_empty), Toast.LENGTH_SHORT).show()
        } else if (allVersions.isNotEmpty() && !showRelease && !showSnapshot && !showOld) {
            showRelease = true
            showSnapshot = true
            showOld = true
        }
    }

    val filteredVersions = remember(allVersions, showRelease, showSnapshot, showOld, hasTypeFilter) {
        val list = if (!hasTypeFilter) {
            allVersions
        } else {
            allVersions.filter { item ->
                when (item.versionType) {
                    ComponentRemoteVersion.Type.RELEASE -> showRelease
                    ComponentRemoteVersion.Type.SNAPSHOT -> showSnapshot
                    ComponentRemoteVersion.Type.OLD -> showOld
                    else -> true
                }
            }
        }
        list.sorted()
    }

    val isLoading = state is DownloadListViewModel.State.Idle || state is DownloadListViewModel.State.Loading
    val failedState = state as? DownloadListViewModel.State.Failed

    val urlsDialogTarget = urlChooserUrls
    if (urlsDialogTarget != null) {
        WindowDialog(
            show = true,
            title = stringResource(R.string.message_select_url),
            onDismissRequest = { urlChooserUrls = null }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                urlsDialogTarget.forEach { url ->
                    LauncherButton(
                        text = url,
                        onClick = {
                            urlChooserUrls = null
                            openLink(context, url)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 34.dp
                    )
                }
                TextButton(
                    text = stringResource(R.string.button_cancel),
                    onClick = { urlChooserUrls = null },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DownloadCardBlock {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LauncherButton(
                    text = stringResource(R.string.button_back),
                    onClick = onBack,
                    height = 30.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Minecraft $gameVersion",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                LauncherButton(
                    text = stringResource(R.string.action_refresh),
                    enabled = !isLoading,
                    onClick = { viewModel.load(gameVersion, componentType, true) },
                    height = 30.dp
                )
            }

            if (hasTypeFilter) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipPill(
                        label = stringResource(R.string.version_game_release),
                        selected = showRelease,
                        onClick = { showRelease = !showRelease }
                    )
                    FilterChipPill(
                        label = stringResource(R.string.version_game_snapshot),
                        selected = showSnapshot,
                        onClick = { showSnapshot = !showSnapshot }
                    )
                    FilterChipPill(
                        label = stringResource(R.string.version_game_old),
                        selected = showOld,
                        onClick = { showOld = !showOld }
                    )
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (failedState != null) {
            DownloadCardBlock {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = failedState.error.localizedMessage ?: failedState.error.javaClass.simpleName,
                        color = Color(0xFFD32F2F),
                        fontSize = 12.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    LauncherButton(
                        text = stringResource(R.string.action_refresh),
                        accentStyle = true,
                        onClick = { viewModel.load(gameVersion, componentType, true) },
                        height = 30.dp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredVersions, key = { "${it.selfVersion}_${it.gameVersion}_${it.versionType}" }) { remote ->
                RemoteVersionCardRow(
                    remote = remote,
                    onSelect = { onSelect(remote) },
                    onOpenUrls = if (remote !is GameRemoteVersion &&
                        remote !is FabricAPIRemoteVersion &&
                        remote !is QuiltAPIRemoteVersion &&
                        remote !is LegacyFabricAPIRemoteVersion &&
                        !remote.urls.isNullOrEmpty()
                    ) {
                        { urlChooserUrls = remote.urls }
                    } else null
                )
            }
        }
    }
}

private val ALL_COMPONENT_INCOMPATIBILITIES: Map<LibraryAnalyzer.LibraryType, Set<LibraryAnalyzer.LibraryType>> = run {
    val map = mutableMapOf<LibraryAnalyzer.LibraryType, MutableSet<LibraryAnalyzer.LibraryType>>()
    fun link(a: LibraryAnalyzer.LibraryType, vararg others: LibraryAnalyzer.LibraryType) {
        val set = map.getOrPut(a) { mutableSetOf() }
        for (o in others) {
            set.add(o)
            map.getOrPut(o) { mutableSetOf() }.add(a)
        }
    }
    val loaders = arrayOf(
        LibraryAnalyzer.LibraryType.FORGE,
        LibraryAnalyzer.LibraryType.FABRIC,
        LibraryAnalyzer.LibraryType.QUILT,
        LibraryAnalyzer.LibraryType.NEO_FORGE,
        LibraryAnalyzer.LibraryType.CLEANROOM,
        LibraryAnalyzer.LibraryType.LEGACY_FABRIC
    )
    loaders.forEach { a -> loaders.filter { it != a }.forEach { b -> link(a, b) } }
    link(
        LibraryAnalyzer.LibraryType.OPTIFINE,
        LibraryAnalyzer.LibraryType.FABRIC,
        LibraryAnalyzer.LibraryType.QUILT,
        LibraryAnalyzer.LibraryType.NEO_FORGE,
        LibraryAnalyzer.LibraryType.CLEANROOM,
        LibraryAnalyzer.LibraryType.LITELOADER,
        LibraryAnalyzer.LibraryType.LEGACY_FABRIC
    )
    link(
        LibraryAnalyzer.LibraryType.LITELOADER,
        LibraryAnalyzer.LibraryType.FABRIC,
        LibraryAnalyzer.LibraryType.QUILT,
        LibraryAnalyzer.LibraryType.NEO_FORGE,
        LibraryAnalyzer.LibraryType.CLEANROOM,
        LibraryAnalyzer.LibraryType.LEGACY_FABRIC
    )
    link(
        LibraryAnalyzer.LibraryType.FABRIC_API,
        LibraryAnalyzer.LibraryType.FORGE,
        LibraryAnalyzer.LibraryType.QUILT_API,
        LibraryAnalyzer.LibraryType.NEO_FORGE,
        LibraryAnalyzer.LibraryType.LITELOADER,
        LibraryAnalyzer.LibraryType.OPTIFINE,
        LibraryAnalyzer.LibraryType.CLEANROOM,
        LibraryAnalyzer.LibraryType.LEGACY_FABRIC,
        LibraryAnalyzer.LibraryType.LEGACY_FABRIC_API
    )
    link(
        LibraryAnalyzer.LibraryType.QUILT_API,
        LibraryAnalyzer.LibraryType.FORGE,
        LibraryAnalyzer.LibraryType.FABRIC,
        LibraryAnalyzer.LibraryType.FABRIC_API,
        LibraryAnalyzer.LibraryType.NEO_FORGE,
        LibraryAnalyzer.LibraryType.LITELOADER,
        LibraryAnalyzer.LibraryType.OPTIFINE,
        LibraryAnalyzer.LibraryType.CLEANROOM,
        LibraryAnalyzer.LibraryType.LEGACY_FABRIC,
        LibraryAnalyzer.LibraryType.LEGACY_FABRIC_API
    )
    link(
        LibraryAnalyzer.LibraryType.LEGACY_FABRIC_API,
        LibraryAnalyzer.LibraryType.FORGE,
        LibraryAnalyzer.LibraryType.FABRIC,
        LibraryAnalyzer.LibraryType.FABRIC_API,
        LibraryAnalyzer.LibraryType.NEO_FORGE,
        LibraryAnalyzer.LibraryType.LITELOADER,
        LibraryAnalyzer.LibraryType.OPTIFINE,
        LibraryAnalyzer.LibraryType.CLEANROOM,
        LibraryAnalyzer.LibraryType.QUILT,
        LibraryAnalyzer.LibraryType.QUILT_API
    )
    map
}

private fun availableComponentTypesForGame(gameVersion: String): List<LibraryAnalyzer.LibraryType> = when {
    gameVersion == "1.12.2" -> listOf(
        LibraryAnalyzer.LibraryType.FORGE,
        LibraryAnalyzer.LibraryType.CLEANROOM,
        LibraryAnalyzer.LibraryType.LITELOADER,
        LibraryAnalyzer.LibraryType.LEGACY_FABRIC,
        LibraryAnalyzer.LibraryType.LEGACY_FABRIC_API,
        LibraryAnalyzer.LibraryType.OPTIFINE
    )
    GameVersionNumber.compare(gameVersion, "1.13.2") <= 0 -> listOf(
        LibraryAnalyzer.LibraryType.FORGE,
        LibraryAnalyzer.LibraryType.LITELOADER,
        LibraryAnalyzer.LibraryType.OPTIFINE,
        LibraryAnalyzer.LibraryType.LEGACY_FABRIC,
        LibraryAnalyzer.LibraryType.LEGACY_FABRIC_API
    )
    else -> listOf(
        LibraryAnalyzer.LibraryType.FORGE,
        LibraryAnalyzer.LibraryType.NEO_FORGE,
        LibraryAnalyzer.LibraryType.OPTIFINE,
        LibraryAnalyzer.LibraryType.FABRIC,
        LibraryAnalyzer.LibraryType.FABRIC_API,
        LibraryAnalyzer.LibraryType.QUILT,
        LibraryAnalyzer.LibraryType.QUILT_API
    )
}

private fun loaderDisplayName(context: Context, type: LibraryAnalyzer.LibraryType): String? = when (type) {
    LibraryAnalyzer.LibraryType.FORGE -> context.getString(R.string.install_installer_forge)
    LibraryAnalyzer.LibraryType.NEO_FORGE -> context.getString(R.string.install_installer_neoforge)
    LibraryAnalyzer.LibraryType.CLEANROOM -> context.getString(R.string.install_installer_cleanroom)
    LibraryAnalyzer.LibraryType.FABRIC -> context.getString(R.string.install_installer_fabric)
    LibraryAnalyzer.LibraryType.LEGACY_FABRIC -> context.getString(R.string.install_installer_legacyfabric)
    LibraryAnalyzer.LibraryType.LITELOADER -> context.getString(R.string.install_installer_liteloader)
    LibraryAnalyzer.LibraryType.QUILT -> context.getString(R.string.install_installer_quilt)
    LibraryAnalyzer.LibraryType.OPTIFINE -> context.getString(R.string.install_installer_optifine)
    else -> null
}

private fun companionApiId(loaderId: String): String? = when (loaderId) {
    LibraryAnalyzer.LibraryType.FABRIC.patchId -> LibraryAnalyzer.LibraryType.FABRIC_API.patchId
    LibraryAnalyzer.LibraryType.QUILT.patchId -> LibraryAnalyzer.LibraryType.QUILT_API.patchId
    else -> null
}

@Composable
fun GameInstallConfigOverlay(
    gameVersion: String,
    pickingLibraryId: String?,
    onPickingLibraryIdChange: (String?) -> Unit,
    onBack: () -> Unit,
    onInstallComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors

    val selectedComponents = remember(gameVersion) { mutableStateMapOf<String, ComponentRemoteVersion>() }

    fun buildAutoName(): String {
        val sb = StringBuilder(gameVersion)
        LibraryAnalyzer.LibraryType.entries
            .filter { selectedComponents.containsKey(it.patchId) }
            .mapNotNull { loaderDisplayName(context, it) }
            .forEach { sb.append('-').append(it) }
        return sb.toString()
    }

    var customName by remember(gameVersion) { mutableStateOf(gameVersion) }
    var nameManuallyModified by remember(gameVersion) { mutableStateOf(false) }

    fun refreshAutoNameIfNeeded() {
        if (!nameManuallyModified) {
            customName = buildAutoName()
        }
    }

    fun incompatibleWith(type: LibraryAnalyzer.LibraryType): LibraryAnalyzer.LibraryType? {
        val conflicts = ALL_COMPONENT_INCOMPATIBILITIES[type] ?: return null
        return conflicts.firstOrNull { selectedComponents.containsKey(it.patchId) }
    }

    fun tryAutoSelectApi(loaderId: String) {
        val apiId = companionApiId(loaderId) ?: return
        if (selectedComponents.containsKey(apiId)) return
        val apiType = LibraryAnalyzer.LibraryType.entries.firstOrNull { it.patchId == apiId } ?: return
        val list = DownloadProviders.getDownloadProvider().getVersionList(GameComponentType.fromPatchId(apiId))
        list.refreshAsync(gameVersion)
            .whenComplete(Schedulers.androidUIThread()) { _, exception ->
                if (exception != null) {
                    Logging.LOG.log(Level.WARNING, "Failed to fetch $apiId versions", exception)
                    return@whenComplete
                }
                if (!selectedComponents.containsKey(loaderId) || selectedComponents.containsKey(apiId)) return@whenComplete
                if (incompatibleWith(apiType) != null) return@whenComplete
                list.getVersions(gameVersion).sorted().firstOrNull()?.let { first ->
                    selectedComponents[apiId] = first
                    refreshAutoNameIfNeeded()
                }
            }.start()
    }

    fun startInstall() {
        val name = customName.trim()
        val profile = Profiles.getSelectedProfile()
        if (name.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.input_not_empty), Toast.LENGTH_SHORT).show()
            return
        }
        if (profile.repository.versionIdConflicts(name)) {
            Toast.makeText(context, context.getString(R.string.install_new_game_already_exists), Toast.LENGTH_SHORT).show()
            return
        }
        if (!FCLGameRepository.isValidVersionId(name)) {
            Toast.makeText(context, context.getString(R.string.install_new_game_malformed), Toast.LENGTH_SHORT).show()
            return
        }

        val builder = profile.getDependency().gameBuilder()
        builder.name(name)
        builder.gameVersion(gameVersion)
        selectedComponents.forEach { (key, remote) ->
            if (key != LibraryAnalyzer.LibraryType.MINECRAFT.patchId) {
                builder.version(remote)
            }
        }

        val optiFineId = LibraryAnalyzer.LibraryType.OPTIFINE.patchId
        val hasModsLoader = selectedComponents.isNotEmpty() &&
            !(selectedComponents.size == 1 && selectedComponents.containsKey(optiFineId))

        val task = builder.buildAsync()
            .whenComplete(Task.FinalizedCallback { _ -> profile.repository.refreshVersions() })
            .thenRunAsync<Exception>(Schedulers.androidUIThread(), ExceptionalRunnable {
                val selectedProfile = Profiles.getSelectedProfile()
                selectedProfile.selectedVersion = name
                if (hasModsLoader) {
                    File(selectedProfile.repository.getRunDirectory(name), "mods").mkdirs()
                }
            })

        val pane = TaskDialog(context, TaskCancellationAction(AppCompatDialog::dismiss))
        pane.setTitle(context.getString(R.string.install_new_game))
        val executor = task.executor(object : TaskListener() {
            override fun onStop(success: Boolean, executor: TaskExecutor) {
                Schedulers.androidUIThread().execute {
                    if (success) {
                        FCLAlertDialog.Builder(context)
                            .setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
                            .setCancelable(false)
                            .setMessage(context.getString(R.string.install_success))
                            .setNegativeButton(context.getString(R.string.dialog_positive)) {
                                onInstallComplete()
                            }
                            .create()
                            .show()
                    } else if (executor.exception != null) {
                        alertInstallFailureMessage(context, executor.exception)
                    }
                }
            }
        })
        pane.setExecutor(executor)
        pane.show()
        executor.start()
    }

    val activePickerId = pickingLibraryId
    if (activePickerId != null) {
        ComponentVersionPickerOverlay(
            gameVersion = gameVersion,
            libraryId = activePickerId,
            onSelect = { remote ->
                selectedComponents[activePickerId] = remote
                refreshAutoNameIfNeeded()
                onPickingLibraryIdChange(null)
                tryAutoSelectApi(activePickerId)
            },
            onBack = { onPickingLibraryIdChange(null) },
            modifier = modifier
        )
        return
    }

    val availableTypes = remember(gameVersion) { availableComponentTypesForGame(gameVersion) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DownloadCardBlock {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LauncherButton(
                    text = stringResource(R.string.button_back),
                    onClick = onBack,
                    height = 30.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.install_new_game),
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Minecraft $gameVersion",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                LauncherButton(
                    text = stringResource(R.string.pclf_install),
                    accentStyle = true,
                    onClick = ::startInstall,
                    height = 32.dp
                )
            }
        }

        SectionCard(title = stringResource(R.string.archive_name)) {
            TextField(
                value = customName,
                onValueChange = { value ->
                    customName = value
                    if (value != buildAutoName()) {
                        nameManuallyModified = true
                    }
                },
                label = stringResource(R.string.archive_name),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionCard(title = stringResource(R.string.settings_tabs_installers)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                availableTypes.forEach { libType ->
                    val libId = libType.patchId
                    val libName = remember(libId) {
                        getLocalizedText(context, "install_installer_${libId.replace('.', '_').replace('-', '_')}")
                    }
                    val chosen = selectedComponents[libId]
                    val incompatible = incompatibleWith(libType)
                    val iconRes = remember(libType) { componentIconRes(libType) }
                    val iconBmp = rememberPixelIconBitmap(iconRes)

                    val stateLabel = when {
                        incompatible != null -> stringResource(
                            R.string.install_installer_incompatible,
                            getLocalizedText(context, "install_installer_${incompatible.patchId.replace('.', '_').replace('-', '_')}")
                        )
                        chosen != null -> chosen.selfVersion
                        else -> stringResource(R.string.install_installer_not_installed)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                            .let { mod ->
                                if (incompatible == null) {
                                    mod.clickable {
                                        if (libType == LibraryAnalyzer.LibraryType.FABRIC_API ||
                                            libType == LibraryAnalyzer.LibraryType.LEGACY_FABRIC_API
                                        ) {
                                            FCLAlertDialog.Builder(context)
                                                .setCancelable(false)
                                                .setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
                                                .setMessage(
                                                    context.getString(
                                                        if (libType == LibraryAnalyzer.LibraryType.LEGACY_FABRIC_API) {
                                                            R.string.install_installer_legacyfabric_api_warning
                                                        } else {
                                                            R.string.install_installer_fabric_api_warning
                                                        }
                                                    )
                                                )
                                                .setNegativeButton(context.getString(R.string.dialog_positive), null)
                                                .create()
                                                .show()
                                        }
                                        onPickingLibraryIdChange(libId)
                                    }
                                } else mod
                            },
                        cornerRadius = 8.dp,
                        insideMargin = PaddingValues(12.dp),
                        colors = CardDefaults.defaultColors(
                            color = colors.cardBg,
                            contentColor = colors.textPrimary
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (iconBmp != null) {
                                Image(
                                    bitmap = iconBmp,
                                    contentDescription = libName,
                                    modifier = Modifier.size(30.dp)
                                )
                            } else {
                                Image(
                                    painter = painterResource(iconRes),
                                    contentDescription = libName,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = libName,
                                    color = colors.textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stateLabel,
                                    color = when {
                                        incompatible != null -> Color(0xFFD32F2F)
                                        chosen != null -> colors.accent
                                        else -> colors.textSecondary
                                    },
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (chosen != null) {
                                LauncherButton(
                                    text = stringResource(R.string.button_remove),
                                    onClick = {
                                        selectedComponents.remove(libId)
                                        companionApiId(libId)?.let { selectedComponents.remove(it) }
                                        refreshAutoNameIfNeeded()
                                    },
                                    height = 28.dp
                                )
                            }
                            if (incompatible == null) {
                                LauncherButton(
                                    text = if (chosen == null) stringResource(R.string.pclf_install) else stringResource(R.string.install_change_version),
                                    accentStyle = chosen != null,
                                    onClick = {
                                        if (libType == LibraryAnalyzer.LibraryType.FABRIC_API ||
                                            libType == LibraryAnalyzer.LibraryType.LEGACY_FABRIC_API
                                        ) {
                                            FCLAlertDialog.Builder(context)
                                                .setCancelable(false)
                                                .setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
                                                .setMessage(
                                                    context.getString(
                                                        if (libType == LibraryAnalyzer.LibraryType.LEGACY_FABRIC_API) {
                                                            R.string.install_installer_legacyfabric_api_warning
                                                        } else {
                                                            R.string.install_installer_fabric_api_warning
                                                        }
                                                    )
                                                )
                                                .setNegativeButton(context.getString(R.string.dialog_positive), null)
                                                .create()
                                                .show()
                                        }
                                        onPickingLibraryIdChange(libId)
                                    },
                                    height = 28.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
