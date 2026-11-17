package top.nkbe.pclf.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.mio.data.FavoriteManager
import com.mio.data.favorite.DownloadFavoriteEntity
import com.mio.download.DownloadManager
import com.mio.util.format
import com.mio.util.getLocalizedText
import com.mio.util.openLink
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.game.LocalizedRemoteModRepository
import com.tungsten.fcl.setting.DownloadProviders
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fcl.ui.download.common.DownloadCategory
import com.tungsten.fcl.ui.download.common.DownloadSearchViewModel
import com.tungsten.fcl.ui.download.favorite.handleFavoriteClick
import com.tungsten.fcl.ui.version.Versions
import com.tungsten.fclcore.mod.ModLoaderType
import com.tungsten.fclcore.mod.RemoteMod
import com.tungsten.fclcore.mod.RemoteModRepository
import com.tungsten.fclcore.mod.curse.CurseAddon
import com.tungsten.fclcore.mod.curse.CurseForgeRemoteModRepository
import com.tungsten.fclcore.mod.modrinth.ModrinthRemoteModRepository
import com.tungsten.fclcore.mod.server.ServerModpackManifest
import com.tungsten.fclcore.task.FileDownloadTask
import com.tungsten.fclcore.task.Schedulers
import com.tungsten.fclcore.task.Task
import com.tungsten.fclcore.util.gson.JsonUtils
import com.tungsten.fclcore.util.io.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LauncherButton
import top.nkbe.pclf.ui.components.SectionCard
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.stream.Collectors

private const val ADDON_PAGE_SIZE = 30

private val GAME_VERSION_OPTIONS = listOf(
    "", "1.21.11", "1.21.10", "1.21.8", "1.21.5", "1.21.4", "1.21.1", "1.21",
    "1.20.6", "1.20.4", "1.20.2", "1.20.1", "1.19.4", "1.19.2",
    "1.18.2", "1.16.5", "1.12.2", "1.8.9", "1.7.10"
)

private val MOD_LOADER_LABELS = listOf("All", "Forge", "NeoForge", "Fabric", "Quilt")

