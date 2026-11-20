package top.nkbe.pclf.ui

import android.widget.Toast
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.ColorFilter
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
import com.mio.cache.VersionCache
import com.mio.datastore.VersionPinManager
import com.mio.ui.dialog.RendererOverrideDialog
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.setting.Profile
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fcl.ui.version.VersionListViewModel
import com.tungsten.fcl.ui.version.Versions
import com.tungsten.fclcore.util.StringUtils
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LauncherButton
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.window.WindowDialog
import java.io.File
import java.util.Locale

private enum class VersionCategoryFilter(val labelRes: Int?, val fixedLabel: String? = null) {
    ALL(R.string.curse_category_0),
    FABRIC(null, "Fabric"),
    FORGE(null, "Forge"),
    NEOFORGE(null, "NeoForge"),
    OTHER(R.string.control_download_device_other)
}

@Composable
fun VersionListScreen(
    onOpenDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { MainActivity.getInstance() }
    val colors = LauncherTheme.colors
    val viewModel = remember(activity) {
        ViewModelProvider(activity)[VersionListViewModel::class.java]
    }

    val uiState by viewModel.state.collectAsState()
    val profiles by Profiles.profilesFlow.collectAsState()
    val selectedProfile by Profiles.selectedProfile.collectAsState()
    val selectedVersion by Profiles.selectedVersion.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(VersionCategoryFilter.ALL) }
    var showAddProfileDialog by remember { mutableStateOf(false) }

    val filteredEntries = remember(uiState.entries, searchQuery, selectedCategory) {
        var list = uiState.entries
        if (searchQuery.isNotBlank()) {
            val kw = searchQuery.trim().lowercase(Locale.getDefault())
            list = list.filter { it.id.lowercase(Locale.getDefault()).contains(kw) }
        }
        when (selectedCategory) {
            VersionCategoryFilter.ALL -> list
            VersionCategoryFilter.FABRIC -> list.filter { it.libraries.hasLoaderTag("Fabric") }
            VersionCategoryFilter.FORGE -> list.filter { it.libraries.hasLoaderTag("Forge", excludeName = "NeoForge") }
            VersionCategoryFilter.NEOFORGE -> list.filter { it.libraries.hasLoaderTag("NeoForge") }
            VersionCategoryFilter.OTHER -> list.filter { entry ->
                entry.libraries.split(",").none { lib ->
                    lib.contains("Fabric") || lib.contains("Forge") || lib.contains("NeoForge")
                }
            }
        }
    }

    val listState = rememberLazyListState()
    var lastEntryIds by remember { mutableStateOf<Set<String>?>(null) }

    LaunchedEffect(uiState.entries, selectedVersion) {
        val entryIds = uiState.entries.map { it.id }.toSet()
        val entriesChanged = entryIds != lastEntryIds
        lastEntryIds = entryIds
        if (entriesChanged && selectedVersion != null) {
            val idx = filteredEntries.indexOfFirst { it.id == selectedVersion }
            if (idx >= 0) {
                val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == idx }
                if (!visible) {
                    listState.scrollToItem(idx)
                }
            }
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(232.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(colors.sidebarBg, colors.sidebarBg.copy(alpha = 0.88f))
                    )
                )
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.version),
                color = colors.textSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val activeProfiles = profiles.ifEmpty { Profiles.profiles }
                activeProfiles.forEach { profile ->
                    val isSelected = profile === selectedProfile
                    ProfileSidebarItem(
                        profile = profile,
                        selected = isSelected,
                        canDelete = activeProfiles.size > 1,
                        onSelect = {
                            if (!activity.isVersionLoading) {
                                Profiles.setSelectedProfile(profile)
                            }
                        },
                        onDelete = {
                            if (activeProfiles.size > 1) {
                                Profiles.removeProfile(profile)
                            }
                        }
                    )
                }
            }

            HorizontalDivider(color = colors.cardBorder)

            SidebarActionButton(
                iconRes = R.drawable.ic_baseline_add_24,
                label = stringResource(R.string.version_new_profile),
                onClick = { showAddProfileDialog = true }
            )
            SidebarActionButton(
                iconRes = R.drawable.ic_baseline_refresh_24,
                label = stringResource(R.string.action_refresh),
                enabled = !(uiState.loading && uiState.entries.isEmpty()),
                onClick = { viewModel.forceRefresh() }
            )
            SidebarActionButton(
                iconRes = R.drawable.ic_baseline_settings_24,
                label = stringResource(R.string.settings_fcl_renderer),
                onClick = { RendererOverrideDialog(context).show() }
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(colors.cardBorder)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VersionCategoryFilter.entries.forEach { category ->
                        val active = category == selectedCategory
                        val text = category.fixedLabel ?: stringResource(category.labelRes!!)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (active) colors.accent else colors.cardBg
                                )
                                .border(
                                    1.dp,
                                    if (active) colors.accent else colors.cardBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = text,
                                color = if (active) Color.White else colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }

                Box(modifier = Modifier.width(220.dp)) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = stringResource(R.string.search),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .clickable { searchQuery = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_baseline_close_24),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(colors.textSecondary),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        } else null
                    )
                }
            }

            when {
                uiState.loading && uiState.entries.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                filteredEntries.isEmpty() -> {
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
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.version_no_version),
                                color = colors.textSecondary,
                                fontSize = 14.sp
                            )
                            if (uiState.entries.isEmpty()) {
                                LauncherButton(
                                    text = stringResource(R.string.download),
                                    onClick = onOpenDownload,
                                    accentStyle = true
                                )
                            }
                        }
                    }
                }

                else -> {
                    val profile = uiState.profile ?: selectedProfile ?: Profiles.getSelectedProfile()
                    val pinnedList = VersionPinManager.getPinned(profile.name)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredEntries, key = { it.id }) { entry ->
                            val isSelected = entry.id == selectedVersion
                            val isPinned = entry.id in uiState.pinned
                            val pinIndex = if (isPinned) pinnedList.indexOf(entry.id) else -1
                            val hasCustomSetting = remember(profile, entry.id, uiState.entries) {
                                !profile.getVersionSetting(entry.id).isUsesGlobal
                            }

                            VersionEntryCard(
                                entry = entry,
                                selected = isSelected,
                                pinned = isPinned,
                                canMovePinUp = isPinned && pinIndex > 0,
                                canMovePinDown = isPinned && pinIndex in 0 until (pinnedList.size - 1),
                                hasCustomSetting = hasCustomSetting,
                                onSelect = {
                                    profile.selectedVersion = entry.id
                                },
                                onTogglePin = {
                                    val current = VersionPinManager.getPinned(profile.name)
                                    val updated = if (entry.id in current) {
                                        current.filterNot { it == entry.id }
                                    } else {
                                        listOf(entry.id) + current
                                    }
                                    VersionPinManager.setPinned(profile.name, updated)
                                },
                                onMovePin = { delta ->
                                    val current = VersionPinManager.getPinned(profile.name).toMutableList()
                                    val from = current.indexOf(entry.id)
                                    val to = from + delta
                                    if (from >= 0 && to in current.indices) {
                                        current.removeAt(from)
                                        current.add(to, entry.id)
                                        VersionPinManager.setPinned(profile.name, current)
                                    }
                                },
                                onOpenManage = {
                                    profile.selectedVersion = entry.id
                                    activity.navigateToManage(0)
                                },
                                onRename = {
                                    Versions.renameVersion(context, profile, entry.id)
                                },
                                onDelete = {
                                    Versions.deleteVersion(context, profile, entry.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddProfileDialog) {
        AddProfileComposeDialog(
            onDismiss = { showAddProfileDialog = false }
        )
    }
}

@Composable
private fun ProfileSidebarItem(
    profile: Profile,
    selected: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LauncherTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.accent.copy(alpha = 0.14f) else colors.cardBg.copy(alpha = 0.65f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.accent else colors.cardBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) colors.accent else Color.Transparent)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.name,
                color = if (selected) colors.accent else colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = profile.gameDir.absolutePath,
                color = colors.textSecondary,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (canDelete) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onDelete),
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

@Composable
private fun SidebarActionButton(
    iconRes: Int,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LauncherTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(7.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                if (enabled) colors.accent else colors.textSecondary.copy(alpha = 0.5f)
            ),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.5f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VersionEntryCard(
    entry: VersionCache.Entry,
    selected: Boolean,
    pinned: Boolean,
    canMovePinUp: Boolean,
    canMovePinDown: Boolean,
    hasCustomSetting: Boolean,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
    onMovePin: (Int) -> Unit,
    onOpenManage: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LauncherTheme.colors
    val iconBitmap = remember(entry.id, entry.iconKey) {
        runCatching { entry.newIcon().toBitmap().asImageBitmap() }.getOrNull()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.accent.copy(alpha = 0.08f) else colors.cardBg)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.accent else colors.cardBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(
                    width = if (selected) 5.dp else 1.5.dp,
                    color = if (selected) colors.accent else colors.textSecondary.copy(alpha = 0.6f),
                    shape = CircleShape
                )
        )

        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            Image(
                painter = painterResource(R.drawable.img_grass),
                contentDescription = null,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = entry.id,
                    color = if (selected) colors.accent else colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!entry.tag.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.accent.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = entry.tag,
                            color = colors.accent,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
            Text(
                text = "${entry.libraries}  Mods:${entry.modCount}",
                color = colors.textSecondary,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canMovePinUp) {
                VersionActionIcon(
                    iconRes = R.drawable.ic_baseline_arrow_upward_24,
                    tint = colors.textSecondary,
                    onClick = { onMovePin(-1) }
                )
            }
            if (canMovePinDown) {
                VersionActionIcon(
                    iconRes = R.drawable.ic_baseline_arrow_downward_24,
                    tint = colors.textSecondary,
                    onClick = { onMovePin(1) }
                )
            }
            VersionActionIcon(
                iconRes = if (pinned) R.drawable.ic_star_filled else R.drawable.ic_star_outline,
                tint = if (pinned) colors.accent else colors.textSecondary,
                onClick = onTogglePin
            )
            VersionActionIcon(
                iconRes = R.drawable.ic_baseline_edit_24,
                tint = colors.textSecondary,
                onClick = onRename
            )
            VersionActionIcon(
                iconRes = R.drawable.ic_baseline_settings_24,
                tint = if (hasCustomSetting) colors.accent else colors.textSecondary,
                onClick = onOpenManage
            )
            VersionActionIcon(
                iconRes = R.drawable.ic_baseline_delete_24,
                tint = colors.textSecondary,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun VersionActionIcon(
    iconRes: Int,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AddProfileComposeDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    var name by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }

    WindowDialog(
        show = true,
        title = stringResource(R.string.version_new_profile),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.profile_name),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.cardBg)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_path),
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = path.ifBlank { "-" },
                        color = colors.textPrimary,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accent.copy(alpha = 0.12f))
                        .clickable {
                            MainActivity.getInstance().fileLauncher.launchSingleSelection(
                                null,
                                null,
                                true
                            ) { files ->
                                if (!files.isNullOrEmpty()) {
                                    path = files[0].path
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_baseline_edit_24),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colors.accent),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
                    onClick = {
                        if (StringUtils.isBlank(name) || StringUtils.isBlank(path)) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.input_not_empty),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (Profiles.profiles.any { it.name == name }) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.profile_already_exist),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Profiles.addProfile(Profile(name, File(path)))
                            onDismiss()
                        }
                    },
                    accentStyle = true
                )
            }
        }
    }
}

private fun String.hasLoaderTag(name: String, excludeName: String? = null): Boolean =
    split(",").any {
        it.contains(":") && it.contains(name) && (excludeName == null || !it.contains(excludeName))
    }
