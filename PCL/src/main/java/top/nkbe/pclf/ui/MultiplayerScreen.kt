package top.nkbe.pclf.ui

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.edit
import com.mio.util.getMimeType
import com.mio.util.openLink
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.terracotta.Terracotta
import com.tungsten.fclauncher.utils.FCLPath
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LauncherButton
import top.nkbe.pclf.ui.components.SectionCard
import top.nkbe.pclf.ui.components.SidebarNavItem
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.window.WindowDialog
import java.io.File

private enum class MultiplayerSection(val titleRes: Int, val iconRes: Int) {
    SETTINGS(R.string.terracotta_terracotta, R.drawable.ic_baseline_tune_24),
    HOST_TUTORIAL(R.string.terracotta_tutorial_host_btn, R.drawable.ic_baseline_host_24),
    GUEST_TUTORIAL(R.string.terracotta_tutorial_guest_btn, R.drawable.ic_baseline_guest_24)
}

@Composable
fun MultiplayerScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val prefs = remember { context.getSharedPreferences("third_party", Context.MODE_PRIVATE) }

    var section by remember { mutableStateOf(MultiplayerSection.SETTINGS) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("terracotta", false)) }
    var showNoticeDialog by remember { mutableStateOf(false) }

    fun applyMultiplayerEnabled(checked: Boolean) {
        enabled = checked
        prefs.edit { putBoolean("terracotta", checked) }
        if (checked) {
            val activity = context as? MainActivity ?: MainActivity.getInstance()
            if (!activity.checkNotificationPermission()) {
                activity.requestNotificationPermission()
            }
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
                text = stringResource(R.string.terracotta),
                color = colors.textSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            MultiplayerSection.entries.forEach { item ->
                SidebarNavItem(
                    label = stringResource(item.titleRes),
                    selected = section == item,
                    onClick = { section = item }
                )
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider(color = colors.cardBorder)
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(colors.cardBg)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(7.dp))
                    .clickable { openLink(context, "https://easytier.cn/") }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_baseline_easytier_24),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.accent),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.terracotta_easytier),
                    color = colors.textPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.End))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.widthIn(max = 700.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (section) {
                    MultiplayerSection.SETTINGS -> {
                        SectionCard(title = stringResource(R.string.terracotta_terracotta)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 8.dp,
                                insideMargin = PaddingValues(0.dp),
                                colors = CardDefaults.defaultColors(
                                    color = Color.Transparent,
                                    contentColor = colors.textPrimary
                                )
                            ) {
                                SwitchPreference(
                                    title = stringResource(R.string.terracotta_enable),
                                    checked = enabled,
                                    onCheckedChange = { isChecked ->
                                        val noticeVersion = prefs.getInt("terracotta_user_notice", 0)
                                        if (noticeVersion >= Terracotta.getUserNoticeVersion() || !isChecked) {
                                            applyMultiplayerEnabled(isChecked)
                                        } else {
                                            showNoticeDialog = true
                                        }
                                    }
                                )
                            }
                            if (enabled) {
                                HorizontalDivider(
                                    color = colors.cardBorder,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.terracotta_export_log_share),
                                        color = colors.textPrimary,
                                        fontSize = 13.5.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    LauncherButton(
                                        text = stringResource(R.string.action_share),
                                        onClick = {
                                            val logFile = File(FCLPath.LOG_DIR, "terracotta.log")
                                            if (logFile.exists()) {
                                                val intent = Intent(Intent.ACTION_SEND)
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.provider",
                                                    logFile
                                                )
                                                intent.type = getMimeType(logFile.absolutePath)
                                                intent.putExtra(Intent.EXTRA_STREAM, uri)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                intent.addCategory(Intent.CATEGORY_DEFAULT)
                                                context.startActivity(
                                                    Intent.createChooser(
                                                        intent,
                                                        context.getString(R.string.terracotta_export_log_share)
                                                    )
                                                )
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.terracotta_export_log_share_null),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        accentStyle = true
                                    )
                                }
                            }
                        }

                        SectionCard(title = stringResource(R.string.terracotta)) {
                            Text(
                                text = stringResource(R.string.terracotta_confirm),
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    MultiplayerSection.HOST_TUTORIAL -> {
                        SectionCard(title = stringResource(R.string.terracotta_tutorial_host_btn)) {
                            Text(
                                text = stringResource(R.string.terracotta_tutorial_host),
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    MultiplayerSection.GUEST_TUTORIAL -> {
                        SectionCard(title = stringResource(R.string.terracotta_tutorial_guest_btn)) {
                            Text(
                                text = stringResource(R.string.terracotta_tutorial_guest),
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNoticeDialog) {
        WindowDialog(
            show = true,
            title = stringResource(R.string.terracotta),
            onDismissRequest = { showNoticeDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.terracotta_status_uninitialized_desc),
                    color = colors.textPrimary,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    LauncherButton(
                        text = stringResource(R.string.dialog_negative),
                        onClick = { showNoticeDialog = false },
                        accentStyle = false
                    )
                    LauncherButton(
                        text = stringResource(R.string.dialog_positive),
                        onClick = {
                            prefs.edit {
                                putInt("terracotta_user_notice", Terracotta.getUserNoticeVersion())
                            }
                            showNoticeDialog = false
                            applyMultiplayerEnabled(true)
                        },
                        accentStyle = true
                    )
                }
            }
        }
    }
}