@Composable
fun AddonSearchScreen(
    section: DownloadSection,
    onSelectMod: (RemoteMod) -> Unit,
    onOpenModpackWizard: (ModpackSource) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { MainActivity.getInstance() }
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()
    val viewModel = remember(activity) {
        ViewModelProvider(activity)[DownloadSearchViewModel::class.java]
    }
    val vmState = remember(section.pageId) { viewModel.getState(section.pageId) }
    val favorites by FavoriteManager.favorites.collectAsState()

    var searchQuery by remember(section.pageId) { mutableStateOf(vmState.searchFilter) }
    var selectedGameVersion by remember(section.pageId) { mutableStateOf(vmState.userGameVersion.orEmpty()) }
    var selectedCategory by remember(section.pageId) {
        mutableStateOf(vmState.category as? DownloadCategory ?: DownloadCategory.ALL)
    }
    var selectedSort by remember(section.pageId) { mutableStateOf(vmState.sortType) }
    var selectedSource by remember(section.pageId) {
        val defaultSrc = if (section == DownloadSection.WORLD) AddonSourceOption.CURSEFORGE else AddonSourceOption.AGGREGATE
        val restored = when (vmState.source) {
            context.getString(R.string.mods_curseforge) -> AddonSourceOption.CURSEFORGE
            context.getString(R.string.mods_modrinth) -> AddonSourceOption.MODRINTH
            else -> defaultSrc
        }
        mutableStateOf(restored)
    }
    var modLoaderPos by remember(section.pageId) { mutableIntStateOf(vmState.modLoaderPosition) }
    var pageOffset by remember(section.pageId) { mutableIntStateOf(vmState.pageOffset) }
    var pageCount by remember(section.pageId) { mutableIntStateOf(vmState.pageCount) }
    var results by remember(section.pageId) { mutableStateOf<List<RemoteMod>>(vmState.result ?: emptyList()) }
    var loading by remember(section.pageId) { mutableStateOf(vmState.loading) }
    var failed by remember(section.pageId) { mutableStateOf(vmState.failed) }
    var showServerUrlDialog by remember { mutableStateOf(false) }

    val categories = remember(section.pageId) {
        if (section == DownloadSection.WORLD) emptyList() else DownloadCategory.forPageId(section.pageId)
    }

    fun executeSearch(targetPage: Int = 0) {
        pageOffset = targetPage
        vmState.searchFilter = searchQuery
        vmState.userGameVersion = selectedGameVersion
        vmState.category = selectedCategory
        vmState.sortType = selectedSort
        vmState.pageOffset = targetPage
        vmState.modLoaderPosition = modLoaderPos
        vmState.source = when (selectedSource) {
            AddonSourceOption.CURSEFORGE -> context.getString(R.string.mods_curseforge)
            AddonSourceOption.MODRINTH -> context.getString(R.string.mods_modrinth)
            AddonSourceOption.AGGREGATE -> context.getString(R.string.mods_aggregate)
        }
        loading = true
        failed = false
        vmState.loading = true
        vmState.failed = false

        scope.launch {
            val outcome = runCatching {
                performAddonSearch(
                    section = section,
                    source = selectedSource,
                    gameVersion = selectedGameVersion,
                    category = selectedCategory,
                    pageOffset = targetPage,
                    query = searchQuery,
                    sortType = selectedSort,
                    modLoaderPos = modLoaderPos
                )
            }
            loading = false
            vmState.loading = false
            outcome.onSuccess { (items, totalPages) ->
                results = items
                pageCount = totalPages
                failed = false
                vmState.result = ArrayList(items)
                vmState.pageCount = totalPages
                vmState.failed = false
            }.onFailure {
                results = emptyList()
                pageCount = -1
                failed = true
                vmState.result = null
                vmState.pageCount = -1
                vmState.failed = true
            }
        }
    }

    LaunchedEffect(section.pageId) {
        if (vmState.result == null && !vmState.loading && !vmState.failed) {
            executeSearch(vmState.pageOffset)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = stringResource(R.string.search),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LauncherButton(
                text = stringResource(R.string.search),
                onClick = { executeSearch(0) },
                accentStyle = true
            )
            if (section == DownloadSection.MODPACK) {
                LauncherButton(
                    text = stringResource(R.string.modpack_install),
                    onClick = {
                        activity.fileLauncher.launchSingleSelection(
                            null,
                            null,
                            false
                        ) { files ->
                            val file = files?.firstOrNull()?.toFile(context, File(context.cacheDir, "modpack_import"))
                            if (file != null) {
                                onOpenModpackWizard(ModpackSource.LocalFile(file))
                            }
                        }
                    },
                    accentStyle = false
                )
                LauncherButton(
                    text = "URL",
                    onClick = { showServerUrlDialog = true },
                    accentStyle = false
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (section != DownloadSection.WORLD) {
                AddonFilterPopup(
                    prefix = stringResource(R.string.mods_check_updates_source),
                    currentText = selectedSource.label,
                    items = AddonSourceOption.entries.map { it.label },
                    onSelect = { idx ->
                        selectedSource = AddonSourceOption.entries[idx]
                        executeSearch(0)
                    }
                )
            }

            AddonFilterPopup(
                prefix = stringResource(R.string.version_game),
                currentText = selectedGameVersion.ifBlank { stringResource(R.string.curse_category_0) },
                items = GAME_VERSION_OPTIONS.map { it.ifBlank { stringResource(R.string.curse_category_0) } },
                onSelect = { idx ->
                    selectedGameVersion = GAME_VERSION_OPTIONS[idx]
                    executeSearch(0)
                }
            )

            if (categories.isNotEmpty()) {
                val categoryLabels = categories.map { cat ->
                    if (cat.isAll) {
                        context.getString(R.string.curse_category_0)
                    } else {
                        val localized = getLocalizedText(context, cat.key)
                        if (localized.startsWith("curse_category_") || localized.startsWith("modrinth_category_")) {
                            cat.fallback
                        } else localized
                    }
                }
                val currentCatIdx = categories.indexOf(selectedCategory).coerceAtLeast(0)
                AddonFilterPopup(
                    prefix = stringResource(R.string.mods_category),
                    currentText = categoryLabels.getOrElse(currentCatIdx) { context.getString(R.string.curse_category_0) },
                    items = categoryLabels,
                    onSelect = { idx ->
                        selectedCategory = categories[idx]
                        executeSearch(0)
                    }
                )
            }

            val sortItems = AddonSortOption.entries.map { stringResource(it.titleRes) }
            val currentSortOption = AddonSortOption.entries.firstOrNull { it.sortType == selectedSort }
                ?: AddonSortOption.POPULARITY
            AddonFilterPopup(
                prefix = stringResource(R.string.mods_sort),
                currentText = stringResource(currentSortOption.titleRes),
                items = sortItems,
                onSelect = { idx ->
                    selectedSort = AddonSortOption.entries[idx].sortType
                    executeSearch(0)
                }
            )

            if (section == DownloadSection.MOD) {
                AddonFilterPopup(
                    prefix = "Loader",
                    currentText = MOD_LOADER_LABELS[modLoaderPos],
                    items = MOD_LOADER_LABELS,
                    onSelect = { idx ->
                        modLoaderPos = idx
                        executeSearch(0)
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                failed -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.cardBg)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.download_failed_refresh),
                                color = colors.textSecondary,
                                fontSize = 14.sp
                            )
                            LauncherButton(
                                text = stringResource(R.string.action_refresh),
                                onClick = { executeSearch(pageOffset) },
                                accentStyle = true
                            )
                        }
                    }
                }

                results.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.cardBg)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.download_failed_empty),
                            color = colors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }

                else -> {
                    val favIds = remember(favorites) { favorites.map { it.id }.toSet() }
                    val repoType = remember(section) { section.toRepoType() }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results, key = { "${it.platformTypeName()}:${it.modID}" }) { mod ->
                            val entity = remember(mod.modID, section) { mod.toFavoriteEntity(section) }
                            AddonItemCard(
                                mod = mod,
                                repoType = repoType,
                                isFavorite = entity.id in favIds,
                                onToggleFavorite = {
                                    handleFavoriteClick(context, entity)
                                },
                                onClick = { onSelectMod(mod) }
                            )
                        }
                    }
                }
            }
        }

        if (pageCount > 1 || pageOffset > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LauncherButton(
                    text = "<",
                    onClick = { if (pageOffset > 0) executeSearch(pageOffset - 1) },
                    enabled = pageOffset > 0 && !loading,
                    accentStyle = false
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (pageCount > 0) "${pageOffset + 1} / $pageCount" else "${pageOffset + 1}",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(12.dp))
                LauncherButton(
                    text = ">",
                    onClick = { executeSearch(pageOffset + 1) },
                    enabled = !loading && (pageCount < 0 || pageOffset + 1 < pageCount),
                    accentStyle = false
                )
            }
        }
    }

    if (showServerUrlDialog) {
        ModpackUrlComposeDialog(
            onDismiss = { showServerUrlDialog = false },
            onManifestLoaded = { manifest ->
                showServerUrlDialog = false
                onOpenModpackWizard(ModpackSource.RemoteManifest(manifest))
            }
        )
    }
}

