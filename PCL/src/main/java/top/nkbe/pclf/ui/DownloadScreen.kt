package top.nkbe.pclf.ui

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tungsten.fcl.R
import com.tungsten.fcl.ui.download.DownloadUI
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.SidebarNavItem

enum class DownloadSection(
    val pageId: Int,
    @StringRes val titleRes: Int
) {
    GAME(DownloadUI.PAGE_ID_DOWNLOAD_GAME, R.string.version_game),
    MODPACK(DownloadUI.PAGE_ID_DOWNLOAD_MODPACK, R.string.modpack),
    MOD(DownloadUI.PAGE_ID_DOWNLOAD_MOD, R.string.mods),
    RESOURCE_PACK(DownloadUI.PAGE_ID_DOWNLOAD_RESOURCE_PACK, R.string.resourcepack),
    WORLD(DownloadUI.PAGE_ID_DOWNLOAD_WORLD, R.string.world),
    SHADER_PACK(DownloadUI.PAGE_ID_DOWNLOAD_SHADER_PACK, R.string.shaderpack),
    FAVORITE(DownloadUI.PAGE_ID_DOWNLOAD_FAVORITE, R.string.favorites);

    companion object {
        fun fromPageId(id: Int): DownloadSection =
            entries.firstOrNull { it.pageId == id } ?: GAME
    }
}

@Composable
fun DownloadScreen(
    downloadUI: DownloadUI,
    initialSection: DownloadSection,
    sectionRequestSeq: Int,
    onRegisterBackHandler: ((() -> Boolean)?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors

    var section by remember { mutableStateOf(initialSection) }
    var tempPageCount by remember { mutableIntStateOf(if (downloadUI.canReturn()) 1 else 0) }
    var selectedGameVersion by remember { mutableStateOf<String?>(null) }
    var pickingLibraryId by remember { mutableStateOf<String?>(null) }
    var activeModpackWizardSource by remember { mutableStateOf<ModpackSource?>(null) }

    LaunchedEffect(sectionRequestSeq) {
        section = initialSection
        selectedGameVersion = null
        pickingLibraryId = null
        activeModpackWizardSource = null
    }

    DisposableEffect(downloadUI) {
        downloadUI.setTabBarVisible(false)
        downloadUI.setTempPageListener(DownloadUI.TempPageListener { count ->
            tempPageCount = count
        })
        onDispose {
            downloadUI.setTempPageListener(null)
        }
    }

    DisposableEffect(Unit) {
        onRegisterBackHandler {
            if (activeModpackWizardSource != null) {
                activeModpackWizardSource = null
                true
            } else if (downloadUI.canReturn()) {
                downloadUI.dismissCurrentTempPage()
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
                        downloadUI.dismissAllTempPages()
                        selectedGameVersion = null
                        pickingLibraryId = null
                        activeModpackWizardSource = null
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
        } else if (tempPageCount > 0 || section != DownloadSection.GAME) {
            LaunchedEffect(section, tempPageCount) {
                downloadUI.setTabBarVisible(false)
                if (tempPageCount == 0) {
                    downloadUI.showDownloadPage(section.pageId)
                }
            }
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { container ->
                    val view: View = downloadUI.contentView
                    if (view.parent !== container) {
                        (view.parent as? ViewGroup)?.removeView(view)
                        container.removeAllViews()
                        container.addView(
                            view,
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                    downloadUI.setTabBarVisible(false)
                },
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
        }
    }
}
