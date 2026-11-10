package com.tungsten.fcl.pclf.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tungsten.fcl.pclf.theme.PclfTheme
import com.tungsten.fcl.pclf.ui.components.PclCard
import com.tungsten.fcl.pclf.ui.components.PclHintBanner
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference

/**
 * PCL2 `更多` tab powered by Miuix Preference cards.
 */
@Composable
fun PclfMorePage(
    versionName: String,
    onOpenController: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenVersions: () -> Unit,
    onShareLatestLog: () -> Unit,
    onOpenShell: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PclfTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PclHintBanner(
            text = "ProCraftLauncherFold (v$versionName) • 基于 FCL 核心重构的 PCL2 风格自适应启动器"
        )

        SmallTitle(text = "常用功能入口")
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 12.dp,
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(
                color = colors.cardBg,
                contentColor = colors.textPrimary
            )
        ) {
            ArrowPreference(
                title = "虚拟按键与控制器布局",
                summary = "管理、导入或编辑游戏内触控布局 (支持 ZL2 布局转换)",
                onClick = onOpenController
            )
            ArrowPreference(
                title = "账号管理",
                summary = "管理微软正版、离线及外置登录账号",
                onClick = onOpenAccount
            )
            ArrowPreference(
                title = "版本与游戏目录管理",
                summary = "切换多游戏目录、添加新文件夹隔离",
                onClick = onOpenVersions
            )
        }

        SmallTitle(text = "诊断与工具")
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 12.dp,
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(
                color = colors.cardBg,
                contentColor = colors.textPrimary
            )
        ) {
            ArrowPreference(
                title = "分享最新游戏日志",
                summary = "快速导出 latest_game.log 以便排查崩溃原因",
                onClick = onShareLatestLog
            )
            ArrowPreference(
                title = "终端与调试 Shell",
                summary = "运行本地命令行工具检查运行时环境",
                onClick = onOpenShell
            )
            ArrowPreference(
                title = "检查启动器更新",
                summary = "检查上游与 PCLF 最新版本",
                onClick = onCheckUpdate
            )
        }

        PclCard(title = "开源与反倒卖声明") {
            Text(
                text = "本启动器代码遵循 GPL-3.0 协议完全免费开源，核心由 FCL-Team 维护，UI 致敬龙腾猫跃的 Plain Craft Launcher 2。网络上任何收费倒卖的「PCL 手机版」均属诈骗行为。",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}
