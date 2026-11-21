package com.tungsten.fcl.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mio.util.showErrorDialog
import com.tungsten.fcl.R
import com.tungsten.fclcore.util.Logging
import com.tungsten.fclcore.util.io.FileUtils
import com.tungsten.fcllibrary.component.FCLActivity
import com.tungsten.fcllibrary.component.theme.ThemeEngine
import com.tungsten.fcllibrary.util.findFatalErrorLogPath
import com.tungsten.fcllibrary.util.shareLogFile
import com.tungsten.fcllibrary.util.uploadLog
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LauncherButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.logging.Level
import kotlin.system.exitProcess

class JVMCrashActivity : FCLActivity() {
    private var exitCode = 0
    private lateinit var logPath: String
    private var fatalErrorLogPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!getSharedPreferences("launcher", MODE_PRIVATE).getBoolean("allowScreenshots", false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        val extras = intent.extras
        val isGame = extras?.getBoolean("isGame") ?: true
        exitCode = extras?.getInt("exitCode") ?: -1
        logPath = extras?.getString("logPath").orEmpty()

        val titleText = if (isGame) {
            getString(R.string.game_crash_title) + getString(R.string.game_crash_title_add)
        } else {
            getString(R.string.jar_executor_crash_title)
        }

        var logContent = ""
        var fabricError: String? = null
        try {
            logContent = readLog()
            fabricError = findFabricIncompatibleModsError(logContent)
            fatalErrorLogPath = findFatalErrorLogPath(logContent)
        } catch (e: IOException) {
            Logging.LOG.log(Level.WARNING, "Failed to read log file", e)
            logContent = e.message.orEmpty()
        }

        val seedColor = runCatching { ThemeEngine.getInstance().getTheme().getColor() }
            .getOrDefault(0xFFD32F2F.toInt())
        val isDark = runCatching { ThemeEngine.isNightMode(this) }.getOrDefault(false)

        setContent {
            LauncherTheme(seedColorInt = seedColor, darkTheme = isDark) {
                CrashReportScreen(
                    title = titleText,
                    exitCode = exitCode,
                    fabricError = fabricError,
                    logText = logContent,
                    onCopy = {
                        val cm = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("crash_log", logContent))
                        Toast.makeText(this, getString(R.string.message_copy), Toast.LENGTH_SHORT).show()
                    },
                    onRestart = {
                        val intent = Intent(this, SplashActivity::class.java).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                            )
                            action = Intent.ACTION_MAIN
                            addCategory(Intent.CATEGORY_LAUNCHER)
                        }
                        finish()
                        startActivity(intent)
                        Process.killProcess(Process.myPid())
                        exitProcess(10)
                    },
                    onUpload = {
                        try {
                            var fullLog = readLog()
                            fatalErrorLogPath?.let {
                                fullLog = "$fullLog\n${readLog(it)}"
                            }
                            uploadLog(this, fullLog)
                        } catch (e: IOException) {
                            showErrorDialog(this, R.string.upload_failed, e.message)
                        }
                    },
                    onShare = {
                        shareLogFile(this, File(logPath))
                    },
                    onClose = {
                        finish()
                        Process.killProcess(Process.myPid())
                        exitProcess(10)
                    }
                )
            }
        }
    }

    @Throws(IOException::class)
    private fun readLog(path: String = logPath): String {
        if (File(path).length() < 8 * 1024 * 1024) {
            return FileUtils.readText(File(path))
        }
        throw IOException("Log file is too large, please check the log file manually.")
    }

    fun findFabricIncompatibleModsError(text: String): String? {
        val pattern = Regex(
            """start net\.fabricmc\.loader\.impl\.gui\.FabricGuiEntry\.displayError(.*?)end net\.fabricmc\.loader\.impl\.gui\.FabricGuiEntry\.displayError""",
            RegexOption.DOT_MATCHES_ALL
        )
        return pattern.find(text)?.groupValues?.get(1)?.trim()
    }

    companion object {
        @JvmStatic
        fun startCrashActivity(game: Boolean, context: Context, exitCode: Int, logPath: String?) {
            val intent = Intent(context, JVMCrashActivity::class.java)
            val bundle = Bundle()
            bundle.putBoolean("isGame", game)
            bundle.putInt("exitCode", exitCode)
            bundle.putString("logPath", logPath)
            intent.putExtras(bundle)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

@Composable
private fun CrashReportScreen(
    title: String,
    exitCode: Int,
    fabricError: String?,
    logText: String,
    onCopy: () -> Unit,
    onRestart: () -> Unit,
    onUpload: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LauncherTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var onlyErrors by remember { mutableStateOf(false) }

    val lines = remember(logText) { logText.lines() }
    val diagnostics = remember(logText) {
        buildList {
            if (logText.contains("OutOfMemoryError", ignoreCase = true) ||
                logText.contains("Java heap space", ignoreCase = true)
            ) {
                add(R.string.crash_diag_oom)
            }
            if (logText.contains("UnsupportedClassVersionError", ignoreCase = true) ||
                logText.contains("class file version", ignoreCase = true)
            ) {
                add(R.string.crash_diag_java)
            }
            if (logText.contains("GLFW error", ignoreCase = true) ||
                logText.contains("EGL_", ignoreCase = true) ||
                logText.contains("GL_INVALID_OPERATION", ignoreCase = true) ||
                logText.contains("VkResult", ignoreCase = true)
            ) {
                add(R.string.crash_diag_render)
            }
            if (logText.contains("MissingModsException", ignoreCase = true) ||
                logText.contains("Missing or unsupported mandatory dependencies", ignoreCase = true)
            ) {
                add(R.string.crash_diag_dep)
            }
            if (logText.contains("MixinApplyError", ignoreCase = true) ||
                logText.contains("MixinTransformerError", ignoreCase = true)
            ) {
                add(R.string.crash_diag_mixin)
            }
        }
    }
    val filteredLines = remember(lines, searchQuery, onlyErrors) {
        lines.filter { line ->
            val matchError = !onlyErrors ||
                line.contains("ERROR", ignoreCase = true) ||
                line.contains("FATAL", ignoreCase = true) ||
                line.contains("Exception", ignoreCase = true) ||
                line.contains("Caused by", ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                line.lowercase(Locale.getDefault()).contains(searchQuery.trim().lowercase(Locale.getDefault()))
            matchError && matchQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvasTint)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(colors.titleBarStart, colors.titleBarEnd)))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Exit $exitCode",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!fabricError.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x22D32F2F))
                        .border(1.dp, Color(0xFFD32F2F), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = fabricError,
                        color = colors.textPrimary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            if (diagnostics.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accent.copy(alpha = 0.10f))
                        .border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.crash_diag_title),
                        color = colors.accent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    diagnostics.forEach { msgRes ->
                        Text(
                            text = "• ${stringResource(msgRes)}",
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

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
                    text = if (onlyErrors) "All" else "Errors",
                    onClick = { onlyErrors = !onlyErrors },
                    accentStyle = onlyErrors
                )
                LauncherButton(
                    text = stringResource(R.string.message_copy),
                    onClick = onCopy,
                    accentStyle = false
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.cardBg)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                SelectionContainer {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filteredLines) { line ->
                            val isErr = line.contains("ERROR", ignoreCase = true) ||
                                line.contains("FATAL", ignoreCase = true) ||
                                line.contains("Exception", ignoreCase = true)
                            Text(
                                text = line,
                                color = if (isErr) Color(0xFFD32F2F) else colors.textPrimary,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                LauncherButton(
                    text = stringResource(R.string.crash_reporter_restart),
                    onClick = onRestart,
                    accentStyle = false
                )
                LauncherButton(
                    text = stringResource(R.string.crash_reporter_upload),
                    onClick = onUpload,
                    accentStyle = false
                )
                LauncherButton(
                    text = stringResource(R.string.crash_reporter_share),
                    onClick = onShare,
                    accentStyle = true
                )
                LauncherButton(
                    text = stringResource(R.string.crash_reporter_close),
                    onClick = onClose,
                    accentStyle = false
                )
            }
        }
    }
}
