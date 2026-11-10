package com.tungsten.fcl.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mio.device.VulkanCapabilities
import com.mio.device.VulkanCheckManager
import com.mio.device.VulkanEnsureResult
import com.mio.device.normalizeMcVersion
import com.mio.device.supportFor
import com.mio.download.DownloadManager
import com.mio.manager.RendererManager
import com.mio.plugin.DriverPlugin
import com.mio.promo.QuarkPromo
import com.mio.ui.dialog.RendererSelectDialog
import com.mio.util.DisplayUtil
import com.mio.util.getLocalizedText
import com.mio.util.hasStringId
import com.mio.util.pixelAwareIcon
import com.tungsten.fcl.R
import com.tungsten.fcl.game.TexturesLoader
import com.tungsten.fcl.pclf.theme.PclfTheme
import com.tungsten.fcl.pclf.ui.LaunchUiState
import com.tungsten.fcl.pclf.ui.PclSubpage
import com.tungsten.fcl.pclf.ui.PclfMainScreen
import com.tungsten.fcl.pclf.ui.components.PclMainTab
import com.tungsten.fcl.setting.Accounts
import com.tungsten.fcl.setting.ConfigHolder
import com.tungsten.fcl.setting.Controllers
import com.tungsten.fcl.setting.Profile
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fcl.ui.UIManager
import com.tungsten.fcl.ui.download.modpack.LocalModpackPage
import com.tungsten.fcl.ui.version.VersionListViewModel
import com.tungsten.fcl.ui.version.Versions
import com.tungsten.fcl.upgrade.UpdateChecker
import com.tungsten.fcl.util.LegalAgreement
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.auth.Account
import com.tungsten.fclcore.auth.authlibinjector.AuthlibInjectorAccount
import com.tungsten.fclcore.auth.yggdrasil.TextureModel
import com.tungsten.fclcore.download.LibraryAnalyzer
import com.tungsten.fclcore.download.LibraryAnalyzer.LibraryType
import com.tungsten.fclcore.fakefx.beans.InvalidationListener
import com.tungsten.fclcore.fakefx.beans.binding.ObjectBinding
import com.tungsten.fclcore.mod.RemoteMod
import com.tungsten.fclcore.mod.RemoteMod.IMod
import com.tungsten.fclcore.mod.RemoteModRepository
import com.tungsten.fclcore.util.Logging.LOG
import com.tungsten.fcllibrary.component.FCLActivity
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog
import com.tungsten.fcllibrary.component.theme.ThemeEngine
import com.tungsten.fcllibrary.component.ui.FCLPage
import com.tungsten.fcllibrary.component.view.FCLMenuView
import com.tungsten.fcllibrary.util.ConvertUtils
import com.tungsten.fcllibrary.util.shareLogFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.logging.Level
import java.util.stream.Stream
import kotlin.system.exitProcess

class MainActivity : FCLActivity() {

    companion object {
        private lateinit var instance: WeakReference<MainActivity>

        @JvmStatic
        fun getInstance(): MainActivity {
            return instance.get()!!
        }
    }

    lateinit var uiManager: UIManager
    lateinit var permissionResultLauncher: ActivityResultLauncher<String>
    var isVersionLoading: Boolean = false
    var mediaPlayer: MediaPlayer? = null

    private lateinit var sharedPreferences: SharedPreferences
    private var modpackHandled = false
    private var pendingOpenDownload = false
    private var avatarBinding: ObjectBinding<BitmapDrawable>? = null

    // Compose UI States
    private var selectedTab by mutableStateOf(PclMainTab.LAUNCH)
    private var activeSubpage by mutableStateOf<PclSubpage?>(null)
    private var showDownloadPanel by mutableStateOf(false)
    private var launchState by mutableStateOf(LaunchUiState())
    private var seedColorInt by mutableIntStateOf(0xFF1E78D6.toInt())
    private var backgroundDrawable by mutableStateOf<BitmapDrawable?>(null)

