package top.nkbe.pclf.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.mio.util.format
import com.tungsten.fcl.R
import com.tungsten.fcl.addon.AddonProjectInfoRegistry
import com.tungsten.fcl.game.FCLGameRepository
import com.tungsten.fcl.game.ManuallyCreatedModpackException
import com.tungsten.fcl.game.ModpackHelper
import com.tungsten.fcl.setting.Profile
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fcl.ui.TaskDialog
import com.tungsten.fcl.ui.download.common.DownloadCategory
import com.tungsten.fcl.util.TaskCancellationAction
import com.tungsten.fclcore.mod.AddonProjectInfo
import com.tungsten.fclcore.mod.ModLoaderType
import com.tungsten.fclcore.mod.Modpack
import com.tungsten.fclcore.mod.RemoteMod
import com.tungsten.fclcore.mod.RemoteModRepository
import com.tungsten.fclcore.mod.curse.CurseAddon
import com.tungsten.fclcore.mod.server.ServerModpackManifest
import com.tungsten.fclcore.task.Schedulers
import com.tungsten.fclcore.task.Task
import com.tungsten.fclcore.task.TaskExecutor
import com.tungsten.fclcore.util.io.CompressingUtils
import com.tungsten.fclcore.util.io.FileUtils
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog
import com.tungsten.fcllibrary.util.LocaleUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

sealed class ModpackSource {
    data class LocalFile(val file: File) : ModpackSource()
    data class RemoteManifest(val manifest: ServerModpackManifest) : ModpackSource()
}

