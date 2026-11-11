package top.nkbe.pclf.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialog
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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.steveice10.opennbt.tag.builtin.ByteTag
import com.github.steveice10.opennbt.tag.builtin.CompoundTag
import com.github.steveice10.opennbt.tag.builtin.DoubleTag
import com.github.steveice10.opennbt.tag.builtin.FloatTag
import com.github.steveice10.opennbt.tag.builtin.IntArrayTag
import com.github.steveice10.opennbt.tag.builtin.IntTag
import com.github.steveice10.opennbt.tag.builtin.ListTag
import com.github.steveice10.opennbt.tag.builtin.LongTag
import com.github.steveice10.opennbt.tag.builtin.StringTag
import com.github.steveice10.opennbt.tag.builtin.Tag
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.graphics.Color
import com.mio.data.ModsChanged
import com.mio.download.DownloadManager
import com.mio.util.getLocalizedText
import com.mio.util.showErrorDialog
import com.tungsten.fcl.FCLApp
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.setting.Accounts
import com.tungsten.fcl.setting.ConfigHolder
import com.tungsten.fcl.setting.Profile
import com.tungsten.fcl.ui.TaskDialog
import com.tungsten.fcl.util.TaskCancellationAction
import com.tungsten.fclcore.game.World
import com.tungsten.fclcore.mod.Datapack
import com.tungsten.fclcore.mod.LocalModFile
import com.tungsten.fclcore.mod.ModAdviser
import com.tungsten.fclcore.mod.ModManager
import com.tungsten.fclcore.mod.ModpackExportInfo
import com.tungsten.fclcore.mod.RemoteMod
import com.tungsten.fclcore.mod.curse.CurseForgeModpackExportTask
import com.tungsten.fclcore.mod.mcbbs.McbbsModpackExportTask
import com.tungsten.fclcore.mod.mcbbs.McbbsModpackManifest
import com.tungsten.fclcore.mod.modrinth.ModrinthModpackExportTask
import com.tungsten.fclcore.mod.server.ServerModpackExportTask
import com.tungsten.fclcore.task.FileDownloadTask
import com.tungsten.fclcore.task.Schedulers
import com.tungsten.fclcore.task.Task
import com.tungsten.fclcore.task.TaskListener
import com.tungsten.fclcore.task.TaskExecutor
import com.tungsten.fclcore.util.Lang
import com.tungsten.fclcore.util.Logging
import com.tungsten.fclcore.util.Pair
import com.tungsten.fclcore.util.StringUtils
import com.tungsten.fclcore.util.io.CSVTable
import com.tungsten.fclcore.util.io.FileUtils
import com.tungsten.fclcore.util.platform.MemoryUtils
import com.tungsten.fclcore.util.platform.OperatingSystem
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog
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
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.window.WindowDialog
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.logging.Level
import kotlin.math.roundToInt

/** Pure-Compose full-pane subpages launched from VersionManage sections. */
sealed interface ManageOverlay {
    data class ModUpdates(
        val modManager: ModManager,
        val updates: List<LocalModFile.ModUpdate>
    ) : ManageOverlay

    data class WorldInfo(val world: World) : ManageOverlay

    data class Datapacks(val world: World) : ManageOverlay

    data object ModpackExport : ManageOverlay
}

