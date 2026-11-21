package top.nkbe.pclf.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.mio.data.FavoriteManager
import com.mio.data.favorite.DownloadFavoriteEntity
import com.mio.download.DownloadManager
import com.mio.util.format
import com.tungsten.fcl.R
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fclcore.download.LibraryAnalyzer
import com.tungsten.fclcore.mod.ModLoaderType
import com.tungsten.fclcore.mod.RemoteMod
import com.tungsten.fclcore.mod.RemoteModRepository
import com.tungsten.fclcore.mod.curse.CurseForgeRemoteModRepository
import com.tungsten.fclcore.mod.modrinth.ModrinthRemoteModRepository
import com.tungsten.fclcore.task.FileDownloadTask
import com.tungsten.fclcore.task.Schedulers
import com.tungsten.fclcore.task.Task
import com.tungsten.fclcore.util.io.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.stream.Collectors
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LauncherButton
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.window.WindowDialog
import java.util.Locale

@Composable
fun DownloadFavoriteScreen(
    onOpenDetail: (RemoteMod, DownloadSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        FavoriteManager.init(context)
    }

    val favorites by FavoriteManager.favorites.collectAsState()
    val groups by FavoriteManager.groups.collectAsState()

    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showGroupDialog by remember { mutableStateOf(false) }
    var loadingEntityId by remember { mutableStateOf<String?>(null) }
    var batchResolving by remember { mutableStateOf(false) }
    var batchMatchedResult by remember { mutableStateOf<BatchModResolveResult?>(null) }

    val filteredFavorites = remember(favorites, selectedGroupId, searchQuery) {
        var list = favorites
        if (selectedGroupId != null) {
            list = list.filter { selectedGroupId in it.groups }
        }
        if (searchQuery.isNotBlank()) {
            val kw = searchQuery.trim().lowercase(Locale.getDefault())
            list = list.filter {
                it.title.lowercase(Locale.getDefault()).contains(kw) ||
                    it.slug.lowercase(Locale.getDefault()).contains(kw)
            }
        }
        list
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
                text = stringResource(R.string.favorite_group_manage),
                onClick = { showGroupDialog = true },
                accentStyle = false
            )
            LauncherButton(
                text = if (batchResolving) "..." else stringResource(R.string.favorite_download_all),
                enabled = !batchResolving,
                accentStyle = true,
                onClick = {
                    if (batchResolving) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.favorite_operation_in_progress),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@LauncherButton
                    }
                    val profile = Profiles.getSelectedProfile()
                    val selected = profile.selectedVersion
                    if (selected.isNullOrBlank() || !profile.repository.hasVersion(selected)) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.favorite_batch_no_version),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@LauncherButton
                    }
                    val mods = filteredFavorites.filter { it.type == RemoteModRepository.Type.MOD.name }
                    if (mods.isEmpty()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.favorite_batch_no_mods),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@LauncherButton
                    }
                    batchResolving = true
                    Toast.makeText(
                        context,
                        context.getString(R.string.favorite_batch_parsing),
                        Toast.LENGTH_SHORT
                    ).show()
                    scope.launch {
                        val result = resolveBatchModsForVersion(profile, selected, mods)
                        batchResolving = false
                        if (result == null) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.favorite_batch_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (result.matched.isEmpty()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.favorite_batch_none_matched),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            batchMatchedResult = result
                        }
                    }
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FavoriteGroupChip(
                label = stringResource(R.string.curse_category_0),
                selected = selectedGroupId == null,
                onClick = { selectedGroupId = null }
            )
            groups.forEach { group ->
                FavoriteGroupChip(
                    label = group.name,
                    selected = selectedGroupId == group.groupId,
                    onClick = { selectedGroupId = group.groupId }
                )
            }
        }

        if (filteredFavorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.cardBg)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.favorite_empty_title),
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.favorite_empty_guide),
                        color = colors.textSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredFavorites, key = { it.id }) { entity ->
                    FavoriteEntityCard(
                        entity = entity,
                        loading = loadingEntityId == entity.id,
                        onClick = {
                            if (loadingEntityId != null) return@FavoriteEntityCard
                            loadingEntityId = entity.id
                            scope.launch {
                                val loaded = resolveFavoriteMod(entity)
                                loadingEntityId = null
                                loaded.onSuccess { (mod, section) ->
                                    onOpenDetail(mod, section)
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        it.localizedMessage ?: "Failed to load",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onRemove = {
                            scope.launch {
                                FavoriteManager.toggle(entity)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.favorite_removed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showGroupDialog) {
        FavoriteGroupManageComposeDialog(
            onDismiss = { showGroupDialog = false }
        )
    }

    batchMatchedResult?.let { batch ->
        WindowDialog(
            show = true,
            title = stringResource(R.string.favorite_download_all),
            onDismissRequest = { batchMatchedResult = null }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (batch.skippedCount > 0) {
                    Text(
                        text = stringResource(R.string.favorite_batch_skipped_note, batch.skippedCount),
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(batch.matched, key = { it.first.id }) { (entity, ver) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.cardBg)
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = entity.title,
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = ver.file.filename,
                                color = colors.textSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    LauncherButton(
                        text = stringResource(R.string.dialog_negative),
                        onClick = { batchMatchedResult = null },
                        accentStyle = false
                    )
                    LauncherButton(
                        text = "${stringResource(R.string.favorite_batch_download)} (${batch.matched.size})",
                        accentStyle = true,
                        onClick = {
                            val modsDir: Path = batch.modsDir
                            batch.matched.forEach { (_, ver) ->
                                val filename = ver.file.filename
                                val dest = modsDir.resolve(filename).toFile()
                                val fileTask = FileDownloadTask(
                                    NetworkUtils.toURL(ver.file.url),
                                    dest,
                                    ver.file.integrityCheck
                                )
                                fileTask.name = filename
                                val task = Task.composeAsync<Void> { fileTask }
                                val executor = task.whenComplete(Schedulers.androidUIThread()) { }.executor()
                                DownloadManager.submit(filename, task, executor)
                                executor.start()
                            }
                            Toast.makeText(
                                context,
                                context.getString(R.string.favorite_batch_queued, batch.matched.size),
                                Toast.LENGTH_SHORT
                            ).show()
                            batchMatchedResult = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteGroupChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LauncherTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) colors.accent else colors.cardBg)
            .border(
                1.dp,
                if (selected) colors.accent else colors.cardBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun FavoriteEntityCard(
    entity: DownloadFavoriteEntity,
    loading: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    var iconBitmap by remember(entity.iconUrl) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(entity.iconUrl) {
        if (entity.iconUrl.isNotBlank()) {
            Glide.with(context).asBitmap().load(entity.iconUrl).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    iconBitmap = resource
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.cardBorder.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.img_grass),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = entity.title,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.accent.copy(alpha = 0.14f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = entity.source,
                        color = colors.accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Text(
                text = entity.description,
                color = colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${entity.type}  •  ${entity.downloadCount.format(context)} dl",
                color = colors.accent,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (loading) {
            CircularProgressIndicator()
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_star_filled),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FavoriteGroupManageComposeDialog(
    onDismiss: () -> Unit
) {
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()
    val groups by FavoriteManager.groups.collectAsState()
    var newGroupName by remember { mutableStateOf("") }

    WindowDialog(
        show = true,
        title = stringResource(R.string.favorite_group_manage),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    TextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = stringResource(R.string.favorite_group_new),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                LauncherButton(
                    text = stringResource(R.string.dialog_positive),
                    enabled = newGroupName.isNotBlank(),
                    accentStyle = true,
                    onClick = {
                        val name = newGroupName.trim()
                        newGroupName = ""
                        scope.launch { FavoriteManager.createGroup(name) }
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                groups.forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.cardBg)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group.name,
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    scope.launch { FavoriteManager.deleteGroup(group.groupId) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_baseline_delete_24),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colors.textSecondary),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LauncherButton(
                    text = stringResource(R.string.dialog_positive),
                    onClick = onDismiss,
                    accentStyle = true
                )
            }
        }
    }
}

private suspend fun resolveFavoriteMod(entity: DownloadFavoriteEntity): Result<Pair<RemoteMod, DownloadSection>> =
    withContext(Dispatchers.IO) {
        runCatching {
            val isModrinth = entity.source.equals("MODRINTH", ignoreCase = true)
            val section = when (entity.type) {
                RemoteModRepository.Type.MODPACK.name -> DownloadSection.MODPACK
                RemoteModRepository.Type.RESOURCE_PACK.name -> DownloadSection.RESOURCE_PACK
                RemoteModRepository.Type.SHADER_PACK.name -> DownloadSection.SHADER_PACK
                RemoteModRepository.Type.WORLD.name -> DownloadSection.WORLD
                else -> DownloadSection.MOD
            }
            val repo = when (section) {
                DownloadSection.MODPACK -> if (isModrinth) ModrinthRemoteModRepository.MODPACKS else CurseForgeRemoteModRepository.MODPACKS
                DownloadSection.RESOURCE_PACK -> if (isModrinth) ModrinthRemoteModRepository.RESOURCE_PACKS else CurseForgeRemoteModRepository.RESOURCE_PACKS
                DownloadSection.SHADER_PACK -> if (isModrinth) ModrinthRemoteModRepository.SHADER_PACKS else CurseForgeRemoteModRepository.SHADER_PACKS
                DownloadSection.WORLD -> CurseForgeRemoteModRepository.WORLDS
                else -> if (isModrinth) ModrinthRemoteModRepository.MODS else CurseForgeRemoteModRepository.MODS
            }
            val mod = repo.getModById(entity.modId)
            mod to section
        }
    }

private data class BatchModResolveResult(
    val modsDir: Path,
    val matched: List<Pair<DownloadFavoriteEntity, RemoteMod.Version>>,
    val skippedCount: Int
)

private suspend fun resolveBatchModsForVersion(
    profile: com.tungsten.fcl.setting.Profile,
    versionId: String,
    mods: List<DownloadFavoriteEntity>
): BatchModResolveResult? = withContext(Dispatchers.IO) {
    runCatching {
        val mcVersion = profile.repository.getGameVersion(versionId).orElse(null) ?: return@runCatching null
        val resolvedVersion = profile.repository.getResolvedPreservingPatchesVersion(versionId)
        val analyzer = LibraryAnalyzer.analyze(resolvedVersion, mcVersion)
        val installedLoaders = buildSet {
            if (analyzer.has(LibraryAnalyzer.LibraryType.FORGE)) add(ModLoaderType.FORGE)
            if (analyzer.has(LibraryAnalyzer.LibraryType.NEO_FORGE)) add(ModLoaderType.NEO_FORGED)
            if (analyzer.has(LibraryAnalyzer.LibraryType.FABRIC)) add(ModLoaderType.FABRIC)
            if (analyzer.has(LibraryAnalyzer.LibraryType.QUILT)) add(ModLoaderType.QUILT)
        }
        val modsDir = profile.repository.getRunDirectory(versionId).toPath().resolve("mods")

        val pairs = mods.map { entity ->
            async {
                runCatching {
                    val isModrinth = entity.source.equals("MODRINTH", ignoreCase = true)
                    val repo = if (isModrinth) ModrinthRemoteModRepository.MODS else CurseForgeRemoteModRepository.MODS
                    val mod = repo.getModById(entity.modId)
                    val versions = mod.data.loadVersions(repo).collect(Collectors.toList())
                    val picked = versions.firstOrNull { v ->
                        val gameMatch = v.gameVersions.any { it.equals(mcVersion, ignoreCase = true) }
                        val loaderMatch = installedLoaders.isEmpty() || v.loaders.isEmpty() ||
                            v.loaders.any { it in installedLoaders }
                        gameMatch && loaderMatch
                    }
                    entity to picked
                }.getOrNull()
            }
        }.awaitAll()

        val matched = pairs.mapNotNull { p ->
            val ver = p?.second ?: return@mapNotNull null
            p.first to ver
        }
        val skipped = mods.size - matched.size
        BatchModResolveResult(modsDir, matched, skipped)
    }.getOrNull()
}