    private val themeRefreshListener = Runnable {
        val theme = ThemeEngine.getInstance().getTheme()
        seedColorInt = theme.getColor()
        backgroundDrawable = theme.getBackground(this)
    }

    private val accountListener = InvalidationListener {
        refreshAccountState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        modpackHandled = savedInstanceState?.getBoolean("modpack_handled") ?: false
        instance = WeakReference(this)
        sharedPreferences = getSharedPreferences("launcher", MODE_PRIVATE)

        if (!LegalAgreement.isAccepted(this)) {
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
            return
        }

        registerBrokenDependencyFallback()
        ensureConfigInitialized()

        seedColorInt = ThemeEngine.getInstance().getTheme().getColor()
        backgroundDrawable = ThemeEngine.getInstance().getTheme().getBackground(this)
        ThemeEngine.getInstance().addRefreshListener(themeRefreshListener)

        permissionResultLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

        uiManager = UIManager(this, pager = null).apply {
            registerDefaultBackEvent {
                activeSubpage = null
                selectedTab = PclMainTab.LAUNCH
            }
            init()
            onComposeSwitchUI = { position ->
                syncComposeRouteFromUiPosition(position)
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackNavigation()
                }
            }
        )

        setupAccountObserver()
        setupVersionObserver()
        setupDownloadObserver()
        handleNotificationIntent(intent)
        if (!modpackHandled) {
            handleModpack(intent)
        }
        UpdateChecker.getInstance().checkAuto(this).start()
        promptNotificationPermissionIfNeeded()