@Composable
fun ManageOverlayHost(
    overlay: ManageOverlay,
    profile: Profile,
    version: String,
    modpackStep: Int,
    onModpackStepChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    when (overlay) {
        is ManageOverlay.ModUpdates -> ModUpdatesOverlay(
            modManager = overlay.modManager,
            updates = overlay.updates,
            onDismiss = onDismiss
        )

        is ManageOverlay.WorldInfo -> WorldInfoOverlay(
            world = overlay.world,
            onDismiss = onDismiss
        )

        is ManageOverlay.Datapacks -> DatapacksOverlay(
            world = overlay.world,
            onDismiss = onDismiss
        )

        ManageOverlay.ModpackExport -> ModpackExportOverlay(
            profile = profile,
            version = version,
            step = modpackStep,
            onStepChange = onModpackStepChange,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun PlainSectionCard(
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
private fun OverlayHeaderCard(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    trailingActions: @Composable RowScope.() -> Unit = {}
) {
    val colors = LauncherTheme.colors
    PlainSectionCard(modifier = Modifier.fillMaxWidth()) {
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
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = trailingActions
            )
        }
    }
}

@Composable
private fun TriStateCheckBadge(
    state: ToggleableState,
    onClick: (() -> Unit)? = null
) {
    val colors = LauncherTheme.colors
    val filled = state != ToggleableState.Off
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (filled) colors.accent else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (filled) colors.accent else colors.textSecondary.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp)
            )
            .let { mod -> if (onClick != null) mod.clickable(onClick = onClick) else mod }
    ) {
        when (state) {
            ToggleableState.On -> Text(
                text = "✓",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            ToggleableState.Indeterminate -> Text(
                text = "—",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            ToggleableState.Off -> Unit
        }
    }
}

private class ComposeModUpdateTask(
    modManager: ModManager,
    mods: List<Pair<LocalModFile, RemoteMod.Version>>,
    keepOldVersion: Boolean
) : Task<Void>() {
    private val dependents: Collection<Task<*>>
    val failedMods = ArrayList<LocalModFile>()

    init {
        stage = "mods.check_updates.update"
        properties["total"] = mods.size

        val list = ArrayList<Task<*>>(mods.size)
        for (mod in mods) {
            val local = mod.key
            val remote = mod.value
            val isDisabled = local.modManager.isDisabled(local.file)

            list.add(
                runAsync(Schedulers.androidUIThread()) { local.isOld = true }
                    .thenComposeAsync<Void> {
                        var fileName = remote.file.filename
                        if (isDisabled) fileName += ModManager.DISABLED_EXTENSION
                        FileDownloadTask(
                            URL(remote.file.url),
                            modManager.modsDirectory.resolve(fileName).toFile()
                        ).apply { name = remote.name }
                    }
                    .whenComplete(Schedulers.androidUIThread()) { exception ->
                        if (exception != null) {
                            local.isOld = false
                            if (isDisabled) local.disable()
                            failedMods.add(local)
                        } else if (!keepOldVersion) {
                            local.file.toFile().delete()
                        }
                    }
                    .withCounter("mods.check_updates.update")
            )
        }
        dependents = list
    }

    override fun getDependents(): Collection<Task<*>> = dependents
    override fun doPreExecute(): Boolean = true
    override fun preExecute() = notifyPropertiesChanged()
    override fun isRelyingOnDependents(): Boolean = false

    @Throws(Exception::class)
    override fun execute() {
        if (!isDependentsSucceeded) exception?.let { throw it }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModUpdatesOverlay(
    modManager: ModManager,
    updates: List<LocalModFile.ModUpdate>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors

    val selectedMap = remember(updates) {
        mutableStateMapOf<String, Boolean>().apply {
            updates.forEach { u ->
                put(u.localMod.fileName, !modManager.isDisabled(u.localMod.file))
            }
        }
    }
    val selectedCount = updates.count { selectedMap[it.localMod.fileName] == true }

    fun exportCsvList() {
        val outDir = File(Environment.getExternalStorageDirectory().absolutePath + "/FCL")
        if (!outDir.exists()) outDir.mkdirs()
        val path = File(
            outDir,
            "fcl-mod-update-list-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss"))}.csv"
        ).toPath()

        val taskDialog = TaskDialog(context, TaskCancellationAction.NORMAL)
        taskDialog.setTitle(context.getString(R.string.button_export))
        val executor = Task.runAsync {
            val csvTable = CSVTable.createEmpty()
            csvTable.set(0, 0, "Source File Name")
            csvTable.set(1, 0, "Current Version")
            csvTable.set(2, 0, "Target Version")
            csvTable.set(3, 0, "Update Source")

            updates.forEachIndexed { i, u ->
                val srcName = when (u.currentVersion.self.type) {
                    RemoteMod.Type.CURSEFORGE -> context.getString(R.string.mods_curseforge)
                    RemoteMod.Type.MODRINTH -> context.getString(R.string.mods_modrinth)
                }
                csvTable.set(0, i + 1, u.localMod.fileName)
                csvTable.set(1, i + 1, u.currentVersion.version)
                csvTable.set(2, i + 1, u.candidates.firstOrNull()?.version ?: "")
                csvTable.set(3, i + 1, srcName)
            }
            csvTable.write(Files.newOutputStream(path))
        }.whenComplete(Schedulers.androidUIThread()) { exception ->
            FCLAlertDialog.Builder(context).apply {
                setCancelable(false)
                if (exception == null) {
                    setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
                    setTitle(context.getString(R.string.message_success))
                    setMessage(path.toString())
                } else {
                    setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
                    setTitle(context.getString(R.string.message_error))
                    setMessage(exception.message)
                }
                setNegativeButton(context.getString(R.string.dialog_positive), null)
            }.create().show()
        }.executor()
        taskDialog.setExecutor(executor)
        taskDialog.show()
        executor.start()
    }

    fun startUpdate(keepOldVersion: Boolean) {
        val pairs = updates
            .filter { selectedMap[it.localMod.fileName] == true }
            .mapNotNull { u ->
                val target = u.candidates.firstOrNull() ?: return@mapNotNull null
                Pair.pair(u.localMod, target)
            }
        if (pairs.isEmpty()) return

        val task = ComposeModUpdateTask(modManager, pairs, keepOldVersion)
        val executor = task.whenComplete(Schedulers.androidUIThread()) { exception ->
            ModsChanged.notifyChanged()
            if (task.failedMods.isNotEmpty()) {
                FCLAlertDialog.Builder(context)
                    .setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
                    .setCancelable(false)
                    .setTitle(context.getString(R.string.install_failed))
                    .setMessage(
                        context.getString(R.string.mods_check_updates_failed) + "\n" +
                            task.failedMods.joinToString("\n") { it.fileName }
                    )
                    .setNegativeButton(context.getString(R.string.dialog_positive), null)
                    .create().show()
            } else if (exception == null) {
                FCLAlertDialog.Builder(context)
                    .setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
                    .setCancelable(false)
                    .setMessage(context.getString(R.string.install_success))
                    .setNegativeButton(context.getString(R.string.dialog_positive), null)
                    .create().show()
            }
        }.executor()
        DownloadManager.submit(context.getString(R.string.mods_check_updates_update), task, executor)
        executor.start()
        onDismiss()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OverlayHeaderCard(
            title = stringResource(R.string.mods_check_updates),
            subtitle = "$selectedCount / ${updates.size}",
            onBack = onDismiss
        )

        PlainSectionCard(modifier = Modifier.fillMaxWidth()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LauncherButton(
                    text = stringResource(R.string.button_select_all),
                    onClick = { updates.forEach { selectedMap[it.localMod.fileName] = true } },
                    height = 30.dp
                )
                LauncherButton(
                    text = stringResource(R.string.button_select_invert),
                    onClick = {
                        updates.forEach {
                            val key = it.localMod.fileName
                            selectedMap[key] = !(selectedMap[key] ?: false)
                        }
                    },
                    height = 30.dp
                )
                LauncherButton(
                    text = stringResource(R.string.button_export),
                    onClick = ::exportCsvList,
                    height = 30.dp
                )
                LauncherButton(
                    text = stringResource(R.string.mods_check_updates_update),
                    onClick = { startUpdate(keepOldVersion = true) },
                    enabled = selectedCount > 0,
                    height = 30.dp
                )
                LauncherButton(
                    text = stringResource(R.string.mods_check_updates_update_without),
                    accentStyle = true,
                    onClick = { startUpdate(keepOldVersion = false) },
                    enabled = selectedCount > 0,
                    height = 30.dp
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(updates, key = { it.localMod.fileName }) { update ->
                val key = update.localMod.fileName
                val checked = selectedMap[key] == true
                val targetVersion = update.candidates.firstOrNull()?.version ?: ""
                val sourceLabel = when (update.currentVersion.self.type) {
                    RemoteMod.Type.CURSEFORGE -> stringResource(R.string.mods_curseforge)
                    RemoteMod.Type.MODRINTH -> stringResource(R.string.mods_modrinth)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (checked) 1.5.dp else 1.dp,
                            color = if (checked) colors.accent else colors.cardBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedMap[key] = !checked },
                    cornerRadius = 8.dp,
                    insideMargin = PaddingValues(12.dp),
                    colors = CardDefaults.defaultColors(
                        color = if (checked) colors.accent.copy(alpha = 0.08f) else colors.cardBg,
                        contentColor = colors.textPrimary
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TriStateCheckBadge(
                            state = if (checked) ToggleableState.On else ToggleableState.Off
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = key,
                                color = colors.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "${update.currentVersion.version}  →  $targetVersion",
                                color = colors.accent,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = sourceLabel,
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.cardBorder.copy(alpha = 0.45f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}


private enum class WorldDifficultyOption {
    PEACEFUL, EASY, NORMAL, HARD;

    fun tagStringValue(): String = name.lowercase(Locale.ROOT)

    fun label(context: Context): String =
        getLocalizedText(context, "world_info_difficulty_${name.lowercase(Locale.ROOT)}")

    companion object {
        fun of(ordinalValue: Int): WorldDifficultyOption? = entries.getOrNull(ordinalValue)
        fun of(raw: String): WorldDifficultyOption? =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}

private enum class WorldGameTypeOption {
    SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR, HARDCORE;

    fun label(context: Context): String =
        getLocalizedText(context, "world_info_player_game_type_${name.lowercase(Locale.ROOT)}")

    companion object {
        fun of(rawMode: Int, hardcore: Boolean): WorldGameTypeOption? {
            if (hardcore && rawMode == 0) return HARDCORE
            return if (rawMode in 0..3) entries[rawMode] else null
        }
    }
}

private class WorldDimension(val displayName: String?) {
    @SuppressLint("DefaultLocale")
    fun formatPosition(tag: Tag?): String? {
        if (tag is ListTag && tag.size() == 3) {
            val x = tag.get<Tag>(0) as? DoubleTag ?: return null
            val y = tag.get<Tag>(1) as? DoubleTag ?: return null
            val z = tag.get<Tag>(2) as? DoubleTag ?: return null
            return formatPosition(x.value, y.value, z.value)
        }
        if (tag is IntArrayTag && tag.length() >= 3) {
            return formatPosition(tag.getValue(0), tag.getValue(1), tag.getValue(2))
        }
        return null
    }

    @SuppressLint("DefaultLocale")
    fun formatPosition(x: Int, y: Int, z: Int): String =
        if (displayName == null) "($x, $y, $z)" else "$displayName ($x, $y, $z)"

    @SuppressLint("DefaultLocale")
    fun formatPosition(x: Double, y: Double, z: Double): String =
        if (displayName == null) String.format("(%.2f, %.2f, %.2f)", x, y, z)
        else String.format("%s (%.2f, %.2f, %.2f)", displayName, x, y, z)

    companion object {
        val OVERWORLD = WorldDimension(null)
        val THE_NETHER by lazy { WorldDimension(FCLApp.getAppContext().getString(R.string.world_info_dimension_the_nether)) }
        val THE_END by lazy { WorldDimension(FCLApp.getAppContext().getString(R.string.world_info_dimension_the_end)) }

        fun of(tag: Tag?): WorldDimension? = when (tag) {
            is IntTag -> when (tag.value) {
                0 -> OVERWORLD
                -1 -> THE_NETHER
                1 -> THE_END
                else -> null
            }

            is StringTag -> when (tag.value) {
                "overworld", "minecraft:overworld" -> OVERWORLD
                "the_nether", "minecraft:the_nether" -> THE_NETHER
                "the_end", "minecraft:the_end" -> THE_END
                else -> WorldDimension(tag.value)
            }

            else -> null
        }
    }
}

private class LoadedWorldInfo(
    val dataTag: CompoundTag,
    val worldName: String,
    val gameVersion: String,
    val seedText: String,
    val lastPlayedText: String,
    val timeDaysText: String,
    val cheatByteTag: ByteTag?,
    val initialAllowCheats: Boolean,
    val generateStructuresTag: ByteTag?,
    val initialGenerateStructures: Boolean,
    val difficultyTag: Tag?,
    val initialDifficulty: WorldDifficultyOption?,
    val playerData: CompoundTag?,
    val playerLocationText: String?,
    val playerLastDeathText: String?,
    val playerSpawnText: String?,
    val playerGameTypeTag: IntTag?,
    val hardcoreTag: ByteTag?,
    val initialGameType: WorldGameTypeOption?,
    val healthTag: FloatTag?,
    val initialHealthText: String,
    val foodTag: IntTag?,
    val initialFoodText: String,
    val xpTag: IntTag?,
    val initialXpText: String
)

@Composable
private fun WorldInfoOverlay(
    world: World,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()

    var loading by remember(world) { mutableStateOf(true) }
    var loadError by remember(world) { mutableStateOf<String?>(null) }
    var loaded by remember(world) { mutableStateOf<LoadedWorldInfo?>(null) }

    LaunchedEffect(world) {
        loading = true
        loadError = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val levelData = world.levelData
                val dataTag = levelData.get<CompoundTag>("Data")
                val worldGenSettings = world.normalizedWorldGenSettingsData
                val player = world.playerData

                val timeDays = (dataTag.get<Tag>("Time") as? LongTag)?.value?.div(24000L)
                val cheatTag = (dataTag.get<Tag>("allowCommands") as? ByteTag)
                    ?.takeIf { it.value == 0.toByte() || it.value == 1.toByte() }

                val rawGenTag: Tag? = when {
                    dataTag.get<Tag>("MapFeatures") is ByteTag -> dataTag.get<ByteTag>("MapFeatures")
                    worldGenSettings != null -> worldGenSettings.get<Tag>("generate_features")
                        ?: worldGenSettings.get<Tag>("generate_structures")
                    else -> null
                }
                val genTag = (rawGenTag as? ByteTag)
                    ?.takeIf { it.value == 0.toByte() || it.value == 1.toByte() }

                var difficultyVal: WorldDifficultyOption? = null
                var difficultyTag: Tag? = null
                val preDiffTag = dataTag.get<Tag>("Difficulty")
                if (preDiffTag is ByteTag) {
                    val found = WorldDifficultyOption.of(preDiffTag.value.toInt())
                    if (found != null) {
                        difficultyVal = found
                        difficultyTag = preDiffTag
                    }
                } else {
                    val diffSettings = dataTag.get<Tag>("difficulty_settings") as? CompoundTag
                    val strTag = diffSettings?.get<Tag>("difficulty") as? StringTag
                    if (strTag != null) {
                        val found = WorldDifficultyOption.of(strTag.value)
                        if (found != null) {
                            difficultyVal = found
                            difficultyTag = strTag
                        }
                    }
                }

                var locStr: String? = null
                var deathStr: String? = null
                var spawnStr: String? = null
                var gtTag: IntTag? = null
                var hcTag: ByteTag? = null
                var initGt: WorldGameTypeOption? = null
                var hpTag: FloatTag? = null
                var hpStr = ""
                var foodTag: IntTag? = null
                var foodStr = ""
                var xpTag: IntTag? = null
                var xpStr = ""

                if (player != null) {
                    val dim = WorldDimension.of(player.get<Tag>("Dimension"))
                    if (dim != null) locStr = dim.formatPosition(player.get<Tag>("Pos"))

                    val lastDeath = player.get<Tag>("LastDeathLocation") as? CompoundTag
                    if (lastDeath != null) {
                        val dDim = WorldDimension.of(lastDeath.get<Tag>("dimension"))
                        if (dDim != null) deathStr = dDim.formatPosition(lastDeath.get<Tag>("pos"))
                    }

                    val respawnTag = player.get<Tag>("respawn") as? CompoundTag
                    val respawnPos = respawnTag?.get<Tag>("pos") as? IntArrayTag
                    if (respawnTag != null && respawnPos != null && respawnPos.length() >= 3) {
                        val rDim = (respawnTag.get<Tag>("dimension") as? StringTag)
                            ?.let { WorldDimension.of(it) } ?: WorldDimension.OVERWORLD
                        spawnStr = rDim.formatPosition(
                            respawnPos.getValue(0),
                            respawnPos.getValue(1),
                            respawnPos.getValue(2)
                        )
                    } else {
                        val sx = player.get<Tag>("SpawnX") as? IntTag
                        val sy = player.get<Tag>("SpawnY") as? IntTag
                        val sz = player.get<Tag>("SpawnZ") as? IntTag
                        if (sx != null && sy != null && sz != null) {
                            val sDim = (player.get<Tag>("SpawnDimension") as? StringTag)
                                ?.let { WorldDimension.of(it) } ?: WorldDimension.OVERWORLD
                            spawnStr = sDim.formatPosition(sx.value, sy.value, sz.value)
                        }
                    }

                    var rawHc = dataTag.get<Tag>("hardcore") as? ByteTag
                    if (rawHc == null) {
                        val diffSettings = dataTag.get<Tag>("difficulty_settings") as? CompoundTag
                        rawHc = diffSettings?.get<Tag>("hardcore") as? ByteTag
                    }
                    val rawGt = player.get<Tag>("playerGameType") as? IntTag
                    if (rawGt != null && rawHc != null) {
                        gtTag = rawGt
                        hcTag = rawHc
                        initGt = WorldGameTypeOption.of(rawGt.value, rawHc.value == 1.toByte())
                    }

                    hpTag = player.get<Tag>("Health") as? FloatTag
                    if (hpTag != null) hpStr = DecimalFormat("#").format(hpTag.value)

                    foodTag = player.get<Tag>("foodLevel") as? IntTag
                    if (foodTag != null) foodStr = foodTag.value.toString()

                    xpTag = player.get<Tag>("XpLevel") as? IntTag
                    if (xpTag != null) xpStr = xpTag.value.toString()
                }

                LoadedWorldInfo(
                    dataTag = dataTag,
                    worldName = world.worldName,
                    gameVersion = world.gameVersion?.toNormalizedString() ?: "",
                    seedText = world.seed?.toString() ?: "",
                    lastPlayedText = LocaleUtils.formatDateTime(context, Instant.ofEpochMilli(world.lastPlayed)),
                    timeDaysText = if (timeDays != null) context.getString(R.string.world_info_time_format, timeDays) else "",
                    cheatByteTag = cheatTag,
                    initialAllowCheats = cheatTag?.value == 1.toByte(),
                    generateStructuresTag = genTag,
                    initialGenerateStructures = genTag?.value == 1.toByte(),
                    difficultyTag = difficultyTag,
                    initialDifficulty = difficultyVal,
                    playerData = player,
                    playerLocationText = locStr,
                    playerLastDeathText = deathStr,
                    playerSpawnText = spawnStr,
                    playerGameTypeTag = gtTag,
                    hardcoreTag = hcTag,
                    initialGameType = initGt,
                    healthTag = hpTag,
                    initialHealthText = hpStr,
                    foodTag = foodTag,
                    initialFoodText = foodStr,
                    xpTag = xpTag,
                    initialXpText = xpStr
                )
            }
        }
        loading = false
        result.onSuccess { loaded = it }
            .onFailure { loadError = it.message ?: it.javaClass.simpleName }
    }

    fun persistWorldData() {
        scope.launch(Dispatchers.IO) {
            runCatching { world.writeWorldData() }
                .onFailure { e ->
                    Logging.LOG.log(Level.WARNING, "Failed to save world data of ${world.worldName}", e)
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OverlayHeaderCard(
            title = stringResource(R.string.world_info_title, world.worldName),
            subtitle = world.gameVersion?.toNormalizedString()?.takeIf { it.isNotBlank() },
            onBack = onDismiss
        )

        val info = loaded
        if (loading) {
            PlainSectionCard(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        } else if (loadError != null || info == null) {
            PlainSectionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = loadError ?: stringResource(R.string.world_import_invalid),
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp
                )
            }
        } else {
            var allowCheats by remember(info) { mutableStateOf(info.initialAllowCheats) }
            var generateStructures by remember(info) { mutableStateOf(info.initialGenerateStructures) }
            var difficultyIdx by remember(info) { mutableIntStateOf(info.initialDifficulty?.ordinal ?: 0) }
            var gameTypeIdx by remember(info) { mutableIntStateOf(info.initialGameType?.ordinal ?: 0) }
            var healthText by remember(info) { mutableStateOf(info.initialHealthText) }
            var foodText by remember(info) { mutableStateOf(info.initialFoodText) }
            var xpText by remember(info) { mutableStateOf(info.initialXpText) }

            SectionCard(title = stringResource(R.string.world_info_basic)) {
                InfoKeyValueRow(stringResource(R.string.world_name), info.worldName)
                if (info.gameVersion.isNotBlank()) {
                    InfoKeyValueRow(stringResource(R.string.world_info_game_version), info.gameVersion)
                }
                if (info.seedText.isNotBlank()) {
                    InfoKeyValueRow(stringResource(R.string.world_info_random_seed), info.seedText)
                }
                InfoKeyValueRow(stringResource(R.string.world_info_last_played), info.lastPlayedText)
                if (info.timeDaysText.isNotBlank()) {
                    InfoKeyValueRow(stringResource(R.string.world_info_time), info.timeDaysText)
                }

                Spacer(Modifier.height(4.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 8.dp,
                    colors = CardDefaults.defaultColors(
                        color = colors.cardBg,
                        contentColor = colors.textPrimary
                    )
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.world_info_allow_cheats),
                        checked = allowCheats,
                        enabled = info.cheatByteTag != null,
                        onCheckedChange = { checked ->
                            allowCheats = checked
                            info.cheatByteTag?.value = if (checked) 1 else 0
                            persistWorldData()
                        }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.world_info_generate_features),
                        checked = generateStructures,
                        enabled = info.generateStructuresTag != null,
                        onCheckedChange = { checked ->
                            generateStructures = checked
                            info.generateStructuresTag?.value = if (checked) 1 else 0
                            persistWorldData()
                        }
                    )
                    val diffLabels = WorldDifficultyOption.entries.map { it.label(context) }
                    WindowDropdownPreference(
                        title = stringResource(R.string.world_info_difficulty),
                        items = diffLabels,
                        selectedIndex = difficultyIdx.coerceIn(0, diffLabels.lastIndex),
                        enabled = info.difficultyTag != null,
                        onSelectedIndexChange = { idx ->
                            difficultyIdx = idx
                            val chosen = WorldDifficultyOption.entries[idx]
                            when (val tag = info.difficultyTag) {
                                is ByteTag -> tag.value = chosen.ordinal.toByte()
                                is StringTag -> tag.value = chosen.tagStringValue()
                            }
                            persistWorldData()
                        }
                    )
                }
            }

            if (info.playerData != null) {
                SectionCard(title = stringResource(R.string.world_info_player)) {
                    info.playerLocationText?.let {
                        InfoKeyValueRow(stringResource(R.string.world_info_player_location), it)
                    }
                    info.playerLastDeathText?.let {
                        InfoKeyValueRow(stringResource(R.string.world_info_player_last_death_location), it)
                    }
                    info.playerSpawnText?.let {
                        InfoKeyValueRow(stringResource(R.string.world_info_player_spawn), it)
                    }

                    Spacer(Modifier.height(4.dp))

                    val modeLabels = WorldGameTypeOption.entries.map { it.label(context) }
                    val modeEditable = info.playerGameTypeTag != null && info.hardcoreTag != null && info.initialGameType != null
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 8.dp,
                        colors = CardDefaults.defaultColors(
                            color = colors.cardBg,
                            contentColor = colors.textPrimary
                        )
                    ) {
                        WindowDropdownPreference(
                            title = stringResource(R.string.world_info_player_game_type),
                            items = modeLabels,
                            selectedIndex = gameTypeIdx.coerceIn(0, modeLabels.lastIndex),
                            enabled = modeEditable,
                            onSelectedIndexChange = { idx ->
                                gameTypeIdx = idx
                                val chosen = WorldGameTypeOption.entries[idx]
                                val gtTag = info.playerGameTypeTag ?: return@WindowDropdownPreference
                                val hcTag = info.hardcoreTag ?: return@WindowDropdownPreference
                                if (chosen == WorldGameTypeOption.HARDCORE) {
                                    gtTag.value = 0
                                    hcTag.value = 1
                                } else {
                                    gtTag.value = chosen.ordinal
                                    hcTag.value = 0
                                }
                                persistWorldData()
                            }
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.world_info_player_health),
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            TextField(
                                value = healthText,
                                onValueChange = { raw ->
                                    healthText = raw
                                    val parsed = raw.trim().toFloatOrNull()
                                    if (parsed != null && info.healthTag != null) {
                                        info.healthTag.value = parsed
                                        persistWorldData()
                                    }
                                },
                                enabled = info.healthTag != null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.world_info_player_food_level),
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            TextField(
                                value = foodText,
                                onValueChange = { raw ->
                                    foodText = raw
                                    val parsed = raw.trim().toIntOrNull()
                                    if (parsed != null && info.foodTag != null) {
                                        info.foodTag.value = parsed
                                        persistWorldData()
                                    }
                                },
                                enabled = info.foodTag != null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.world_info_player_xp_level),
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            TextField(
                                value = xpText,
                                onValueChange = { raw ->
                                    xpText = raw
                                    val parsed = raw.trim().toIntOrNull()
                                    if (parsed != null && info.xpTag != null) {
                                        info.xpTag.value = parsed
                                        persistWorldData()
                                    }
                                },
                                enabled = info.xpTag != null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoKeyValueRow(label: String, value: String) {
    val colors = LauncherTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 12.5.sp
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DatapacksOverlay(
    world: World,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = MainActivity.getInstance()
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()

    var datapackManager by remember(world) { mutableStateOf<Datapack?>(null) }
    var packs by remember(world) { mutableStateOf<List<Datapack.Pack>>(emptyList()) }
    var loading by remember(world) { mutableStateOf(true) }
    var reloadNonce by remember(world) { mutableIntStateOf(0) }
    var query by remember(world) { mutableStateOf("") }
    var selectedIds by remember(world) { mutableStateOf<Set<String>>(emptySet()) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(world, reloadNonce) {
        loading = true
        val loadedList = withContext(Dispatchers.IO) {
            val dp = Datapack(world.file.resolve("datapacks"))
            dp.loadFromDir()
            datapackManager = dp
            dp.info.toList()
        }
        packs = loadedList
        val validIds = loadedList.map { it.id }.toSet()
        selectedIds = selectedIds.intersect(validIds)
        loading = false
    }

    val filteredPacks = remember(packs, query) {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) packs
        else packs.filter {
            it.id.lowercase(Locale.ROOT).contains(q) ||
                it.description.toString().lowercase(Locale.ROOT).contains(q)
        }
    }

    fun installZip() {
        activity.fileLauncher.launchSingleSelection(
            null,
            listOf(".zip"),
            false
        ) { files ->
            val path = files?.firstOrNull()?.path ?: return@launchSingleSelection
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        val dp = Datapack(File(path).toPath())
                        dp.loadFromZip()
                        dp.installTo(world.file)
                    }
                }
                res.onSuccess {
                    reloadNonce++
                }.onFailure { e ->
                    showErrorDialog(context, e.message ?: "Failed to import datapack")
                }
            }
        }
    }

    if (showRemoveConfirm) {
        WindowDialog(
            show = true,
            title = stringResource(R.string.button_remove),
            summary = stringResource(R.string.button_remove_confirm),
            onDismissRequest = { showRemoveConfirm = false }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    text = stringResource(R.string.dialog_negative),
                    onClick = { showRemoveConfirm = false },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = stringResource(R.string.dialog_positive),
                    onClick = {
                        showRemoveConfirm = false
                        val targets = packs.filter { it.id in selectedIds }
                        val dp = datapackManager
                        if (dp != null && targets.isNotEmpty()) {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    targets.forEach { pack ->
                                        runCatching { dp.deletePack(pack) }
                                    }
                                }
                                selectedIds = emptySet()
                                reloadNonce++
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OverlayHeaderCard(
            title = stringResource(R.string.world_datapack),
            subtitle = world.worldName,
            onBack = onDismiss,
            trailingActions = {
                LauncherButton(
                    text = stringResource(R.string.action_refresh),
                    onClick = { reloadNonce++ },
                    height = 30.dp
                )
                LauncherButton(
                    text = stringResource(R.string.datapack_add),
                    accentStyle = true,
                    onClick = ::installZip,
                    height = 30.dp
                )
            }
        )

        PlainSectionCard(modifier = Modifier.fillMaxWidth()) {
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
                LauncherButton(
                    text = stringResource(R.string.button_select_all),
                    onClick = { selectedIds = filteredPacks.map { it.id }.toSet() },
                    height = 28.dp
                )
                LauncherButton(
                    text = stringResource(R.string.button_select_invert),
                    onClick = {
                        val visible = filteredPacks.map { it.id }.toSet()
                        selectedIds = visible - selectedIds
                    },
                    height = 28.dp
                )
                LauncherButton(
                    text = stringResource(R.string.mods_enable),
                    onClick = {
                        packs.filter { it.id in selectedIds }.forEach { it.isActive = true }
                    },
                    enabled = selectedIds.isNotEmpty(),
                    height = 28.dp
                )
                LauncherButton(
                    text = stringResource(R.string.mods_disable),
                    onClick = {
                        packs.filter { it.id in selectedIds }.forEach { it.isActive = false }
                    },
                    enabled = selectedIds.isNotEmpty(),
                    height = 28.dp
                )
                LauncherButton(
                    text = stringResource(R.string.button_remove),
                    onClick = { showRemoveConfirm = true },
                    enabled = selectedIds.isNotEmpty(),
                    height = 28.dp
                )
            }
        }

        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredPacks, key = { it.id }) { pack ->
                val selected = pack.id in selectedIds
                var activeState by remember(pack, reloadNonce) { mutableStateOf(pack.isActive) }
                val desc = remember(pack) { pack.description.toString() }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) colors.accent else colors.cardBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedIds = if (selected) selectedIds - pack.id else selectedIds + pack.id
                        },
                    cornerRadius = 8.dp,
                    insideMargin = PaddingValues(12.dp),
                    colors = CardDefaults.defaultColors(
                        color = if (selected) colors.accent.copy(alpha = 0.08f) else colors.cardBg,
                        contentColor = colors.textPrimary
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TriStateCheckBadge(
                            state = if (selected) ToggleableState.On else ToggleableState.Off
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pack.id,
                                color = colors.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (desc.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    color = colors.textSecondary,
                                    fontSize = 11.5.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Switch(
                            checked = activeState,
                            onCheckedChange = { checked ->
                                activeState = checked
                                pack.isActive = checked
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun WorldExportComposeDialog(
    world: World,
    targetDirPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    var fileName by remember(world, targetDirPath) { mutableStateOf(world.worldName) }

    WindowDialog(
        show = true,
        title = stringResource(R.string.world_export_wizard, world.worldName),
        summary = "${stringResource(R.string.world_export_location)}: $targetDirPath",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.world_name),
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            TextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = stringResource(R.string.world_name_enter),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    text = stringResource(R.string.dialog_negative),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = stringResource(R.string.dialog_positive),
                    onClick = {
                        val trimmed = fileName.trim()
                        if (trimmed.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.input_not_empty), Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        if (!OperatingSystem.isNameValid(trimmed)) {
                            Toast.makeText(context, context.getString(R.string.install_new_game_malformed), Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val outFile = File(targetDirPath, "$trimmed.zip")
                        if (outFile.exists()) {
                            Toast.makeText(context, context.getString(R.string.message_file_exist), Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        onDismiss()
                        val taskDialog = TaskDialog(context, TaskCancellationAction.NORMAL)
                        taskDialog.setTitle(context.getString(R.string.world_export))
                        val executor = Task.runAsync {
                            world.export(outFile.toPath(), trimmed)
                        }.whenComplete(Schedulers.androidUIThread()) { exception ->
                            FCLAlertDialog.Builder(context).apply {
                                setCancelable(false)
                                if (exception == null) {
                                    setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
                                    setTitle(context.getString(R.string.message_success))
                                    setMessage(outFile.absolutePath)
                                } else {
                                    setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
                                    setTitle(context.getString(R.string.message_error))
                                    setMessage(exception.message)
                                }
                                setNegativeButton(context.getString(R.string.dialog_positive), null)
                            }.create().show()
                        }.executor()
                        taskDialog.setExecutor(executor)
                        taskDialog.show()
                        executor.start()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}


private const val MODPACK_TYPE_MCBBS = "mcbbs"
private const val MODPACK_TYPE_SERVER = "server"
private const val MODPACK_TYPE_CURSEFORGE = "curseforge"
private const val MODPACK_TYPE_MODRINTH = "modrinth"

private fun modpackExtensionForType(type: String): String =
    if (type == MODPACK_TYPE_MODRINTH) ".mrpack" else ".zip"

private data class ModpackTypeCardSpec(
    val typeId: String,
    val titleRes: Int,
    val descRes: Int
)

private val MODPACK_TYPE_SPECS = listOf(
    ModpackTypeCardSpec(
        MODPACK_TYPE_MCBBS,
        R.string.modpack_type_mcbbs,
        R.string.modpack_type_mcbbs_export
    ),
    ModpackTypeCardSpec(
        MODPACK_TYPE_SERVER,
        R.string.modpack_type_server,
        R.string.modpack_type_server_export
    ),
    ModpackTypeCardSpec(
        MODPACK_TYPE_CURSEFORGE,
        R.string.modpack_type_curseforge,
        R.string.modpack_type_curseforge_export
    ),
    ModpackTypeCardSpec(
        MODPACK_TYPE_MODRINTH,
        R.string.modpack_type_modrinth,
        R.string.modpack_type_modrinth_export
    )
)

private class ModpackFileTreeNode(
    val label: String,
    val basePath: String,
    val description: String?,
    val isDirectory: Boolean,
    val children: List<ModpackFileTreeNode>,
    initialSelected: Boolean,
    initialIndeterminate: Boolean = false
) {
    var selected by mutableStateOf(initialSelected)
    var indeterminate by mutableStateOf(initialIndeterminate)
    var expanded by mutableStateOf(basePath == "minecraft")
}

private fun recomputeTreeSelection(node: ModpackFileTreeNode) {
    if (node.children.isEmpty()) return
    node.children.forEach(::recomputeTreeSelection)
    val allSelected = node.children.all { it.selected && !it.indeterminate }
    val anyCheckedOrIndet = node.children.any { it.selected || it.indeterminate }
    node.selected = allSelected
    node.indeterminate = !allSelected && anyCheckedOrIndet
}

private fun setSubtreeSelection(node: ModpackFileTreeNode, selected: Boolean) {
    node.selected = selected
    node.indeterminate = false
    node.children.forEach { setSubtreeSelection(it, selected) }
}

private fun collectVisibleTreeRows(
    node: ModpackFileTreeNode,
    depth: Int,
    out: MutableList<kotlin.Pair<ModpackFileTreeNode, Int>>
) {
    out.add(node to depth)
    if (node.isDirectory && node.expanded) {
        node.children.forEach { collectVisibleTreeRows(it, depth + 1, out) }
    }
}

private fun collectNeededRelativePaths(
    node: ModpackFileTreeNode,
    out: MutableList<String>
) {
    if (node.selected || node.indeterminate) {
        if (node.basePath.length > "minecraft/".length) {
            out.add(StringUtils.substringAfter(node.basePath, "minecraft/"))
        }
        node.children.forEach { collectNeededRelativePaths(it, out) }
    }
}

private fun modpackPathDescriptions(context: Context): Map<String, String> = mapOf(
    "minecraft/fclversion.cfg" to context.getString(R.string.modpack_files_fclversion_cfg),
    "minecraft/servers.dat" to context.getString(R.string.modpack_files_servers_dat),
    "minecraft/saves" to context.getString(R.string.modpack_files_saves),
    "minecraft/mods" to context.getString(R.string.modpack_files_mods),
    "minecraft/config" to context.getString(R.string.modpack_files_config),
    "minecraft/liteconfig" to context.getString(R.string.modpack_files_liteconfig),
    "minecraft/resourcepacks" to context.getString(R.string.modpack_files_resourcepacks),
    "minecraft/resources" to context.getString(R.string.modpack_files_resourcepacks),
    "minecraft/options.txt" to context.getString(R.string.modpack_files_options_txt),
    "minecraft/optionsshaders.txt" to context.getString(R.string.modpack_files_optionsshaders_txt),
    "minecraft/mods/VoxelMods" to context.getString(R.string.modpack_files_mods_voxelmods),
    "minecraft/dumps" to context.getString(R.string.modpack_files_dumps),
    "minecraft/blueprints" to context.getString(R.string.modpack_files_blueprints),
    "minecraft/scripts" to context.getString(R.string.modpack_files_scripts)
)

private fun buildModpackTreeNode(
    file: File,
    basePath: String,
    version: String,
    descriptions: Map<String, String>
): ModpackFileTreeNode? {
    if (!file.exists()) return null

    var state = ModAdviser.ModSuggestion.SUGGESTED
    if (basePath.length > "minecraft/".length) {
        val relPath = StringUtils.substringAfter(basePath, "minecraft/") + if (file.isDirectory) "/" else ""
        state = ModAdviser.suggestMod(relPath, file.isDirectory)
        if (file.isFile && FileUtils.getNameWithoutExtension(file) == version) {
            state = ModAdviser.ModSuggestion.HIDDEN
        }
        if (file.isDirectory && file.name == "$version-natives") {
            state = ModAdviser.ModSuggestion.HIDDEN
        }
        if (state == ModAdviser.ModSuggestion.HIDDEN) return null
    }

    val childNodes = ArrayList<ModpackFileTreeNode>()
    if (file.isDirectory) {
        val childrenFiles = file.listFiles()
        if (childrenFiles != null) {
            childrenFiles
                .sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
                .forEach { child ->
                    buildModpackTreeNode(child, "$basePath/${child.name}", version, descriptions)
                        ?.let(childNodes::add)
                }
        }
        if (childNodes.isEmpty()) return null
    }

    val initSelected = state == ModAdviser.ModSuggestion.SUGGESTED
    val node = ModpackFileTreeNode(
        label = StringUtils.substringAfterLast(basePath, "/"),
        basePath = basePath,
        description = descriptions[basePath],
        isDirectory = file.isDirectory,
        children = childNodes,
        initialSelected = initSelected
    )
    if (file.isDirectory) {
        val allSel = childNodes.all { it.selected && !it.indeterminate }
        val anySel = childNodes.any { it.selected || it.indeterminate }
        node.selected = allSel
        node.indeterminate = !allSel && anySel
    }
    return node
}

@Composable
private fun ModpackExportOverlay(
    profile: Profile,
    version: String,
    step: Int,
    onStepChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = MainActivity.getInstance()
    val colors = LauncherTheme.colors

    var selectedType by remember(version) { mutableStateOf(MODPACK_TYPE_MCBBS) }
    val options: ModpackExportInfo.Options = remember(selectedType) {
        when (selectedType) {
            MODPACK_TYPE_MCBBS -> McbbsModpackExportTask.OPTION
            MODPACK_TYPE_SERVER -> ServerModpackExportTask.OPTION
            MODPACK_TYPE_CURSEFORGE -> CurseForgeModpackExportTask.OPTION
            MODPACK_TYPE_MODRINTH -> ModrinthModpackExportTask.OPTION
            else -> McbbsModpackExportTask.OPTION
        }
    }
    val extension = remember(selectedType) { modpackExtensionForType(selectedType) }

    val versionSetting = remember(profile, version) {
        profile.repository.getVersionSetting(version)
    }

    var packName by remember(version) { mutableStateOf(version) }
    var packAuthor by remember(version) {
        mutableStateOf(Accounts.getSelectedAccount()?.character ?: "")
    }
    var packVersion by remember(version) { mutableStateOf("1.0") }
    var fileApi by remember(version) { mutableStateOf("") }
    var launchArgs by remember(version) { mutableStateOf(versionSetting.minecraftArgs) }
    var jvmArgs by remember(version) { mutableStateOf(versionSetting.javaArgs) }
    var originUrl by remember(version) { mutableStateOf("") }
    var mcbbsThreadId by remember(version) { mutableStateOf("") }
    var minMemory by remember(version) { mutableIntStateOf(versionSetting.minMemory ?: 0) }
    var selectedServerIdx by remember(version) { mutableIntStateOf(0) }
    var forceUpdate by remember(version) { mutableStateOf(false) }
    var packCurseForge by remember(version) { mutableStateOf(false) }
    var description by remember(version) { mutableStateOf("") }
    var outputDir by remember(version) { mutableStateOf("") }
    var outputFileName by remember(version) { mutableStateOf(version) }

    var pendingExportInfo by remember { mutableStateOf<ModpackExportInfo?>(null) }
    var pendingOutputFile by remember { mutableStateOf<File?>(null) }

    var treeRoot by remember { mutableStateOf<ModpackFileTreeNode?>(null) }
    var treeLoading by remember { mutableStateOf(false) }

    LaunchedEffect(step, version) {
        if (step == 3 && treeRoot == null) {
            treeLoading = true
            val built = withContext(Dispatchers.IO) {
                buildModpackTreeNode(
                    file = profile.repository.getRunDirectory(version),
                    basePath = "minecraft",
                    version = version,
                    descriptions = modpackPathDescriptions(context)
                )
            }
            treeRoot = built
            treeLoading = false
        }
    }

    fun pickOutputFolder() {
        activity.fileLauncher.launchSingleSelection(null, null, true) { files ->
            val path = files?.firstOrNull()?.path ?: return@launchSingleSelection
            outputDir = path
        }
    }

    fun validateStep2AndAdvance() {
        val urlValid = if (fileApi.isNotBlank()) {
            runCatching { URL(fileApi).toURI() }.isSuccess
        } else {
            false
        }
        if (packName.isBlank() ||
            packAuthor.isBlank() ||
            packVersion.isBlank() ||
            outputFileName.isBlank() ||
            (options.isRequireFileApi && options.isValidateFileApi && fileApi.isBlank())
        ) {
            Toast.makeText(context, context.getString(R.string.input_not_empty), Toast.LENGTH_SHORT).show()
            return
        }
        if (options.isRequireFileApi && fileApi.isNotBlank() && !urlValid) {
            Toast.makeText(context, context.getString(R.string.input_url), Toast.LENGTH_SHORT).show()
            return
        }
        if (options.isRequireOrigins && mcbbsThreadId.isNotBlank() && Lang.toIntOrNull(mcbbsThreadId) == null) {
            Toast.makeText(context, context.getString(R.string.input_number), Toast.LENGTH_SHORT).show()
            return
        }
        if (!OperatingSystem.isNameValid(outputFileName)) {
            Toast.makeText(context, context.getString(R.string.install_new_game_malformed), Toast.LENGTH_SHORT).show()
            return
        }
        if (outputDir.isBlank()) {
            pickOutputFolder()
            return
        }
        val outFile = File(outputDir, outputFileName + extension)
        if (outFile.exists()) {
            Toast.makeText(context, context.getString(R.string.message_file_exist), Toast.LENGTH_SHORT).show()
            return
        }

        val servers = ConfigHolder.config().authlibInjectorServers
        val serverUrl = if (selectedServerIdx in 1..servers.size) {
            servers[selectedServerIdx - 1].url
        } else {
            null
        }

        val info = ModpackExportInfo().apply {
            name = packName
            this.fileApi = fileApi
            this.version = packVersion
            author = packAuthor
            this.description = description
            isPackWithLauncher = false
            url = originUrl
            isForceUpdate = forceUpdate
            isPackCurseForge = packCurseForge
            this.minMemory = minMemory
            this.launchArguments = launchArgs
            this.javaArguments = jvmArgs
            this.authlibInjectorServer = serverUrl
            if (mcbbsThreadId.isNotBlank()) {
                origins = listOf(McbbsModpackManifest.Origin("mcbbs", mcbbsThreadId.toInt()))
            }
        }
        pendingExportInfo = info
        pendingOutputFile = outFile
        onStepChange(3)
    }

    fun executeExport() {
        val root = treeRoot
        val info = pendingExportInfo ?: return
        val outFile = pendingOutputFile ?: return
        val whitelist = ArrayList<String>()
        if (root != null) {
            collectNeededRelativePaths(root, whitelist)
        }
        info.whitelist = whitelist

        val exportTask: Task<*> = when (selectedType) {
            MODPACK_TYPE_MCBBS ->
                McbbsModpackExportTask(profile.repository, version, info, outFile)
            MODPACK_TYPE_SERVER ->
                ServerModpackExportTask(profile.repository, version, info, outFile)
            MODPACK_TYPE_CURSEFORGE ->
                CurseForgeModpackExportTask(profile.repository, version, info, outFile)
            MODPACK_TYPE_MODRINTH ->
                ModrinthModpackExportTask(profile.repository, version, info, outFile)
            else -> return
        }.setSignificance(Task.TaskSignificance.MINOR)

        val taskDialog = TaskDialog(context, TaskCancellationAction(AppCompatDialog::dismiss))
        taskDialog.setTitle(context.getString(R.string.message_doing))
        val executor: TaskExecutor = exportTask.executor(object : TaskListener() {
            override fun onStop(success: Boolean, executor: TaskExecutor) {
                Schedulers.androidUIThread().execute {
                    if (success) {
                        FCLAlertDialog.Builder(context)
                            .setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
                            .setCancelable(false)
                            .setMessage(context.getString(R.string.message_success))
                            .setNegativeButton(context.getString(R.string.dialog_positive)) {
                                onDismiss()
                            }
                            .create().show()
                    } else {
                        val ex = executor.exception ?: return@execute
                        FCLAlertDialog.Builder(context)
                            .setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
                            .setCancelable(false)
                            .setTitle(context.getString(R.string.message_failed))
                            .setMessage(StringUtils.getStackTrace(ex))
                            .setNegativeButton(context.getString(R.string.dialog_positive), null)
                            .create().show()
                    }
                }
            }
        })
        taskDialog.setExecutor(executor)
        taskDialog.show()
        executor.start()
    }

    val stepTitle = when (step) {
        1 -> stringResource(R.string.modpack_export_as)
        2 -> stringResource(R.string.modpack_wizard_step_1)
        else -> stringResource(R.string.modpack_wizard_step_2)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OverlayHeaderCard(
            title = "${stringResource(R.string.modpack_wizard)} · $step/3",
            subtitle = stepTitle,
            onBack = {
                if (step > 1) onStepChange(step - 1) else onDismiss()
            },
            trailingActions = {
                if (step == 2) {
                    LauncherButton(
                        text = stringResource(R.string.button_next),
                        accentStyle = true,
                        onClick = ::validateStep2AndAdvance,
                        height = 30.dp
                    )
                } else if (step == 3 && !treeLoading) {
                    LauncherButton(
                        text = stringResource(R.string.button_export),
                        accentStyle = true,
                        onClick = ::executeExport,
                        height = 30.dp
                    )
                }
            }
        )

        when (step) {
            1 -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MODPACK_TYPE_SPECS.forEach { spec ->
                        val isCurrent = spec.typeId == selectedType
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                    color = if (isCurrent) colors.accent else colors.cardBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedType = spec.typeId
                                    treeRoot = null
                                    onStepChange(2)
                                },
                            cornerRadius = 10.dp,
                            insideMargin = PaddingValues(14.dp),
                            colors = CardDefaults.defaultColors(
                                color = if (isCurrent) colors.accent.copy(alpha = 0.08f) else colors.cardBg,
                                contentColor = colors.textPrimary
                            )
                        ) {
                            Text(
                                text = stringResource(spec.titleRes),
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = stringResource(spec.descRes),
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            2 -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlainSectionCard {
                        Text(
                            text = stringResource(R.string.modpack_wizard_step_initialization_warning),
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        InfoKeyValueRow(
                            label = stringResource(R.string.modpack_wizard_step_initialization_exported_version),
                            value = version
                        )
                    }

                    SectionCard(title = stringResource(R.string.modpack_wizard_step_1)) {
                        LabeledField(stringResource(R.string.modpack_name), packName, { packName = it })
                        LabeledField(stringResource(R.string.archive_author), packAuthor, { packAuthor = it })
                        LabeledField(stringResource(R.string.archive_version), packVersion, { packVersion = it })

                        if (options.isRequireFileApi) {
                            LabeledField(
                                label = stringResource(R.string.modpack_file_api),
                                value = fileApi,
                                onValueChange = { fileApi = it },
                                hint = if (options.isValidateFileApi) stringResource(R.string.input_hint_not_empty) else ""
                            )
                        }
                        if (options.isRequireLaunchArguments) {
                            LabeledField(stringResource(R.string.settings_advanced_minecraft_arguments), launchArgs, { launchArgs = it })
                        }
                        if (options.isRequireJavaArguments) {
                            LabeledField(stringResource(R.string.settings_advanced_jvm_args), jvmArgs, { jvmArgs = it })
                        }
                        if (options.isRequireUrl) {
                            LabeledField(stringResource(R.string.modpack_origin_url), originUrl, { originUrl = it })
                        }
                        if (options.isRequireOrigins) {
                            LabeledField(
                                label = stringResource(R.string.modpack_origin_mcbbs),
                                value = mcbbsThreadId,
                                onValueChange = { mcbbsThreadId = it },
                                hint = stringResource(R.string.modpack_origin_mcbbs_prompt)
                            )
                        }

                        if (options.isRequireMinMemory) {
                            val totalMb = remember { MemoryUtils.getTotalDeviceMemory(context) }
                            val sliderMax = (totalMb.coerceAtLeast(1024) / 256).toFloat().coerceAtLeast(4f)
                            var sliderVal by remember(minMemory) {
                                mutableFloatStateOf((minMemory / 256f).coerceIn(0f, sliderMax))
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_memory_lower_bound),
                                        color = colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${minMemory} MB",
                                        color = colors.accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Slider(
                                    value = sliderVal,
                                    onValueChange = { v ->
                                        sliderVal = v
                                        minMemory = (v.roundToInt() * 256).coerceAtLeast(0)
                                    },
                                    valueRange = 0f..sliderMax,
                                    colors = SliderDefaults.sliderColors()
                                )
                            }
                        }

                        if (options.isRequireAuthlibInjectorServer) {
                            val servers = remember { ConfigHolder.config().authlibInjectorServers }
                            val items = remember(servers) {
                                listOf("") + servers.map { it.name }
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 8.dp,
                                colors = CardDefaults.defaultColors(
                                    color = colors.cardBg,
                                    contentColor = colors.textPrimary
                                )
                            ) {
                                WindowDropdownPreference(
                                    title = stringResource(R.string.account_injector_server),
                                    items = items,
                                    selectedIndex = selectedServerIdx.coerceIn(0, items.lastIndex),
                                    onSelectedIndexChange = { selectedServerIdx = it }
                                )
                            }
                        }

                        if (options.isRequireForceUpdate) {
                            SwitchPreference(
                                title = stringResource(R.string.modpack_wizard_step_initialization_force_update),
                                checked = forceUpdate,
                                onCheckedChange = { forceUpdate = it }
                            )
                        }
                        if (options.isRequirePackCurseForge) {
                            SwitchPreference(
                                title = stringResource(R.string.modpack_export_pack_curseforge),
                                checked = packCurseForge,
                                onCheckedChange = { packCurseForge = it }
                            )
                        }

                        Text(
                            text = stringResource(R.string.modpack_description),
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        TextField(
                            value = description,
                            onValueChange = { description = it },
                            label = stringResource(R.string.modpack_desc),
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    SectionCard(title = stringResource(R.string.modpack_wizard_step_initialization_save)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = outputDir.ifBlank { stringResource(R.string.modpack_wizard_step_initialization_save) },
                                color = if (outputDir.isBlank()) colors.textSecondary else colors.textPrimary,
                                fontSize = 12.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            LauncherButton(
                                text = stringResource(R.string.modpack_wizard_step_initialization_save),
                                onClick = ::pickOutputFolder,
                                height = 30.dp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = outputFileName,
                                onValueChange = { outputFileName = it },
                                label = stringResource(R.string.world_name),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = extension,
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            else -> {
                if (treeLoading) {
                    PlainSectionCard(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    val root = treeRoot
                    var treeVersion by remember { mutableIntStateOf(0) }
                    val rows = remember(root, treeVersion) {
                        val list = ArrayList<kotlin.Pair<ModpackFileTreeNode, Int>>()
                        if (root != null) collectVisibleTreeRows(root, 0, list)
                        list
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(rows, key = { it.first.basePath }) { (node, depth) ->
                            val checkState = when {
                                node.indeterminate -> ToggleableState.Indeterminate
                                node.selected -> ToggleableState.On
                                else -> ToggleableState.Off
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = (depth * 18).dp)
                                    .border(
                                        width = 1.dp,
                                        color = colors.cardBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (node.isDirectory) {
                                            node.expanded = !node.expanded
                                        } else {
                                            setSubtreeSelection(node, !node.selected)
                                            root?.let(::recomputeTreeSelection)
                                        }
                                        treeVersion++
                                    },
                                cornerRadius = 8.dp,
                                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                                colors = CardDefaults.defaultColors(
                                    color = colors.cardBg,
                                    contentColor = colors.textPrimary
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (node.isDirectory) {
                                        Text(
                                            text = if (node.expanded) "▼" else "▶",
                                            color = colors.textSecondary,
                                            fontSize = 11.sp
                                        )
                                    } else {
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    TriStateCheckBadge(
                                        state = checkState,
                                        onClick = {
                                            val target = checkState != ToggleableState.On
                                            setSubtreeSelection(node, target)
                                            root?.let(::recomputeTreeSelection)
                                            treeVersion++
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = node.label,
                                            color = colors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (node.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!node.description.isNullOrBlank()) {
                                            Text(
                                                text = node.description,
                                                color = colors.textSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String = ""
) {
    val colors = LauncherTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(4.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = hint.ifBlank { label },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
    }
}