@Composable
fun ModpackInstallWizardOverlay(
    source: ModpackSource,
    profile: Profile = Profiles.getSelectedProfile(),
    updateVersion: String? = null,
    onBack: () -> Unit,
    onInstallSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var modpackTitle by remember { mutableStateOf("") }
    var modpackVersion by remember { mutableStateOf("") }
    var modpackAuthor by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf(updateVersion ?: "") }
    var isManuallyCreated by remember { mutableStateOf(false) }
    var parsedModpack by remember { mutableStateOf<Modpack?>(null) }
    var parsedCharset by remember { mutableStateOf<Charset>(StandardCharsets.UTF_8) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(source) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                when (source) {
                    is ModpackSource.LocalFile -> {
                        val file = source.file
                        val encoding = if (!ModpackHelper.isFileModpackByExtension(file)) {
                            StandardCharsets.UTF_8
                        } else {
                            CompressingUtils.findSuitableEncoding(file.toPath())
                        }
                        parsedCharset = encoding
                        try {
                            val manifest = ModpackHelper.readModpackManifest(file.toPath(), encoding)
                            parsedModpack = manifest
                            modpackTitle = manifest.name
                            modpackVersion = manifest.version
                            modpackAuthor = manifest.author
                            if (customName.isBlank()) customName = manifest.name.trim()
                        } catch (e: ManuallyCreatedModpackException) {
                            isManuallyCreated = true
                            modpackTitle = file.name
                            if (customName.isBlank()) customName = FileUtils.getNameWithoutExtension(file)
                        }
                    }
                    is ModpackSource.RemoteManifest -> {
                        val manifest = source.manifest
                        val pack = manifest.toModpack(null)
                        parsedModpack = pack
                        modpackTitle = manifest.name
                        modpackVersion = manifest.version
                        modpackAuthor = manifest.author
                        if (customName.isBlank()) customName = manifest.name.trim()
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: e.javaClass.simpleName
            }
        }
        isLoading = false
    }

    fun startInstall() {
        val name = customName.trim()
        if (name.isBlank()) {
            Toast.makeText(context, context.getString(R.string.input_not_empty), Toast.LENGTH_SHORT).show()
            return
        }
        if (updateVersion == null) {
            if (profile.repository.versionIdConflicts(name)) {
                Toast.makeText(context, context.getString(R.string.install_new_game_already_exists), Toast.LENGTH_SHORT).show()
                return
            }
            if (!FCLGameRepository.isValidVersionId(name)) {
                Toast.makeText(context, context.getString(R.string.install_new_game_malformed), Toast.LENGTH_SHORT).show()
                return
            }
        }

        val localFile = (source as? ModpackSource.LocalFile)?.file
        val remoteManifest = (source as? ModpackSource.RemoteManifest)?.manifest

        val task: Task<*>? = if (isManuallyCreated && localFile != null) {
            ModpackHelper.getInstallManuallyCreatedModpackTask(profile, localFile, name, parsedCharset)
        } else if (updateVersion != null) {
            if (localFile == null) {
                Toast.makeText(context, context.getString(R.string.modpack_unsupported), Toast.LENGTH_SHORT).show()
                return
            }
            try {
                val config = ModpackHelper.readModpackConfiguration(profile.repository.getModpackConfiguration(name))
                if (remoteManifest != null) {
                    ModpackHelper.getUpdateTask(profile, remoteManifest, parsedModpack?.encoding, name, config)
                } else {
                    ModpackHelper.getUpdateTask(profile, localFile, parsedModpack?.encoding, name, config)
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.localizedMessage ?: e.javaClass.simpleName, Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            val pack = parsedModpack
            if (pack == null) {
                Toast.makeText(context, context.getString(R.string.modpack_invalid), Toast.LENGTH_SHORT).show()
                return
            }
            if (remoteManifest != null) {
                ModpackHelper.getInstallTask(profile, remoteManifest, name, pack)
                    .thenRunAsync<Exception>(Schedulers.androidUIThread()) { profile.selectedVersion = name }
            } else {
                ModpackHelper.getInstallTask(profile, localFile, name, pack)
                    .thenRunAsync<Exception>(Schedulers.androidUIThread()) { profile.selectedVersion = name }
            }
        }

        if (task == null) return

        val dialog = TaskDialog(context, TaskCancellationAction(AppCompatDialog::dismiss))
        dialog.setTitle(context.getString(R.string.install_modpack))
        val executor = task.executor(object : com.tungsten.fclcore.task.TaskListener() {
            override fun onStop(success: Boolean, executor: TaskExecutor) {
                Schedulers.androidUIThread().execute {
                    if (success) {
                        FCLAlertDialog.Builder(context)
                            .setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
                            .setCancelable(false)
                            .setMessage(context.getString(R.string.install_success))
                            .setNegativeButton(context.getString(R.string.dialog_positive)) {
                                onInstallSuccess()
                            }
                            .create()
                            .show()
                    } else if (executor.exception != null) {
                        alertInstallFailureMessage(context, executor.exception)
                    }
                }
            }
        })
        dialog.setExecutor(executor)
        dialog.show()
        executor.start()
    }

    WindowDialog(
        show = showConfirmDialog,
        title = stringResource(R.string.modpack_download_warn_title),
        summary = stringResource(R.string.modpack_download_warn_msg),
        onDismissRequest = { showConfirmDialog = false }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                text = stringResource(R.string.dialog_negative),
                onClick = { showConfirmDialog = false },
                modifier = Modifier.weight(1f)
            )
            TextButton(
                text = stringResource(R.string.dialog_positive),
                onClick = {
                    showConfirmDialog = false
                    startInstall()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp)),
            cornerRadius = 8.dp,
            insideMargin = PaddingValues(14.dp),
            colors = CardDefaults.defaultColors(color = colors.cardBg, contentColor = colors.textPrimary)
        ) {
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
                        text = if (updateVersion != null) stringResource(R.string.version_update) else stringResource(R.string.install_modpack),
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (modpackTitle.isNotBlank()) {
                        Text(
                            text = modpackTitle,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!isLoading && errorMessage == null) {
                    LauncherButton(
                        text = stringResource(R.string.pclf_install),
                        accentStyle = true,
                        onClick = { showConfirmDialog = true },
                        height = 32.dp
                    )
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        val err = errorMessage
        if (err != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE57373), RoundedCornerShape(8.dp)),
                cornerRadius = 8.dp,
                insideMargin = PaddingValues(14.dp),
                colors = CardDefaults.defaultColors(color = colors.cardBg, contentColor = colors.textPrimary)
            ) {
                Text(
                    text = err,
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp
                )
            }
        }

        if (!isLoading && err == null) {
            SectionCard(title = stringResource(R.string.archive_name)) {
                TextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = stringResource(R.string.archive_name),
                    enabled = updateVersion == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionCard(title = stringResource(R.string.modpack_description)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = stringResource(R.string.modpack_name), color = colors.textSecondary, fontSize = 13.sp)
                        Text(text = modpackTitle.ifBlank { "-" }, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    if (modpackVersion.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = stringResource(R.string.version), color = colors.textSecondary, fontSize = 13.sp)
                            Text(text = modpackVersion, color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (modpackAuthor.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = stringResource(R.string.archive_author), color = colors.textSecondary, fontSize = 13.sp)
                            Text(text = modpackAuthor, color = colors.textPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

enum class AddonSortOption(val sortType: RemoteModRepository.SortType, val titleRes: Int) {
    POPULARITY(RemoteModRepository.SortType.POPULARITY, R.string.curse_sort_popularity),
    LAST_UPDATED(RemoteModRepository.SortType.LAST_UPDATED, R.string.curse_sort_last_updated),
    TOTAL_DOWNLOADS(RemoteModRepository.SortType.TOTAL_DOWNLOADS, R.string.curse_sort_total_downloads),
    NAME(RemoteModRepository.SortType.NAME, R.string.curse_sort_name)
}

enum class AddonSourceOption(val label: String) {
    AGGREGATE("Aggregate"),
    CURSEFORGE("CurseForge"),
    MODRINTH("Modrinth")
}

data class AddonSearchCriteria(
    val keyword: String = "",
    val category: DownloadCategory = DownloadCategory.ALL,
    val sortOption: AddonSortOption = AddonSortOption.POPULARITY,
    val sourceOption: AddonSourceOption = AddonSourceOption.AGGREGATE,
    val modLoader: ModLoaderType = ModLoaderType.UNKNOWN,
    val gameVersion: String? = null
)

@Composable
fun AddonItemCard(
    mod: RemoteMod,
    repoType: RemoteModRepository.Type,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors

    val displayName = remember(mod, repoType) {
        val proj = AddonProjectInfoRegistry.getByAddonType(repoType)?.getInfoByCurseForgeId(mod.slug)
        if (proj != null && LocaleUtils.isChinese(context)) proj.displayName() else mod.title ?: ""
    }

    var loadedBitmap by remember(mod.iconUrl) { mutableStateOf<Bitmap?>(null) }
    DisposableEffect(mod.iconUrl) {
        val url = mod.iconUrl
        if (!url.isNullOrBlank()) {
            val target = object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    loadedBitmap = resource
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    loadedBitmap = null
                }
            }
            Glide.with(context)
                .asBitmap()
                .load(url)
                .override(90, 90)
                .into(target)
            onDispose {
                Glide.with(context).clear(target)
            }
        } else {
            loadedBitmap = null
            onDispose {}
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        cornerRadius = 8.dp,
        insideMargin = PaddingValues(12.dp),
        colors = CardDefaults.defaultColors(color = colors.cardBg, contentColor = colors.textPrimary)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val bmp = loadedBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.buttonBg),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_grass),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayName,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (mod.data is CurseAddon) "CurseForge" else "Modrinth",
                        color = if (mod.data is CurseAddon) Color(0xFFF16436) else Color(0xFF1BD96A),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val desc = mod.description
                if (!desc.isNullOrBlank()) {
                    Text(
                        text = desc,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val authorName = mod.author
                    if (!authorName.isNullOrBlank()) {
                        Text(
                            text = authorName,
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "${mod.downloadCount.format(context)} dl",
                        color = colors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onToggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
