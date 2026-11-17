package top.nkbe.pclf.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mio.download.DownloadManager
import com.mio.util.LayoutConverter
import com.mio.util.getMimeType
import com.mio.util.showErrorDialog
import com.mio.util.showItemSelectionDialog
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.ControllerActivity
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.control.download.ControllerCategory
import com.tungsten.fcl.control.download.ControllerIndex
import com.tungsten.fcl.control.download.ControllerVersion
import com.tungsten.fcl.setting.Controller
import com.tungsten.fcl.setting.Controllers
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.task.FileDownloadTask
import com.tungsten.fclcore.task.Schedulers
import com.tungsten.fclcore.task.Task
import com.tungsten.fclcore.util.gson.JsonUtils
import com.tungsten.fclcore.util.io.FileUtils
import com.tungsten.fclcore.util.io.NetworkUtils
import com.tungsten.fcllibrary.util.LocaleUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LauncherButton
import top.nkbe.pclf.ui.components.SectionCard
import top.nkbe.pclf.ui.components.SidebarNavItem
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
import java.util.Locale

private const val CONTROLLER_GITHUB = "https://raw.githubusercontent.com/FCL-Team/FCL-Controllers/main/"
private const val CONTROLLER_GIT_CN = "https://repo.miawa.cn/fcl_controllers/"

private enum class ControllerTab(val titleRes: Int) {
    LOCAL(R.string.pclf_more_controller),
    REPO(R.string.download)
}