@Composable
private fun AddonFilterPopup(
    prefix: String,
    currentText: String,
    items: List<String>,
    onSelect: (Int) -> Unit
) {
    val colors = LauncherTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val selectedIdx = items.indexOf(currentText).coerceAtLeast(0)

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.cardBg)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(6.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$prefix: ",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Text(
                text = currentText,
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
        WindowListPopup(
            show = expanded,
            alignment = PopupPositionProvider.Align.Start,
            onDismissRequest = { expanded = false }
        ) {
            ListPopupColumn {
                items.forEachIndexed { idx, label ->
                    DropdownImpl(
                        text = label,
                        optionSize = items.size,
                        isSelected = idx == selectedIdx,
                        index = idx,
                        onSelectedIndexChange = {
                            expanded = false
                            onSelect(it)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModpackUrlComposeDialog(
    onDismiss: () -> Unit,
    onManifestLoaded: (ServerModpackManifest) -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    WindowDialog(
        show = true,
        title = "${stringResource(R.string.modpack)} (URL)",
        onDismissRequest = { if (!loading) onDismiss() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(
                value = url,
                onValueChange = { url = it },
                label = "URL (server-manifest.json)",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loading) {
                    CircularProgressIndicator()
                }
                LauncherButton(
                    text = stringResource(R.string.dialog_negative),
                    onClick = onDismiss,
                    enabled = !loading,
                    accentStyle = false
                )
                LauncherButton(
                    text = stringResource(R.string.dialog_positive),
                    enabled = !loading && url.isNotBlank(),
                    accentStyle = true,
                    onClick = {
                        loading = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val json = NetworkUtils.doGet(NetworkUtils.toURL(url.trim()))
                                    JsonUtils.fromNonNullJson(json, ServerModpackManifest::class.java)
                                }
                            }
                            loading = false
                            result.onSuccess(onManifestLoaded).onFailure {
                                Toast.makeText(
                                    context,
                                    it.localizedMessage ?: "Failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AddonDetailOverlay(
    mod: RemoteMod,
    section: DownloadSection,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()
    val repository = remember(mod.modID, section) { repositoryForMod(mod, section) }
    val favorites by FavoriteManager.favorites.collectAsState()
    val entity = remember(mod.modID, section) { mod.toFavoriteEntity(section) }
    val isFavorite = favorites.any { it.id == entity.id }

    var versions by remember(mod.modID) { mutableStateOf<List<RemoteMod.Version>>(emptyList()) }
    var screenshots by remember(mod.modID) { mutableStateOf<List<RemoteMod.Screenshot>>(emptyList()) }
    var loadingVersions by remember(mod.modID) { mutableStateOf(true) }
    var versionFilter by remember(mod.modID) { mutableStateOf("") }

    LaunchedEffect(mod.modID) {
        loadingVersions = true
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                mod.data.loadVersions(repository).collect(Collectors.toList())
            }.getOrDefault(emptyList())
        }
        val shots = withContext(Dispatchers.IO) {
            runCatching { mod.data.loadScreenshots(repository) }.getOrDefault(emptyList())
        }
        versions = loaded
        screenshots = shots
        loadingVersions = false
    }

    val filteredVersions = remember(versions, versionFilter) {
        if (versionFilter.isBlank()) versions
        else versions.filter { v -> v.gameVersions.any { it.equals(versionFilter, ignoreCase = true) } }
    }

    val gameVersionsInAddon = remember(versions) {
        listOf("") + versions.flatMap { it.gameVersions }.distinct().take(20)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LauncherButton(
                text = stringResource(R.string.button_back),
                onClick = onBack,
                accentStyle = false
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mod.title,
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${mod.author.orEmpty()}  •  ${mod.downloadCount.format(context)} dl",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            LauncherButton(
                text = if (isFavorite) stringResource(R.string.favorite_remove_selected) else stringResource(R.string.favorites),
                onClick = { handleFavoriteClick(context, entity) },
                accentStyle = isFavorite
            )
            if (!mod.pageUrl.isNullOrBlank()) {
                LauncherButton(
                    text = "Web",
                    onClick = { openLink(context, mod.pageUrl) },
                    accentStyle = false
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionCard(title = stringResource(R.string.modpack_description)) {
                    Text(
                        text = mod.description.orEmpty(),
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }

                if (screenshots.isNotEmpty()) {
                    SectionCard(title = "Screenshots") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(screenshots) { shot ->
                                var bmp by remember(shot.imageUrl) { mutableStateOf<Bitmap?>(null) }
                                LaunchedEffect(shot.imageUrl) {
                                    Glide.with(context).asBitmap().load(shot.imageUrl).into(object : CustomTarget<Bitmap>() {
                                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                            bmp = resource
                                        }
                                        override fun onLoadCleared(placeholder: Drawable?) {}
                                    })
                                }
                                Box(
                                    modifier = Modifier
                                        .size(width = 200.dp, height = 112.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.cardBorder.copy(alpha = 0.25f))
                                ) {
                                    bmp?.let {
                                        Image(
                                            bitmap = it.asImageBitmap(),
                                            contentDescription = shot.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.download),
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    AddonFilterPopup(
                        prefix = stringResource(R.string.version_game),
                        currentText = versionFilter.ifBlank { stringResource(R.string.curse_category_0) },
                        items = gameVersionsInAddon.map { it.ifBlank { stringResource(R.string.curse_category_0) } },
                        onSelect = { idx -> versionFilter = gameVersionsInAddon[idx] }
                    )
                }

                if (loadingVersions) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredVersions, key = { "${it.modid}:${it.name}:${it.file.filename}" }) { ver ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.cardBg)
                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        scope.launch {
                                            downloadAddonVersion(context, section, ver)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ver.name,
                                        color = colors.textPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sub = buildString {
                                        append(ver.versionType.name)
                                        if (ver.gameVersions.isNotEmpty()) {
                                            append("  •  ")
                                            append(ver.gameVersions.take(4).joinToString(", "))
                                        }
                                    }
                                    Text(
                                        text = sub,
                                        color = colors.textSecondary,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                LauncherButton(
                                    text = stringResource(R.string.download),
                                    onClick = {
                                        scope.launch {
                                            downloadAddonVersion(context, section, ver)
                                        }
                                    },
                                    accentStyle = true,
                                    height = 30.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun repositoryForSection(section: DownloadSection, source: AddonSourceOption): RemoteModRepository {
    if (section == DownloadSection.WORLD) return CurseForgeRemoteModRepository.WORLDS
    val (mr, cf) = when (section) {
        DownloadSection.MODPACK -> ModrinthRemoteModRepository.MODPACKS to CurseForgeRemoteModRepository.MODPACKS
        DownloadSection.RESOURCE_PACK -> ModrinthRemoteModRepository.RESOURCE_PACKS to CurseForgeRemoteModRepository.RESOURCE_PACKS
        DownloadSection.SHADER_PACK -> ModrinthRemoteModRepository.SHADER_PACKS to CurseForgeRemoteModRepository.SHADER_PACKS
        else -> ModrinthRemoteModRepository.MODS to CurseForgeRemoteModRepository.MODS
    }
    val type = if (section == DownloadSection.MODPACK) RemoteModRepository.Type.MODPACK else RemoteModRepository.Type.MOD
    return object : LocalizedRemoteModRepository() {
        override fun getBackedRemoteModRepository(): RemoteModRepository =
            if (source == AddonSourceOption.MODRINTH) mr else cf

        override fun getBackedRemoteModRepositorySortOrder(): RemoteModRepository.SortType =
            if (source == AddonSourceOption.MODRINTH) RemoteModRepository.SortType.NAME else RemoteModRepository.SortType.POPULARITY

        override fun getType(): RemoteModRepository.Type = type
    }
}

private fun repositoryForMod(mod: RemoteMod, section: DownloadSection): RemoteModRepository {
    val src = if (mod.isModrinthPlatform()) AddonSourceOption.MODRINTH else AddonSourceOption.CURSEFORGE
    return repositoryForSection(section, src)
}

private fun RemoteMod.isModrinthPlatform(): Boolean =
    data !is CurseAddon && pageUrl.orEmpty().contains("modrinth.com", ignoreCase = true)

private fun RemoteMod.platformTypeName(): String =
    if (isModrinthPlatform()) RemoteMod.Type.MODRINTH.name else RemoteMod.Type.CURSEFORGE.name

private fun DownloadSection.toRepoType(): RemoteModRepository.Type = when (this) {
    DownloadSection.MODPACK -> RemoteModRepository.Type.MODPACK
    DownloadSection.RESOURCE_PACK -> RemoteModRepository.Type.RESOURCE_PACK
    DownloadSection.SHADER_PACK -> RemoteModRepository.Type.SHADER_PACK
    DownloadSection.WORLD -> RemoteModRepository.Type.WORLD
    else -> RemoteModRepository.Type.MOD
}

private fun DownloadCategory.toCurseCategory(): RemoteModRepository.Category? {
    val id = cfId ?: return null
    val self = CurseAddon.Category(id, 432, fallback, "", "", "", Instant.EPOCH, false, 0, 0)
    return RemoteModRepository.Category(self, id.toString(), emptyList())
}

private fun DownloadCategory.toModrinthCategory(): RemoteModRepository.Category? {
    val id = mrId ?: return null
    return RemoteModRepository.Category(null, id, emptyList())
}

private suspend fun performAddonSearch(
    section: DownloadSection,
    source: AddonSourceOption,
    gameVersion: String,
    category: DownloadCategory,
    pageOffset: Int,
    query: String,
    sortType: RemoteModRepository.SortType,
    modLoaderPos: Int
): Pair<List<RemoteMod>, Int> = withContext(Dispatchers.IO) {
    val provider = DownloadProviders.getDownloadProvider()
    val loaders = when (modLoaderPos) {
        1 -> listOf(ModLoaderType.FORGE)
        2 -> listOf(ModLoaderType.NEO_FORGED)
        3 -> listOf(ModLoaderType.FABRIC)
        4 -> listOf(ModLoaderType.QUILT)
        else -> emptyList()
    }
    val queryText = if (loaders.isNotEmpty() && section == DownloadSection.MOD) query else query

    if (section == DownloadSection.WORLD || source != AddonSourceOption.AGGREGATE) {
        val repo = repositoryForSection(section, source)
        val catArg = if (category.isAll) null else {
            if (source == AddonSourceOption.MODRINTH) category.toModrinthCategory() else category.toCurseCategory()
        }
        val res = repo.search(provider, gameVersion, catArg, pageOffset, ADDON_PAGE_SIZE, queryText, sortType, RemoteModRepository.SortOrder.DESC)
        return@withContext res.results.collect(Collectors.toList()) to res.totalPages
    }

    val cfRepo = repositoryForSection(section, AddonSourceOption.CURSEFORGE)
    val mrRepo = repositoryForSection(section, AddonSourceOption.MODRINTH)

    val cfDeferred = if (category.isAll || category.cfId != null) {
        async {
            runCatching {
                cfRepo.search(provider, gameVersion, category.toCurseCategory(), pageOffset, ADDON_PAGE_SIZE, queryText, sortType, RemoteModRepository.SortOrder.DESC)
            }.getOrNull()
        }
    } else null

    val mrDeferred = if (category.isAll || category.mrId != null) {
        async {
            runCatching {
                mrRepo.search(provider, gameVersion, category.toModrinthCategory(), pageOffset, ADDON_PAGE_SIZE, queryText, sortType, RemoteModRepository.SortOrder.DESC)
            }.getOrNull()
        }
    } else null

    val cfRes = cfDeferred?.await()
    val mrRes = mrDeferred?.await()
    if (cfRes == null && mrRes == null) throw IOException("Search failed")

    val cfList = cfRes?.results?.collect(Collectors.toList()).orEmpty()
    val mrList = mrRes?.results?.collect(Collectors.toList()).orEmpty()
    val merged = ArrayList<RemoteMod>(cfList.size + mrList.size)
    val seen = HashSet<String>()
    for (i in 0 until maxOf(cfList.size, mrList.size)) {
        if (i < cfList.size) addMergedMod(merged, seen, cfList[i])
        if (i < mrList.size) addMergedMod(merged, seen, mrList[i])
    }
    val totalPages = maxOf(cfRes?.totalPages ?: 0, mrRes?.totalPages ?: 0)
    merged to totalPages
}

private fun addMergedMod(merged: MutableList<RemoteMod>, seen: MutableSet<String>, mod: RemoteMod) {
    val key = mod.slug.ifBlank { mod.title }.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
    if (seen.add(key)) {
        merged.add(mod)
    }
}

private fun RemoteMod.toFavoriteEntity(section: DownloadSection): DownloadFavoriteEntity {
    val repoType = section.toRepoType()
    val srcName = platformTypeName()
    return DownloadFavoriteEntity(
        id = "$srcName:$modID",
        source = srcName,
        type = repoType.name,
        modId = modID,
        slug = slug,
        title = title,
        description = description,
        iconUrl = iconUrl.orEmpty(),
        pageUrl = pageUrl.orEmpty(),
        downloadCount = downloadCount,
        categories = categories,
        favoriteTime = System.currentTimeMillis()
    )
}

private fun downloadAddonVersion(
    context: Context,
    section: DownloadSection,
    version: RemoteMod.Version
) {
    val profile = Profiles.getSelectedProfile()
    if (section == DownloadSection.MODPACK) {
        Versions.downloadModpackImpl(context, profile, version)
        return
    }
    val subdir = when (section) {
        DownloadSection.MOD -> "mods"
        DownloadSection.RESOURCE_PACK -> "resourcepacks"
        DownloadSection.SHADER_PACK -> "shaderpacks"
        DownloadSection.WORLD -> "saves"
        else -> "mods"
    }
    val selectedVer = profile.selectedVersion
    val runDir: Path = if (!selectedVer.isNullOrBlank() && profile.repository.hasVersion(selectedVer)) {
        profile.repository.getRunDirectory(selectedVer).toPath()
    } else {
        profile.repository.baseDirectory.toPath()
    }
    val destDir = runDir.resolve(subdir)
    val filename = version.file.filename
    val dest = destDir.resolve(filename).toFile()
    val fileTask = FileDownloadTask(NetworkUtils.toURL(version.file.url), dest, version.file.integrityCheck)
    fileTask.name = filename
    val task = Task.composeAsync<Void> { fileTask }
    val executor = task.whenComplete(Schedulers.androidUIThread()) { ex ->
        if (ex == null) {
            Toast.makeText(context, context.getString(R.string.install_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, ex.localizedMessage ?: "Download failed", Toast.LENGTH_SHORT).show()
        }
    }.executor()
    DownloadManager.submit(filename, task, executor)
    executor.start()
    Toast.makeText(context, "${context.getString(R.string.download)}: $filename", Toast.LENGTH_SHORT).show()
}
