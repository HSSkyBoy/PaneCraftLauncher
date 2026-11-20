package com.tungsten.fcl.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tungsten.fcl.R
import com.tungsten.fcl.util.ShellUtil
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fcllibrary.component.FCLActivity
import com.tungsten.fcllibrary.component.theme.ThemeEngine
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LauncherButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import java.io.File

class ShellActivity : FCLActivity() {

    private lateinit var shellUtil: ShellUtil
    private val outputLines = mutableStateListOf(
        "Welcome to use Pane Craft Launcher!",
        "Here is the shell command line!"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val homeDir = File(FCLPath.FILES_DIR).parent ?: FCLPath.FILES_DIR
        shellUtil = ShellUtil(homeDir) { output ->
            runOnUiThread {
                outputLines.add("\t$output")
            }
        }
        shellUtil.start()

        val seedColor = runCatching { ThemeEngine.getInstance().getTheme().getColor() }
            .getOrDefault(0xFF1976D2.toInt())
        val isDark = runCatching { ThemeEngine.isNightMode(this) }.getOrDefault(false)

        setContent {
            LauncherTheme(seedColorInt = seedColor, darkTheme = isDark) {
                val colors = LauncherTheme.colors
                var commandInput by remember { mutableStateOf("") }
                val listState = rememberLazyListState()

                LaunchedEffect(outputLines.size) {
                    if (outputLines.isNotEmpty()) {
                        listState.animateScrollToItem(outputLines.lastIndex)
                    }
                }

                fun sendCommand() {
                    val cmd = commandInput.trim()
                    if (cmd.isEmpty()) return
                    outputLines.add("-> $cmd")
                    commandInput = ""
                    if (cmd == "clear") {
                        outputLines.clear()
                    } else {
                        shellUtil.append("$cmd\n")
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.pclf_more_shell),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LauncherButton(
                            text = stringResource(R.string.button_back),
                            onClick = { finish() },
                            accentStyle = false,
                            height = 28.dp
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF14181F))
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            SelectionContainer {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    items(outputLines) { line ->
                                        Text(
                                            text = line,
                                            color = if (line.startsWith("->")) Color(0xFF64B5F6) else Color(0xFFE6EDF3),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 17.sp
                                        )
                                    }
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
                                    value = commandInput,
                                    onValueChange = { commandInput = it },
                                    label = "Command",
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = { sendCommand() }),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            LauncherButton(
                                text = "Run",
                                onClick = { sendCommand() },
                                accentStyle = true
                            )
                            LauncherButton(
                                text = "Clear",
                                onClick = { outputLines.clear() },
                                accentStyle = false
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::shellUtil.isInitialized) {
            shellUtil.interrupt()
        }
    }
}
