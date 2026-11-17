package top.nkbe.pclf.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tungsten.fcl.R
import com.tungsten.fclcore.mod.RemoteMod
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.SidebarNavItem
import java.io.File

enum class DownloadSection(
    val pageId: Int,
    @StringRes val titleRes: Int
) {
    GAME(15010, R.string.version_game),
    MODPACK(15011, R.string.modpack),
    MOD(15012, R.string.mods),
    RESOURCE_PACK(15013, R.string.resourcepack),
    WORLD(15014, R.string.world),
    SHADER_PACK(15015, R.string.shaderpack),
    FAVORITE(15016, R.string.favorites);

    companion object {
        fun fromPageId(id: Int): DownloadSection =
            entries.firstOrNull { it.pageId == id } ?: GAME
    }
}

@Composable
fun DownloadScreen(
    initialSection: DownloadSection,
    sectionRequestSeq: Int,
    modpackWizardFile: File?,
    modpackWizardSeq: Int,
    onRegisterBackHandler: ((() -> Boolean)?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors

    var section by remember { mutableStateOf(initialSection) }
    var selectedGameVersion by remember { mutableStateOf<String?>(null) }
    var pickingLibraryId by remember { mutableStateOf<String?>(null) }
    var activeModpackWizardSource by remember { mutableStateOf<ModpackSource?>(null) }
    var activeAddonDetail by remember { mutableStateOf<Pair<RemoteMod, DownloadSection>?>(null) }

    LaunchedEffect(sectionRequestSeq) {
        section = initialSection
        selectedGameVersion = null
        pickingLibraryId = null
        activeModpackWizardSource = null
        activeAddonDetail = null
    }

    LaunchedEffect(modpackWizardSeq) {
        if (modpackWizardSeq > 0 && modpackWizardFile != null) {
            section = DownloadSection.MODPACK
            selectedGameVersion = null
            pickingLibraryId = null
            activeAddonDetail = null
            activeModpackWizardSource = ModpackSource.LocalFile(modpackWizardFile)
        }
    }

    DisposableEffect(Unit) {
        onRegisterBackHandler {
            if (activeModpackWizardSource != null) {
                activeModpackWizardSource = null
                true
            } else if (activeAddonDetail != null) {
                activeAddonDetail = null
                true
            } else if (section == DownloadSection.GAME && pickingLibraryId != null) {
                pickingLibraryId = null
                true
            } else if (section == DownloadSection.GAME && selectedGameVersion != null) {
                selectedGameVersion = null
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
                .width(210.dp)
                .fillMaxHeight()
                .background(colors.sidebarBg)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Left))
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DownloadSection.entries.forEach { s ->
                SidebarNavItem(
                    label = stringResource(s.titleRes),
                    selected = section == s,
                    onClick = {
                        selectedGameVersion = null
                        pickingLibraryId = null
                        activeModpackWizardSource = null
                        activeAddonDetail = null
                        section = s
                    }
                )
            }
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

        val wizardSource = activeModpackWizardSource
        val detail = activeAddonDetail
        if (wizardSource != null) {
            ModpackInstallWizardOverlay(
                source = wizardSource,
                onBack = { activeModpackWizardSource = null },
                onInstallSuccess = {
                    activeModpackWizardSource = null
                    selectedGameVersion = null
                    pickingLibraryId = null
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
            )
        } else if (detail != null) {
            AddonDetailOverlay(
                mod = detail.first,
                section = detail.second,
                onBack = { activeAddonDetail = null },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
            ) {
                when (section) {
                    DownloadSection.GAME -> {
                        val gv = selectedGameVersion
                        if (gv != null) {
                            GameInstallConfigOverlay(
                                gameVersion = gv,
                                pickingLibraryId = pickingLibraryId,
                                onPickingLibraryIdChange = { pickingLibraryId = it },
                                onBack = {
                                    selectedGameVersion = null
                                    pickingLibraryId = null
                                },
                                onInstallComplete = {
                                    selectedGameVersion = null
                                    pickingLibraryId = null
                                }
                            )
                        } else {
                            GameVersionInstallTab(
                                onSelectGameVersion = { ver ->
                                    selectedGameVersion = ver
                                    pickingLibraryId = null
                                }
                            )
                        }
                    }

                    DownloadSection.FAVORITE -> {
                        DownloadFavoriteScreen(
                            onOpenDetail = { mod, targetSection ->
                                activeAddonDetail = mod to targetSection
                            }
                        )
                    }

                    else -> {
                        AddonSearchScreen(
                            section = section,
                            onSelectMod = { mod -> activeAddonDetail = mod to section },
                            onOpenModpackWizard = { source -> activeModpackWizardSource = source }
                        )
                    }
                }
            }
        }
    }
}
