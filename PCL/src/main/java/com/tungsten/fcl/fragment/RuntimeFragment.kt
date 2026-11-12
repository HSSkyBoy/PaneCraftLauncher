package com.tungsten.fcl.fragment

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.SplashActivity
import com.tungsten.fcl.util.RuntimeUtils
import com.tungsten.fclauncher.utils.Architecture
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.util.Logging
import com.tungsten.fcllibrary.component.theme.ThemeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LauncherButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.window.WindowDialog
import java.util.logging.Level

class RuntimeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val context = LocalContext.current
                val isDark = ThemeEngine.isNightMode(context)
                val theme = ThemeEngine.getInstance().getTheme()
                LauncherTheme(seedColorInt = theme.getColor(), darkTheme = isDark) {
                    RuntimeScreen()
                }
            }
        }
    }
}

private enum class RuntimeTarget(
    val id: String,
    @StringRes val titleRes: Int,
    val installAction: (Context, RuntimeUtils.InstallListener) -> Unit,
    val isPresent: (SplashActivity) -> Boolean,
    val markPresent: (SplashActivity) -> Unit
) {
    LWJGL(
        id = "lwjgl",
        titleRes = R.string.splash_runtime_lwjgl,
        installAction = { ctx, listener ->
            RuntimeUtils.install(ctx, FCLPath.LWJGL_DIR, "app_runtime/lwjgl", listener)
        },
        isPresent = { it.lwjgl },
        markPresent = { it.lwjgl = true }
    ),
    CACIO(
        id = "caciocavallo",
        titleRes = R.string.splash_runtime_cacio,
        installAction = { ctx, listener ->
            RuntimeUtils.install(ctx, FCLPath.CACIOCAVALLO_8_DIR, "app_runtime/caciocavallo", listener)
        },
        isPresent = { it.cacio },
        markPresent = { it.cacio = true }
    ),
    CACIO17(
        id = "caciocavallo17",
        titleRes = R.string.splash_runtime_cacio17,
        installAction = { ctx, listener ->
            RuntimeUtils.install(ctx, FCLPath.CACIOCAVALLO_17_DIR, "app_runtime/caciocavallo17", listener)
        },
        isPresent = { it.cacio17 },
        markPresent = { it.cacio17 = true }
    ),
    JAVA8(
        id = "jre8",
        titleRes = R.string.splash_runtime_java8,
        installAction = { ctx, listener ->
            RuntimeUtils.installJava(ctx, FCLPath.JAVA_8_PATH, "app_runtime/java/jre8", listener)
        },
        isPresent = { it.java8 },
        markPresent = { it.java8 = true }
    ),
    JAVA17(
        id = "jre17",
        titleRes = R.string.splash_runtime_java17,
        installAction = { ctx, listener ->
            RuntimeUtils.installJava(ctx, FCLPath.JAVA_17_PATH, "app_runtime/java/jre17", listener)
        },
        isPresent = { it.java17 },
        markPresent = { it.java17 = true }
    ),
    JAVA21(
        id = "jre21",
        titleRes = R.string.splash_runtime_java21,
        installAction = { ctx, listener ->
            RuntimeUtils.installJava(ctx, FCLPath.JAVA_21_PATH, "app_runtime/java/jre21", listener)
        },
        isPresent = { it.java21 },
        markPresent = { it.java21 = true }
    ),
    JAVA25(
        id = "jre25",
        titleRes = R.string.splash_runtime_java25,
        installAction = { ctx, listener ->
            RuntimeUtils.installJava(ctx, FCLPath.JAVA_25_PATH, "app_runtime/java/jre25", listener)
        },
        isPresent = { it.java25 },
        markPresent = { it.java25 = true }
    ),
    JNA(
        id = "jna",
        titleRes = R.string.splash_runtime_jna,
        installAction = { ctx, listener ->
            RuntimeUtils.installJna(ctx, FCLPath.JNA_PATH, "app_runtime/jna", listener)
        },
        isPresent = { it.jna },
        markPresent = { it.jna = true }
    )
}