@Composable
fun ControllerScreen(
    initialTabIndex: Int = 0,
    tabRequestSeq: Int = 0,
    onRegisterBackHandler: ((() -> Boolean)?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { MainActivity.getInstance() }
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()

    var currentTab by remember {
        mutableStateOf(if (initialTabIndex == 1) ControllerTab.REPO else ControllerTab.LOCAL)
    }
    var controllerRefreshTick by remember { mutableIntStateOf(0) }
    var controllers by remember(controllerRefreshTick) {
        mutableStateOf(Controllers.getControllers().toList())
    }
    var selectedController by remember {
        mutableStateOf(controllers.firstOrNull())
    }
    var editingDialogController by remember { mutableStateOf<Pair<Controller, Boolean>?>(null) }
    var copyingSourceController by remember { mutableStateOf<Controller?>(null) }
    var selectedRepoItem by remember { mutableStateOf<Pair<ControllerIndex, Int>?>(null) }

    DisposableEffect(Unit) {
        val cb = Runnable {
            controllerRefreshTick++
        }
        Controllers.addCallback(cb)
        onDispose {
            // Controllers callback list stays lightweight; keep tick synced
        }
    }

    LaunchedEffect(controllers) {
        if (selectedController == null || !controllers.contains(selectedController)) {
            selectedController = controllers.firstOrNull()
        }
    }

    LaunchedEffect(tabRequestSeq) {
        if (tabRequestSeq > 0) {
            currentTab = if (initialTabIndex == 1) ControllerTab.REPO else ControllerTab.LOCAL
            selectedRepoItem = null
        }
    }

    DisposableEffect(Unit) {
        onRegisterBackHandler {
            if (selectedRepoItem != null) {
                selectedRepoItem = null
                true
            } else if (currentTab == ControllerTab.REPO) {
                currentTab = ControllerTab.LOCAL
                true
            } else {
                false
            }
        }
        onDispose {
            onRegisterBackHandler(null)
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(196.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(colors.sidebarBg, colors.sidebarBg.copy(alpha = 0.88f))
                    )
                )
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
                .padding(horizontal = 10.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.pclf_more_controller),
                color = colors.textSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            ControllerTab.entries.forEach { tab ->
                SidebarNavItem(
                    label = stringResource(tab.titleRes),
                    selected = currentTab == tab,
                    onClick = {
                        selectedRepoItem = null
                        currentTab = tab
                    }
                )
            }

            if (currentTab == ControllerTab.LOCAL) {
                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = colors.cardBorder)
                Spacer(Modifier.height(6.dp))
                LauncherButton(
                    text = stringResource(R.string.control_create),
                    onClick = { editingDialogController = Controller("") to true },
                    accentStyle = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LauncherButton(
                    text = stringResource(R.string.control_import),
                    onClick = {
                        activity.fileLauncher.launchSingleSelection(
                            null,
                            arrayListOf(".json")
                        ) { files ->
                            val picked = files?.firstOrNull() ?: return@launchSingleSelection
                            scope.launch {
                                importControllerFromFile(context, activity, picked.toFile(activity, File(FCLPath.CACHE_DIR))) { imported ->
                                    Controllers.addController(imported)
                                    selectedController = imported
                                    controllerRefreshTick++
                                }
                            }
                        }
                    },
                    accentStyle = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(colors.cardBorder)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.End))
        ) {
            when {
                currentTab == ControllerTab.LOCAL -> {
                    LocalControllerContent(
                        controllers = controllers,
                        selectedController = selectedController,
                        onSelect = { selectedController = it },
                        onDelete = { controller ->
                            Controllers.removeControllers(controller)
                            if (selectedController == controller) selectedController = null
                            controllerRefreshTick++
                        },
                        onEditKeys = { controller ->
                            val intent = Intent(context, ControllerActivity::class.java)
                            val bundle = Bundle()
                            bundle.putString("controller", controller.id)
                            intent.putExtras(bundle)
                            activity.startActivity(intent)
                        },
                        onEditInfo = { controller ->
                            editingDialogController = controller to false
                        },
                        onCopy = { controller ->
                            scope.launch {
                                withContext(Dispatchers.IO) { controller.ensureAllLoaded() }
                                val cloned = controller.clone()
                                copyingSourceController = cloned
                            }
                        },
                        onShare = { controller ->
                            shareControllerMenu(context, activity, controller, scope)
                        }
                    )
                }

                selectedRepoItem != null -> {
                    ControllerRepoDetailOverlay(
                        index = selectedRepoItem!!.first,
                        source = selectedRepoItem!!.second,
                        onBack = { selectedRepoItem = null },
                        onInstalled = {
                            controllerRefreshTick++
                            selectedRepoItem = null
                            currentTab = ControllerTab.LOCAL
                        }
                    )
                }

                else -> {
                    ControllerRepoContent(
                        onSelectController = { item, source ->
                            selectedRepoItem = item to source
                        }
                    )
                }
            }
        }
    }

    editingDialogController?.let { (target, isNew) ->
        ControllerInfoComposeDialog(
            initial = target,
            titleRes = if (isNew) R.string.control_create else R.string.control_info_edit,
            onDismiss = { editingDialogController = null },
            onConfirm = { name, version, versionCode, author, description ->
                target.name = name
                target.version = version
                target.versionCode = versionCode
                target.author = author
                target.description = description
                if (isNew) {
                    Controllers.addController(target)
                    selectedController = target
                } else {
                    target.saveToDisk()
                }
                controllerRefreshTick++
                editingDialogController = null
            }
        )
    }

    copyingSourceController?.let { cloned ->
        ControllerInfoComposeDialog(
            initial = cloned,
            titleRes = R.string.control_copy,
            onDismiss = { copyingSourceController = null },
            onConfirm = { name, version, versionCode, author, description ->
                cloned.name = name
                cloned.version = version
                cloned.versionCode = versionCode
                cloned.author = author
                cloned.description = description
                Controllers.addController(cloned)
                selectedController = cloned
                controllerRefreshTick++
                copyingSourceController = null
            }
        )
    }
}