        val appVersion = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.3.4.5"
        }.getOrDefault("1.3.4.5")

        setContent {
            val isDark = ThemeEngine.isNightMode(this)
            PclfTheme(
                seedColorInt = seedColorInt,
                darkTheme = isDark
            ) {
                PclfMainScreen(
                    selectedTab = selectedTab,
                    activeSubpage = activeSubpage,
                    launchState = launchState,
                    backgroundDrawable = backgroundDrawable,
                    showDownloadPanel = showDownloadPanel,
                    appVersionName = appVersion,
                    legacyUiProvider = { pos -> uiManager.getUI(pos) },
                    onSelectTab = { tab ->
                        activeSubpage = null
                        selectedTab = tab
                        syncUiManagerFromTab(tab)
                    },
                    onOpenSubpage = { sub ->
                        openSubpage(sub)
                    },
                    onBackFromSubpage = {
                        handleBackNavigation()
                    },
                    onAccountClick = {
                        openSubpage(PclSubpage.ACCOUNT)
                    },
                    onLaunchClick = {
                        launchVersion(Profiles.getSelectedVersion())
                    },
                    onLaunchLongClick = {
                        openRendererSelectorAndLaunch()
                    },
                    onVersionLongClick = {
                        openQuickVersionSwitch()
                    },
                    onVersionSettingClick = {
                        openVersionOrGlobalSetting()
                    },
                    onOpenModManage = {
                        navigateToManage(1)
                    },
                    onToggleDownloadPanel = {
                        showDownloadPanel = !showDownloadPanel
                    },
                    onCancelTask = { info ->
                        DownloadManager.cancel(info)
                    },
                    onShareLatestLog = {
                        shareLogFile(this, FCLPath.getLatestGameLog())
                    },
                    onOpenShell = {
                        startActivity(Intent(this, ShellActivity::class.java))
                    },
                    onCheckUpdate = {
                        UpdateChecker.getInstance().checkAuto(this).start()
                        Toast.makeText(this, "正在检查更新...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
        handleModpack(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("modpack_handled", modpackHandled)
    }

    override fun onPause() {
        super.onPause()
        if (::uiManager.isInitialized) uiManager.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (::uiManager.isInitialized) uiManager.onResume()
        refreshVersionDisplay(Profiles.getSelectedVersion())
        mediaPlayer?.start()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        seedColorInt = ThemeEngine.getInstance().getTheme().getColor()
        backgroundDrawable = ThemeEngine.getInstance().getTheme().getBackground(this)
        DisplayUtil.updateWindowSize(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ThemeEngine.getInstance().removeRefreshListener(themeRefreshListener)
        Accounts.selectedAccountProperty().removeListener(accountListener)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBackNavigation()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // ==================== Navigation Helpers (Clean Bridge for FCL Callers) ====================

    fun navigateToHome() {
        activeSubpage = null
        selectedTab = PclMainTab.LAUNCH
    }

    @JvmOverloads
    fun navigateToDownload(pageId: Int? = null) {
        activeSubpage = null
        selectedTab = PclMainTab.DOWNLOAD
        val downloadUI = uiManager.downloadUI
        uiManager.currentUI = downloadUI
        if (pageId != null) {
            downloadUI.showDownloadPage(pageId)
        }
    }

    @JvmOverloads
    fun navigateToManage(tabIndex: Int? = null) {
        val profile = Profiles.getSelectedProfile()
        val version = profile.selectedVersion
        if (version != null) {
            uiManager.manageUI.setVersion(version, profile)
        }
        activeSubpage = PclSubpage.MANAGE
        uiManager.currentUI = uiManager.manageUI
        if (tabIndex != null) {
            val tab = uiManager.manageUI.tabLayout.getTabAt(tabIndex)
            uiManager.manageUI.tabLayout.selectTab(tab)
        }
    }

    @JvmOverloads
    fun navigateToController(pageIndex: Int? = null) {
        activeSubpage = PclSubpage.CONTROLLER
        uiManager.currentUI = uiManager.controllerUI
        if (pageIndex != null) {
            uiManager.controllerUI.showPage(pageIndex)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun refreshMenuView(view: FCLMenuView?) {
        // Legacy menu highlight hook preserved as no-op for existing FCL callers
    }

    fun shouldPlayVideo(): Boolean = File(FCLPath.LIVE_BACKGROUND_PATH).exists()

    fun setupLiveBackground() {
        // Live background video playback will be integrated into Compose player in a follow-up step
    }

    fun setLiveBackgroundVolume() {
        mediaPlayer?.let {
            val volume = sharedPreferences.getInt("videoBackgroundVolume", 100) / 100f
            it.setVolume(volume, volume)
        }
    }

    fun stopLiveBackground() {
        mediaPlayer?.let {
            runCatching { it.stop(); it.release() }
        }
        mediaPlayer = null
    }

    fun refreshAvatar(account: Account) {
        lifecycleScope.launch {
            if (Accounts.getSelectedAccount() === account) {
                refreshAccountState()
            }
        }
    }

    fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            } else {
                permissionResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // ==================== Private Setup & Logic ====================

    private fun handleBackNavigation() {
        val current = activeSubpage
        if (current != null) {
            uiManager.getUI(current.uiIndex).onBackPressed()
            return
        }

        if (selectedTab != PclMainTab.LAUNCH) {
            val legacyIdx = when (selectedTab) {
                PclMainTab.DOWNLOAD -> 2
                PclMainTab.LINK -> 4
                PclMainTab.SETUP -> 5
                else -> -1
            }
            if (legacyIdx >= 0) {
                uiManager.getUI(legacyIdx).onBackPressed()
            } else {
                selectedTab = PclMainTab.LAUNCH
            }
            return
        }

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addCategory(Intent.CATEGORY_HOME)
        }
        startActivity(homeIntent)
        exitProcess(0)
    }

    private fun syncUiManagerFromTab(tab: PclMainTab) {
        when (tab) {
            PclMainTab.DOWNLOAD -> uiManager.currentUI = uiManager.downloadUI
            PclMainTab.LINK -> uiManager.currentUI = uiManager.multiplayerUI
            PclMainTab.SETUP -> uiManager.currentUI = uiManager.settingUI
            else -> {}
        }
    }

    private fun syncComposeRouteFromUiPosition(position: Int) {
        when (position) {
            0 -> navigateToHome()
            1 -> activeSubpage = PclSubpage.MANAGE
            2 -> {
                activeSubpage = null
                selectedTab = PclMainTab.DOWNLOAD
            }
            3 -> activeSubpage = PclSubpage.CONTROLLER
            4 -> {
                activeSubpage = null
                selectedTab = PclMainTab.LINK
            }
            5 -> {
                activeSubpage = null
                selectedTab = PclMainTab.SETUP
            }
            6 -> activeSubpage = PclSubpage.ACCOUNT
            7 -> activeSubpage = PclSubpage.VERSION
        }
    }

    private fun openSubpage(sub: PclSubpage) {
        if (sub == PclSubpage.MANAGE) {
            val profile = Profiles.getSelectedProfile()
            profile.selectedVersion?.let { uiManager.manageUI.setVersion(it, profile) }
        }
        uiManager.currentUI = uiManager.getUI(sub.uiIndex)
        activeSubpage = sub
    }

    private fun openVersionOrGlobalSetting() {
        val profile = Profiles.getSelectedProfile()
        if (profile.versionSetting.isUsesGlobal) {
            activeSubpage = null
            selectedTab = PclMainTab.SETUP
            val settingUI = uiManager.settingUI
            uiManager.currentUI = settingUI
            val tab = settingUI.tabLayout.getTabAt(0)
            settingUI.tabLayout.selectTab(tab)
        } else {
            navigateToManage(0)
        }
    }

    private fun openQuickVersionSwitch() {
        val profile = Profiles.getSelectedProfile()
        val versions = profile.repository.versions.map { it.id }
        if (versions.isEmpty()) {
            openSubpage(PclSubpage.VERSION)
            return
        }
        val currentIdx = versions.indexOf(profile.selectedVersion).coerceAtLeast(0)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.version))
            .setSingleChoiceItems(versions.toTypedArray(), currentIdx) { dialog, which ->
                profile.selectedVersion = versions[which]
                dialog.dismiss()
            }
            .setPositiveButton("版本管理") { _, _ -> openSubpage(PclSubpage.VERSION) }
            .setNegativeButton(getString(R.string.dialog_negative), null)
            .show()
    }

    private fun openRendererSelectorAndLaunch() {
        val previousRenderer = Profiles.getSelectedProfile().versionSetting.renderer
        RendererSelectDialog(this, false) { picked ->
            val profile = Profiles.getSelectedProfile()
            val versionId = profile.selectedVersion
            val override = versionId?.let { profile.getEffectiveRendererOverride(it) }
            if (versionId != null && override != null && override != picked.id) {
                FCLAlertDialog.Builder(this)
                    .setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
                    .setMessage(
                        getString(
                            R.string.message_warn_renderer_override_shadow,
                            versionId,
                            RendererManager.getRenderer(override).des
                        )
                    )
                    .setPositiveButton(getString(R.string.renderer_update_override)) {
                        profile.setRendererOverride(versionId, picked.id)
                        launchVersion(versionId)
                    }
                    .setNeutralButton(getString(R.string.renderer_clear_override)) {
                        profile.setRendererOverride(versionId, null)
                        launchVersion(versionId)
                    }
                    .setNegativeButton(getString(R.string.dialog_negative)) {
                        profile.versionSetting.renderer = previousRenderer
                    }
                    .create()
                    .show()
            } else {
                launchVersion(versionId)
            }
        }.show()
    }

    private fun setupAccountObserver() {
        Accounts.selectedAccountProperty().addListener(accountListener)
        refreshAccountState()
    }

    private fun refreshAccountState() {
        val account = Accounts.getSelectedAccount()
        if (account == null) {
            avatarBinding = null
            val defaultAvatar = TexturesLoader.toAvatar(
                TexturesLoader.getDefaultSkin(TextureModel.ALEX).image,
                ConvertUtils.dip2px(this, 64f)
            ).asImageBitmap()
            launchState = launchState.copy(
                accountName = getString(R.string.account_state_no_account),
                accountTypeBadge = "未登录",
                accountHint = getString(R.string.account_state_add),
                accountAvatar = defaultAvatar
            )
        } else {
            val typeLabel = if (account is AuthlibInjectorAccount) {
                account.server.name
            } else {
                Accounts.getLocalizedLoginTypeName(this, Accounts.getAccountFactory(account))
            }
            val binding = TexturesLoader.avatarBinding(account, ConvertUtils.dip2px(this, 64f))
            avatarBinding = binding
            val currentBmp = binding.get()?.bitmap?.asImageBitmap()
            binding.addListener { _, _, newVal ->
                val img = newVal?.bitmap?.asImageBitmap()
                launchState = launchState.copy(accountAvatar = img)
            }
            launchState = launchState.copy(
                accountName = account.character,
                accountTypeBadge = typeLabel,
                accountHint = "点击管理或切换账号",
                accountAvatar = currentBmp
            )
        }
    }

    private fun setupVersionObserver() {
        lifecycleScope.launch {
            Profiles.selectedVersion.collect { versionId ->
                refreshVersionDisplay(versionId)
            }
        }
        ViewModelProvider(this).get(VersionListViewModel::class.java)
    }

    private fun refreshVersionDisplay(version: String?) {
        isVersionLoading = true
        launchState = launchState.copy(isVersionLoading = true)
        val profile = Profiles.getSelectedProfile()

        if (version != null && profile.repository.hasVersion(version)) {
            lifecycleScope.launch(Dispatchers.IO) {
                val game = runCatching {
                    profile.repository.getGameVersion(version).orElse(getString(R.string.message_unknown))
                }.getOrNull() ?: return@launch

                val tags = mutableListOf<String>()
                tags.add("MC $game")
                val libraries = StringBuilder(game)
                val analyzer = LibraryAnalyzer.analyze(
                    profile.repository.getResolvedPreservingPatchesVersion(version),
                    profile.repository.getGameVersion(version).orElse(null)
                )
                for (mark in analyzer) {
                    val libraryId = mark.libraryId
                    val libraryVersion = mark.libraryVersion
                    if (libraryId == LibraryType.MINECRAFT.patchId) continue
                    if (hasStringId(this@MainActivity, "install_installer_" + libraryId.replace("-", "_"))) {
                        val libName = getLocalizedText(this@MainActivity, "install_installer_" + libraryId.replace("-", "_"))
                        val cleanVer = libraryVersion?.replace("(?i)$libraryId".toRegex(), "")?.trim()
                        val tagText = if (!cleanVer.isNullOrEmpty()) "$libName $cleanVer" else libName
                        tags.add(tagText)
                        libraries.append(", ").append(libName)
                        if (libraryVersion != null) {
                            libraries.append(": ").append(libraryVersion.replace("(?i)$libraryId".toRegex(), ""))
                        }
                    }
                }

                val iconDrawable = profile.repository.getVersionIconImage(version)
                val iconBitmap: ImageBitmap? = runCatching {
                    iconDrawable.toBitmap(96, 96).asImageBitmap()
                }.getOrNull()

                val setting = profile.getVersionSetting(version)
                val rendererDes = runCatching {
                    RendererManager.getRenderer(setting.renderer).des
                }.getOrDefault(setting.renderer)
                val driverDes = if (setting.isVKDriverSystem) "系统驱动" else setting.driver.ifBlank { "自定义" }
                val javaLabel = if (setting.java == "Auto") "自动" else setting.java
                val memLabel = if (setting.isAutoMemory) "自动" else "${setting.maxMemory} MB"

                withContext(Dispatchers.Main) {
                    isVersionLoading = false
                    launchState = launchState.copy(
                        selectedVersionName = version,
                        versionDetail = libraries.toString(),
                        versionTags = tags,
                        hasSelectedVersion = true,
                        versionIcon = iconBitmap,
                        isVersionLoading = false,
                        rendererName = rendererDes,
                        driverName = driverDes,
                        javaVersion = javaLabel,
                        memorySummary = memLabel
                    )
                }
            }
        } else {
            isVersionLoading = false
            val defaultIcon = runCatching {
                pixelAwareIcon(this, R.drawable.img_grass).toBitmap(96, 96).asImageBitmap()
            }.getOrNull()
            launchState = launchState.copy(
                selectedVersionName = getString(R.string.version_no_version),
                versionDetail = "请前往「下载」安装或导入整合包",
                versionTags = emptyList(),
                hasSelectedVersion = false,
                versionIcon = defaultIcon,
                isVersionLoading = false
            )
        }
    }

    private fun setupDownloadObserver() {
        lifecycleScope.launch {
            var wasEmpty = true
            DownloadManager.tasks.collect { tasks ->
                launchState = launchState.copy(downloadTasks = tasks)
                if (tasks.isEmpty()) {
                    showDownloadPanel = false
                } else if (wasEmpty) {
                    showDownloadPanel = true
                }
                wasEmpty = tasks.isEmpty()
            }
        }
        lifecycleScope.launch {
            DownloadManager.progress.collect { progress ->
                launchState = launchState.copy(downloadProgress = progress.toFloat())
            }
        }
    }

    private fun launchVersion(versionId: String?) {
        if (!Controllers.isInitialized()) {
            Toast.makeText(this, getString(R.string.message_loading_controllers), Toast.LENGTH_SHORT).show()
            return
        }
        QuarkPromo.interceptLaunch(this) {
            val selectedProfile = Profiles.getSelectedProfile()
            DriverPlugin.selected = runCatching {
                DriverPlugin.driverList.find {
                    it.driver == selectedProfile.getVersionSetting(versionId).driver
                }
            }.getOrNull() ?: DriverPlugin.driverList[0]
            DisplayUtil.updateWindowSize(this)
            DisplayUtil.refreshDisplayMetrics(this)
            checkVulkanThenLaunch(selectedProfile, versionId)
        }
    }

    private fun checkVulkanThenLaunch(profile: Profile, versionId: String?) {
        lifecycleScope.launch {
            val launchId = versionId ?: Profiles.getSelectedVersion()
            val gameVersion = withContext(Dispatchers.IO) {
                launchId?.let {
                    runCatching { profile.repository.getGameVersion(it).orElse(null) }.getOrNull()
                }
            }
            if (gameVersion == null || !VulkanCheckManager.hasVulkanBackend(gameVersion)) {
                doLaunchVersion(profile, versionId)
                return@launch
            }
            val versionSetting = profile.getVersionSetting(launchId)
            if (versionSetting.isNotCheckVulkan) {
                doLaunchVersion(profile, versionId)
                return@launch
            }
            val useCustomDriver = !versionSetting.isVKDriverSystem
            val driver = if (useCustomDriver) {
                DriverPlugin.driverList.find { it.driver == versionSetting.driver }
                    ?: DriverPlugin.driverList[0]
            } else null

            when (VulkanCheckManager.ensureSupported(gameVersion, useCustomDriver, driver)) {
                VulkanEnsureResult.SUPPORTED -> doLaunchVersion(profile, versionId)
                VulkanEnsureResult.NEED_CHECK ->
                    promptVulkanCheck(gameVersion, useCustomDriver, driver, profile, versionId)
                VulkanEnsureResult.UNSUPPORTED ->
                    runVulkanCheckAndDecide(gameVersion, useCustomDriver, driver, profile, versionId)
            }
        }
    }

    private fun promptVulkanCheck(
        gameVersion: String,
        useCustomDriver: Boolean,
        driver: DriverPlugin.Driver?,
        profile: Profile,
        versionId: String?
    ) {
        FCLAlertDialog.Builder(this)
            .setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
            .setMessage(getString(R.string.vulkan_check_launch_tip))
            .setPositiveButton(getString(R.string.dialog_positive)) {
                lifecycleScope.launch {
                    runVulkanCheckAndDecide(gameVersion, useCustomDriver, driver, profile, versionId)
                }
            }
            .setNegativeButton { doLaunchVersion(profile, versionId) }
            .create()
            .show()
    }

    private suspend fun runVulkanCheckAndDecide(
        gameVersion: String,
        useCustomDriver: Boolean,
        driver: DriverPlugin.Driver?,
        profile: Profile,
        versionId: String?
    ) {
        val capabilities = withContext(Dispatchers.IO) {
            VulkanCheckManager.check(useCustomDriver, driver)
        }
        val usedCustomDriver = capabilities?.usedCustomDriver == true
        val driverName = if (usedCustomDriver) driver?.driver else null
        com.mio.ui.dialog.VulkanCheckDialog(this, capabilities, usedCustomDriver, driverName) {
            decideVulkanLaunch(gameVersion, capabilities, profile, versionId)
        }.show()
    }

    private fun decideVulkanLaunch(
        gameVersion: String,
        capabilities: VulkanCapabilities?,
        profile: Profile,
        versionId: String?
    ) {
        val support = capabilities?.supportFor(normalizeMcVersion(gameVersion))
        if (support?.isSupported == true) {
            doLaunchVersion(profile, versionId)
            return
        }
        val missing = support?.missingRequired
            ?.joinToString("\n") { "· ${it.dependency.name}" }
            .orEmpty()
        FCLAlertDialog.Builder(this)
            .setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
            .setMessage(getString(R.string.vulkan_check_launch_warning, missing))
            .setPositiveButton(getString(R.string.vulkan_check_launch_continue)) {
                doLaunchVersion(profile, versionId)
            }
            .setNegativeButton {}
            .create()
            .show()
    }

    private fun doLaunchVersion(profile: Profile, versionId: String?) {
        Versions.launch(this, profile, versionId)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(DownloadManager.EXTRA_OPEN_PANEL, false) == true) {
            pendingOpenDownload = true
            if (::uiManager.isInitialized) {
                pendingOpenDownload = false
                navigateToDownload()
                showDownloadPanel = true
            }
        }
    }

    private fun handleModpack(intent: Intent) {
        val path = intent.getStringExtra("modpack_cache_path") ?: return
        modpackHandled = true
        val file = File(path)
        if (!file.exists()) return
        Toast.makeText(
            this,
            getString(R.string.modpack_external_detected, file.name),
            Toast.LENGTH_SHORT
        ).show()
        navigateToDownload()
        val page = LocalModpackPage(
            this,
            FCLPage.PAGE_ID_TEMP,
            Profiles.getSelectedProfile(),
            null,
            file
        )
        uiManager.downloadUI.showTempPage(page)
    }

    private fun promptNotificationPermissionIfNeeded() {
        if (!checkNotificationPermission() &&
            sharedPreferences.getBoolean("check_notification_permission", true)
        ) {
            sharedPreferences.edit {
                putBoolean("check_notification_permission", false)
            }
            FCLAlertDialog.Builder(this)
                .setMessage(getString(R.string.notification_permission))
                .setPositiveButton { requestNotificationPermission() }
                .setNegativeButton {}
                .create()
                .show()
        }
    }

    private fun ensureConfigInitialized() {
        if (!ConfigHolder.isInit()) {
            try {
                ConfigHolder.init()
                RendererManager.init(this)
            } catch (e: IOException) {
                LOG.log(Level.WARNING, e.message)
            }
        }
    }

    private fun registerBrokenDependencyFallback() {
        RemoteMod.registerEmptyRemoteMod(
            RemoteMod(
                "",
                "",
                getString(R.string.mods_broken_dependency_title),
                getString(R.string.mods_broken_dependency_desc),
                ArrayList(),
                "",
                "",
                object : IMod {
                    @Throws(IOException::class)
                    override fun loadDependencies(modRepository: RemoteModRepository): List<RemoteMod> =
                        throw IOException()

                    @Throws(IOException::class)
                    override fun loadVersions(modRepository: RemoteModRepository): Stream<RemoteMod.Version> =
                        throw IOException()

                    override fun loadScreenshots(modRepository: RemoteModRepository): MutableList<RemoteMod.Screenshot> =
                        throw IOException()
                },
                114514,
                ""
            )
        )
    }
}