@Composable
private fun RuntimeScreen() {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val targets = remember { RuntimeTarget.values() }

    val installedStates = remember { mutableStateMapOf<String, Boolean>() }
    val installingStates = remember { mutableStateMapOf<String, Boolean>() }
    val detailTexts = remember { mutableStateMapOf<String, String>() }

    var isGlobalInstalling by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val splashActivity = context as? SplashActivity

    LaunchedEffect(Unit) {
        splashActivity?.let { act ->
            targets.forEach { target ->
                installedStates[target.id] = target.isPresent(act)
            }
        }
        if (targets.all { installedStates[it.id] == true }) {
            splashActivity?.enterLauncher()
        }
    }

    val installedCount = targets.count { installedStates[it.id] == true }
    val isAllInstalled = installedCount == targets.size

    val performInstall: () -> Unit = {
        if (!isGlobalInstalling && !isAllInstalled) {
            val deviceArch = Architecture.archAsString(Architecture.getDeviceArchitecture())
            if (!isJavaArchSupported(context, deviceArch)) {
                errorMessage = context.getString(
                    R.string.missing_runtime_arch_files,
                    deviceArch,
                    "PCL-release-x.x.x.x-$deviceArch.apk",
                    "PCL-release-x.x.x.x-all.apk"
                )
            } else {
                isGlobalInstalling = true
                val missingTargets = targets.filter { installedStates[it.id] != true }
                missingTargets.forEach { target ->
                    installingStates[target.id] = true
                    scope.launch {
                        val listener = object : RuntimeUtils.InstallListener {
                            private var lastUpdateTime = 0L
                            override fun onUpdate(detailText: String) {
                                val now = SystemClock.elapsedRealtime()
                                if (now - lastUpdateTime < 50L) return
                                lastUpdateTime = now
                                detailTexts[target.id] = detailText
                            }

                            override fun onStage(resId: Int) {
                                detailTexts[target.id] = context.getString(resId)
                            }
                        }

                        val error = withContext(Dispatchers.IO) {
                            runCatching {
                                target.installAction(context, listener)
                            }.exceptionOrNull()
                        }

                        installingStates[target.id] = false
                        detailTexts.remove(target.id)

                        if (error != null) {
                            Logging.LOG.log(
                                Level.SEVERE,
                                "Failed to install runtime component: ${target.id}",
                                error
                            )
                            errorMessage = Log.getStackTraceString(error)
                            isGlobalInstalling = false
                        } else {
                            installedStates[target.id] = true
                            splashActivity?.let { target.markPresent(it) }

                            if (targets.all { installedStates[it.id] == true }) {
                                isGlobalInstalling = false
                                splashActivity?.enterLauncher()
                            }
                        }
                    }
                }
            }
        }
    }

    val insets = WindowInsets.statusBars.union(WindowInsets.displayCutout)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(colors.titleBarStart, colors.titleBarEnd)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.splash_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "執行環境補全",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.66f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.cardBg)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        targets.forEach { target ->
                            val isInstalled = installedStates[target.id] == true
                            val isInstalling = installingStates[target.id] == true
                            val detail = detailTexts[target.id]

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.sidebarBg.copy(alpha = 0.6f))
                                    .border(0.5.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val iconRes = if (isInstalled) {
                                    R.drawable.ic_baseline_done_24
                                } else {
                                    R.drawable.ic_baseline_update_24
                                }
                                val iconTint = when {
                                    isInstalled -> Color(0xFF4CAF50)
                                    isInstalling -> colors.accent
                                    else -> colors.textSecondary.copy(alpha = 0.6f)
                                }

                                Image(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(iconTint),
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(target.titleRes),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textPrimary
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        val statusText = when {
                                            isInstalled -> "已就緒"
                                            isInstalling -> "正在安裝"
                                            else -> "未安裝"
                                        }
                                        val statusColor = when {
                                            isInstalled -> Color(0xFF4CAF50)
                                            isInstalling -> colors.accent
                                            else -> colors.textSecondary
                                        }
                                        Text(
                                            text = statusText,
                                            fontSize = 12.sp,
                                            color = statusColor
                                        )
                                    }

                                    if (isInstalling) {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 6.dp, bottom = 4.dp)
                                        )
                                        if (!detail.isNullOrBlank()) {
                                            Text(
                                                text = detail,
                                                fontSize = 11.sp,
                                                color = colors.textSecondary,
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

                Box(
                    modifier = Modifier
                        .weight(0.34f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.cardBg)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "執行環境配置",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(R.string.splash_runtime_title),
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "就緒組件",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "$installedCount / ${targets.size}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent
                            )
                        }

                        LinearProgressIndicator(
                            progress = if (targets.isNotEmpty()) installedCount.toFloat() / targets.size.toFloat() else 0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        LauncherButton(
                            text = when {
                                isGlobalInstalling -> "正在解壓環境..."
                                isAllInstalled -> "環境已就緒"
                                else -> stringResource(R.string.splash_runtime_install)
                            },
                            enabled = !isGlobalInstalling && !isAllInstalled,
                            accentStyle = true,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = performInstall
                        )
                    }
                }
            }
        }
    }

    if (errorMessage != null) {
        WindowDialog(
            show = true,
            title = "環境安裝失敗",
            onDismissRequest = { errorMessage = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "執行環境安裝遭遇錯誤，詳細調用棧如下：",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .background(colors.sidebarBg)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    LauncherButton(
                        text = "複製錯誤日誌",
                        accentStyle = false,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(errorMessage.orEmpty()))
                            Toast.makeText(context, "已複製調用棧至剪貼簿", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    LauncherButton(
                        text = stringResource(R.string.dialog_positive),
                        accentStyle = true,
                        onClick = { errorMessage = null }
                    )
                }
            }
        }
    }
}

private fun isJavaArchSupported(context: Context, arch: String): Boolean {
    return try {
        val javaDirs = listOf("jre8", "jre17", "jre21", "jre25")
        val assetManager = context.assets
        var supportedCount = 0
        for (javaDir in javaDirs) {
            val dirPath = "app_runtime/java/$javaDir"
            val files = assetManager.list(dirPath)
            if (files != null) {
                val expectedFile = "bin-$arch.tar.xz"
                if (files.contains(expectedFile)) {
                    supportedCount++
                }
            }
        }
        supportedCount > 0
    } catch (e: Exception) {
        Logging.LOG.log(Level.SEVERE, "Failed to check runtime arch support", e)
        false
    }
}