@Composable
private fun LocalControllerContent(
    controllers: List<Controller>,
    selectedController: Controller?,
    onSelect: (Controller) -> Unit,
    onDelete: (Controller) -> Unit,
    onEditKeys: (Controller) -> Unit,
    onEditInfo: (Controller) -> Unit,
    onCopy: (Controller) -> Unit,
    onShare: (Controller) -> Unit
) {
    val colors = LauncherTheme.colors
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(controllers, key = { it.id }) { controller ->
                val isSelected = controller === selectedController
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.accent.copy(alpha = 0.10f) else colors.cardBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) colors.accent else colors.cardBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelect(controller) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = controller.name,
                            color = if (isSelected) colors.accent else colors.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = controller.version.ifBlank { "1.0" },
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }
                    if (controllers.size > 1) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onDelete(controller) },
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
        }

        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedController == null) {
                SectionCard(title = stringResource(R.string.pclf_more_controller)) {
                    Text(
                        text = stringResource(R.string.version_no_version),
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                SectionCard(title = selectedController.name) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoLine(stringResource(R.string.control_info_version), selectedController.version)
                        InfoLine(stringResource(R.string.control_info_author), selectedController.author)
                        if (!selectedController.description.isNullOrBlank()) {
                            HorizontalDivider(
                                color = colors.cardBorder,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                text = selectedController.description,
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                SectionCard(title = stringResource(R.string.manage)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LauncherButton(
                            text = stringResource(R.string.control_edit),
                            onClick = { onEditKeys(selectedController) },
                            accentStyle = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LauncherButton(
                                text = stringResource(R.string.control_info_edit),
                                onClick = { onEditInfo(selectedController) },
                                accentStyle = false,
                                modifier = Modifier.weight(1f)
                            )
                            LauncherButton(
                                text = stringResource(R.string.control_copy),
                                onClick = { onCopy(selectedController) },
                                accentStyle = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        LauncherButton(
                            text = stringResource(R.string.action_share),
                            onClick = { onShare(selectedController) },
                            accentStyle = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String?) {
    val colors = LauncherTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = colors.textSecondary, fontSize = 12.5.sp)
        Text(
            text = value.orEmpty().ifBlank { "-" },
            color = colors.textPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ControllerRepoContent(
    onSelectController: (ControllerIndex, Int) -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var sourceIdx by remember { mutableIntStateOf(1) }
    var deviceIdx by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<ControllerIndex>>(emptyList()) }

    val sourceOptions = listOf("GitHub", "CN Mirror")
    val deviceOptions = listOf(
        stringResource(R.string.curse_category_0),
        stringResource(R.string.control_download_device_phone),
        stringResource(R.string.control_download_device_pad),
        stringResource(R.string.control_download_device_other)
    )

    fun loadRepo() {
        loading = true
        failed = false
        val head = if (sourceIdx == 0) CONTROLLER_GITHUB else CONTROLLER_GIT_CN
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val indexStr = NetworkUtils.doGet(NetworkUtils.toURL(head + "index.json"))
                    val type = object : TypeToken<ArrayList<ControllerIndex>>() {}.type
                    val all: ArrayList<ControllerIndex> = JsonUtils.GSON.fromJson(indexStr, type)
                    val q = query.trim().lowercase(Locale.getDefault())
                    all.filter { item ->
                        (deviceIdx == 0 || item.device.contains(deviceIdx)) &&
                            (q.isEmpty() || item.name.lowercase(Locale.getDefault()).contains(q) ||
                                item.introduction.orEmpty().lowercase(Locale.getDefault()).contains(q))
                    }
                }
            }
            loading = false
            loaded.onSuccess { list ->
                results = list
            }.onFailure {
                failed = true
                results = emptyList()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRepo()
    }

    Column(
        modifier = Modifier
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
                    value = query,
                    onValueChange = { query = it },
                    label = stringResource(R.string.search),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LauncherButton(
                text = stringResource(R.string.search),
                onClick = { loadRepo() },
                accentStyle = true
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RepoPopupChip(
                prefix = stringResource(R.string.mods_check_updates_source),
                items = sourceOptions,
                selectedIdx = sourceIdx,
                onSelect = {
                    sourceIdx = it
                    loadRepo()
                }
            )
            RepoPopupChip(
                prefix = "Device",
                items = deviceOptions,
                selectedIdx = deviceIdx,
                onSelect = {
                    deviceIdx = it
                    loadRepo()
                }
            )
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
                                onClick = { loadRepo() },
                                accentStyle = true
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.cardBg)
                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                                    .clickable { onSelectController(item, sourceIdx) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        color = colors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!item.introduction.isNullOrBlank()) {
                                        Text(
                                            text = item.introduction,
                                            color = colors.textSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 2,
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

@Composable
private fun RepoPopupChip(
    prefix: String,
    items: List<String>,
    selectedIdx: Int,
    onSelect: (Int) -> Unit
) {
    val colors = LauncherTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.cardBg)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(6.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(text = "$prefix: ", color = colors.textSecondary, fontSize = 12.sp)
            Text(
                text = items.getOrElse(selectedIdx) { "" },
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
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
private fun ControllerRepoDetailOverlay(
    index: ControllerIndex,
    source: Int,
    onBack: () -> Unit,
    onInstalled: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val head = if (source == 0) CONTROLLER_GITHUB else CONTROLLER_GIT_CN
    val repoUrl = "${head}repo_json/${index.id}/"

    var loading by remember { mutableStateOf(true) }
    var versionInfo by remember { mutableStateOf<ControllerVersion?>(null) }

    LaunchedEffect(index.id) {
        loading = true
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val str = NetworkUtils.doGet(NetworkUtils.toURL("${repoUrl}version.json"))
                JsonUtils.GSON.fromJson(str, ControllerVersion::class.java)
            }.getOrNull()
        }
        versionInfo = loaded
        loading = false
    }

    fun startDownload(versionCode: Int) {
        val downloadUrl = "${repoUrl}versions/${versionCode}.json"
        val destFile = File("${FCLPath.CONTROLLER_DIR}/${index.id}.json")
        val fileTask = FileDownloadTask(NetworkUtils.toURL(downloadUrl), destFile)
        fileTask.name = index.name
        val task = Task.composeAsync<Void> { fileTask }
        val executor = task.whenComplete(Schedulers.androidUIThread()) { ex ->
            if (ex == null) {
                runCatching {
                    val controller = GsonBuilder()
                        .setPrettyPrinting()
                        .create()
                        .fromJson(FileUtils.readText(destFile), Controller::class.java)
                    Controllers.addController(controller)
                }
                Toast.makeText(context, context.getString(R.string.install_success), Toast.LENGTH_SHORT).show()
                onInstalled()
            } else {
                Toast.makeText(context, ex.localizedMessage ?: "Download failed", Toast.LENGTH_SHORT).show()
            }
        }.executor()
        DownloadManager.submit(index.name, fileTask, executor)
        executor.start()
    }

    Column(
        modifier = Modifier
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
            Text(
                text = index.name,
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            val latest = versionInfo?.latest
            if (latest != null) {
                LauncherButton(
                    text = stringResource(R.string.download),
                    onClick = { startDownload(latest.versionCode) },
                    accentStyle = true
                )
            }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionCard(title = stringResource(R.string.control_info_description)) {
                    Text(
                        text = versionInfo?.description ?: index.introduction.orEmpty(),
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
                val latest = versionInfo?.latest
                if (latest != null) {
                    SectionCard(title = latest.versionName) {
                        Text(
                            text = versionInfo?.author.orEmpty(),
                            color = colors.textSecondary,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControllerInfoComposeDialog(
    initial: Controller,
    titleRes: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var version by remember { mutableStateOf(initial.version.ifBlank { "1.0" }) }
    var versionCode by remember { mutableStateOf(initial.versionCode.toString()) }
    var author by remember { mutableStateOf(initial.author) }
    var description by remember { mutableStateOf(initial.description) }

    WindowDialog(
        show = true,
        title = stringResource(titleRes),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.control_info_name),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = version,
                    onValueChange = { version = it },
                    label = stringResource(R.string.control_info_version),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                TextField(
                    value = versionCode,
                    onValueChange = { versionCode = it.filter { c -> c.isDigit() } },
                    label = "Code",
                    singleLine = true,
                    modifier = Modifier.weight(0.6f)
                )
            }
            TextField(
                value = author,
                onValueChange = { author = it },
                label = stringResource(R.string.control_info_author),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.control_info_description),
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                LauncherButton(
                    text = stringResource(R.string.dialog_negative),
                    onClick = onDismiss,
                    accentStyle = false
                )
                LauncherButton(
                    text = stringResource(R.string.dialog_positive),
                    enabled = name.isNotBlank(),
                    accentStyle = true,
                    onClick = {
                        onConfirm(
                            name.trim(),
                            version.trim().ifBlank { "1.0" },
                            versionCode.toIntOrNull() ?: 1,
                            author.trim(),
                            description.trim()
                        )
                    }
                )
            }
        }
    }
}

private suspend fun importControllerFromFile(
    context: Context,
    activity: MainActivity,
    file: File,
    onSuccess: (Controller) -> Unit
) {
    val result = withContext(Dispatchers.IO) {
        runCatching {
            var content = FileUtils.readText(file)
            var convertedFromZl2 = false
            if (LayoutConverter.isZl2Layout(content)) {
                val input = File(FCLPath.CACHE_DIR, "import_zl2_${System.currentTimeMillis()}.json")
                val output = File(FCLPath.CACHE_DIR, "import_zl2_converted.json")
                try {
                    input.writeText(content, Charsets.UTF_8)
                    val err = LayoutConverter.convertZl2ToFcl(input, output)
                    if (err != null) throw IllegalStateException(err)
                    content = FileUtils.readText(output)
                    convertedFromZl2 = true
                } finally {
                    input.delete()
                    output.delete()
                }
            }
            val controller = GsonBuilder().setPrettyPrinting().create()
                .fromJson(content, Controller::class.java)
            controller to convertedFromZl2
        }
    }
    result.onSuccess { (controller, converted) ->
        if (controller.name == "Error") {
            Toast.makeText(context, context.getString(R.string.control_import_failed), Toast.LENGTH_SHORT).show()
        } else {
            onSuccess(controller)
            if (converted) {
                Toast.makeText(context, context.getString(R.string.control_import_zl2_converted), Toast.LENGTH_SHORT).show()
            }
        }
    }.onFailure { ex ->
        showErrorDialog(context, "${context.getString(R.string.control_import_failed)}\n${ex.message}")
    }
}

private fun shareControllerMenu(
    context: Context,
    activity: MainActivity,
    controller: Controller,
    scope: kotlinx.coroutines.CoroutineScope
) {
    showItemSelectionDialog(
        context,
        context.getString(R.string.control_share_choose),
        listOf(
            context.getString(R.string.control_share_direct),
            context.getString(R.string.control_share_zl2)
        ),
        true
    ) { _: Int, selected: String ->
        if (selected == context.getString(R.string.control_share_direct)) {
            val file = File(FCLPath.CONTROLLER_DIR, controller.fileName)
            shareControllerFile(context, activity, file, R.string.control_share, getMimeType(file.absolutePath))
        } else if (selected == context.getString(R.string.control_share_zl2)) {
            if (!LayoutConverter.isSupported()) {
                Toast.makeText(context, R.string.control_convert_unsupported, Toast.LENGTH_LONG).show()
                return@showItemSelectionDialog
            }
            scope.launch {
                val input = File(FCLPath.CONTROLLER_DIR, controller.fileName)
                val output = File(FCLPath.SHARE_DIR, "${controller.id}_zl2.json")
                val err = withContext(Dispatchers.IO) {
                    output.delete()
                    LayoutConverter.convertFclToZl2(input, output)
                }
                if (err == null) {
                    shareControllerFile(context, activity, output, R.string.control_share_zl2_title, "application/json")
                } else {
                    showErrorDialog(context, "${context.getString(R.string.control_convert_failed)}\n$err")
                }
            }
        }
    }
}

private fun shareControllerFile(
    context: Context,
    activity: MainActivity,
    file: File,
    chooserTitleRes: Int,
    mimeType: String
) {
    val intent = Intent(Intent.ACTION_SEND)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    intent.type = mimeType
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.addCategory(Intent.CATEGORY_DEFAULT)
    activity.startActivity(Intent.createChooser(intent, context.getString(chooserTitleRes)))
}
