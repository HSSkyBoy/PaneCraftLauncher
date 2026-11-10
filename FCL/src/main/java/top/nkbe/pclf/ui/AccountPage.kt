package top.nkbe.pclf.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mio.util.copyText
import com.mio.util.loginStageText
import com.mio.util.openLink
import com.mio.util.openLinkWithBuiltinWebView
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.game.OAuthServer
import com.tungsten.fcl.game.TexturesLoader
import com.tungsten.fcl.setting.Accounts
import com.tungsten.fcl.setting.ConfigHolder.config
import com.tungsten.fcl.ui.account.MicrosoftAccountSkinDialog
import com.tungsten.fcl.ui.account.OfflineAccountSkinDialog
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.auth.Account
import com.tungsten.fclcore.auth.AccountFactory
import com.tungsten.fclcore.auth.CharacterSelector
import com.tungsten.fclcore.auth.CredentialExpiredException
import com.tungsten.fclcore.auth.NoSelectedCharacterException
import com.tungsten.fclcore.auth.OAuth
import com.tungsten.fclcore.auth.authlibinjector.AuthlibInjectorAccount
import com.tungsten.fclcore.auth.authlibinjector.AuthlibInjectorServer
import com.tungsten.fclcore.auth.microsoft.MicrosoftAccount
import com.tungsten.fclcore.auth.offline.OfflineAccount
import com.tungsten.fclcore.auth.yggdrasil.GameProfile
import com.tungsten.fclcore.auth.yggdrasil.YggdrasilAccount
import com.tungsten.fclcore.auth.yggdrasil.YggdrasilService
import com.tungsten.fclcore.fakefx.beans.InvalidationListener
import com.tungsten.fclcore.fakefx.beans.value.ChangeListener
import com.tungsten.fclcore.task.Schedulers
import com.tungsten.fclcore.task.Task
import com.tungsten.fclcore.task.TaskExecutor
import com.tungsten.fclcore.util.skin.InvalidSkinException
import com.tungsten.fclcore.util.skin.NormalizedSkin
import com.tungsten.fcllibrary.util.ConvertUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LauncherButton
import top.nkbe.pclf.ui.components.SectionCard
import top.nkbe.pclf.ui.components.SidebarNavItem
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.window.WindowDialog
import java.io.File
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.function.Consumer
import javax.net.ssl.SSLException
import kotlin.coroutines.resume

enum class AccountSection {
    SAVED,
    MICROSOFT,
    OFFLINE,
    EXTERNAL
}

private data class CharacterPickRequest(
    val service: YggdrasilService,
    val profiles: List<GameProfile>,
    val onSelect: (GameProfile?) -> Unit
)

/**
 * Pure Compose + Miuix two-pane Account Management page:
 * - Left navigation sidebar: Saved Accounts / Microsoft / Offline / External Auth
 * - Right card canvas: clean single-purpose forms, recommendation banner on Offline tab,
 *   real-time OAuth progress & code card, and Miuix confirmation/character dialogs.
 */
@Composable
fun AccountPage(
    initialSection: AccountSection = AccountSection.SAVED,
    sectionRequestSeq: Int = 0,
    onAccountChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    var section by remember { mutableStateOf(initialSection) }
    var refreshNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(initialSection, sectionRequestSeq) {
        section = initialSection
    }

    var accounts by remember { mutableStateOf(Accounts.getAccounts().toList()) }
    var selectedAccount by remember { mutableStateOf(Accounts.getSelectedAccount()) }

    DisposableEffect(Unit) {
        val listener = InvalidationListener {
            accounts = Accounts.getAccounts().toList()
            selectedAccount = Accounts.getSelectedAccount()
        }
        Accounts.getAccounts().addListener(listener)
        Accounts.selectedAccountProperty().addListener(listener)
        onDispose {
            Accounts.getAccounts().removeListener(listener)
            Accounts.selectedAccountProperty().removeListener(listener)
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .background(colors.sidebarBg)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Left))
                .width(172.dp)
                .fillMaxHeight()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SidebarNavItem(
                label = stringResource(R.string.acct_tab_saved),
                selected = section == AccountSection.SAVED,
                onClick = { section = AccountSection.SAVED }
            )
            SidebarNavItem(
                label = stringResource(R.string.acct_tab_microsoft),
                selected = section == AccountSection.MICROSOFT,
                onClick = { section = AccountSection.MICROSOFT }
            )
            SidebarNavItem(
                label = stringResource(R.string.acct_tab_offline),
                selected = section == AccountSection.OFFLINE,
                onClick = { section = AccountSection.OFFLINE }
            )
            SidebarNavItem(
                label = stringResource(R.string.acct_tab_external),
                selected = section == AccountSection.EXTERNAL,
                onClick = { section = AccountSection.EXTERNAL }
            )
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

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.widthIn(max = 660.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (section) {
                    AccountSection.SAVED -> SavedAccountsSection(
                        accounts = accounts,
                        selectedAccount = selectedAccount,
                        avatarNonce = refreshNonce,
                        onSelectAccount = {
                            Accounts.setSelectedAccount(it)
                            onAccountChanged()
                        },
                        onGoToSection = { section = it },
                        onAvatarUpdated = {
                            refreshNonce++
                            onAccountChanged()
                        }
                    )

                    AccountSection.MICROSOFT -> MicrosoftLoginSection(
                        onSuccess = {
                            refreshNonce++
                            onAccountChanged()
                            section = AccountSection.SAVED
                        }
                    )

                    AccountSection.OFFLINE -> OfflineCreateSection(
                        onRecommendMicrosoft = { section = AccountSection.MICROSOFT },
                        onSuccess = {
                            refreshNonce++
                            onAccountChanged()
                            section = AccountSection.SAVED
                        }
                    )

                    AccountSection.EXTERNAL -> ExternalLoginSection(
                        onSuccess = {
                            refreshNonce++
                            onAccountChanged()
                            section = AccountSection.SAVED
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedAccountsSection(
    accounts: List<Account>,
    selectedAccount: Account?,
    avatarNonce: Int,
    onSelectAccount: (Account) -> Unit,
    onGoToSection: (AccountSection) -> Unit,
    onAvatarUpdated: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()
    var busyAccount by remember { mutableStateOf<Account?>(null) }
    var deleteCandidate by remember { mutableStateOf<Account?>(null) }

    if (accounts.isEmpty()) {
        SectionCard(title = stringResource(R.string.acct_tab_saved)) {
            Text(
                text = stringResource(R.string.acct_saved_empty),
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LauncherButton(
                    text = stringResource(R.string.acct_go_microsoft),
                    accentStyle = true,
                    onClick = { onGoToSection(AccountSection.MICROSOFT) }
                )
                LauncherButton(
                    text = stringResource(R.string.acct_tab_offline),
                    onClick = { onGoToSection(AccountSection.OFFLINE) }
                )
                LauncherButton(
                    text = stringResource(R.string.acct_tab_external),
                    onClick = { onGoToSection(AccountSection.EXTERNAL) }
                )
            }
        }
        return
    }

    accounts.forEach { account ->
        val isActive = account == selectedAccount
        val isBusy = busyAccount == account
        val typeLabel = Accounts.getLocalizedLoginTypeName(context, Accounts.getAccountFactory(account))
        val subtitle = if (account is AuthlibInjectorAccount) {
            "$typeLabel · ${account.server.name}"
        } else if (account !is OfflineAccount && account.username.isNotBlank()) {
            "$typeLabel · ${account.username}"
        } else {
            typeLabel
        }

        val avatarBmp = rememberAccountAvatar(account, avatarNonce)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isActive) 1.5.dp else 1.dp,
                    color = if (isActive) colors.accent else colors.cardBorder,
                    shape = RoundedCornerShape(10.dp)
                ),
            cornerRadius = 10.dp,
            insideMargin = PaddingValues(14.dp),
            colors = CardDefaults.defaultColors(
                color = colors.cardBg,
                contentColor = colors.textPrimary
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.accent.copy(alpha = 0.12f))
                ) {
                    if (avatarBmp != null) {
                        Image(
                            bitmap = avatarBmp,
                            contentDescription = account.character,
                            contentScale = ContentScale.Crop,
                            filterQuality = FilterQuality.None,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = account.character.take(1).uppercase(),
                            color = colors.accent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = account.character,
                            color = colors.textPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.accent.copy(alpha = 0.14f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.acct_current),
                                    color = colors.accent,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isActive) {
                    LauncherButton(
                        text = stringResource(R.string.acct_use),
                        accentStyle = true,
                        enabled = !isBusy,
                        height = 30.dp,
                        onClick = { onSelectAccount(account) }
                    )
                }
                LauncherButton(
                    text = stringResource(R.string.acct_refresh),
                    enabled = !isBusy,
                    height = 30.dp,
                    onClick = {
                        busyAccount = account
                        scope.launch {
                            val err = withContext(Dispatchers.IO) {
                                runCatching {
                                    account.clearCache()
                                    account.logIn()
                                }.exceptionOrNull()
                            }
                            busyAccount = null
                            if (err == null) {
                                account.clearCache()
                                MainActivity.getInstance().refreshAvatar(account)
                                onAvatarUpdated()
                                Toast.makeText(context, R.string.acct_refresh_done, Toast.LENGTH_SHORT).show()
                            } else if (err is CredentialExpiredException) {
                                Toast.makeText(context, R.string.acct_relogin_required, Toast.LENGTH_LONG).show()
                                when (account) {
                                    is MicrosoftAccount -> onGoToSection(AccountSection.MICROSOFT)
                                    is AuthlibInjectorAccount -> onGoToSection(AccountSection.EXTERNAL)
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    Accounts.localizeErrorMessage(context, (err as? Exception) ?: Exception(err)),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
                LauncherButton(
                    text = stringResource(R.string.acct_skin),
                    enabled = !isBusy,
                    height = 30.dp,
                    onClick = {
                        when (account) {
                            is OfflineAccount -> OfflineAccountSkinDialog(context, account) {
                                onAvatarUpdated()
                            }.show()

                            is MicrosoftAccount -> MicrosoftAccountSkinDialog(context, account) {
                                onAvatarUpdated()
                            }.show()

                            is YggdrasilAccount -> {
                                scope.launch {
                                    val selectedFile = suspendCancellableCoroutine<String?> { cont ->
                                        MainActivity.getInstance().fileLauncher.launchSingleSelection(
                                            null,
                                            listOf(".png")
                                        ) {
                                            cont.resume(
                                                it?.getOrNull(0)
                                                    ?.toFile(context, File(FCLPath.CACHE_DIR))
                                                    ?.absolutePath
                                            )
                                        }
                                    } ?: return@launch

                                    busyAccount = account
                                    val uploadErr = withContext(Dispatchers.IO) {
                                        runCatching {
                                            account.clearCache()
                                            account.logIn()
                                            val img = BitmapFactory.decodeFile(selectedFile)
                                                ?: throw InvalidSkinException("Failed to read skin image")
                                            val skin = NormalizedSkin(img)
                                            val model = if (skin.isSlim) "slim" else ""
                                            account.uploadSkin(model, File(selectedFile).toPath())
                                            account.clearCache()
                                            account.logIn()
                                        }.exceptionOrNull()
                                    }
                                    busyAccount = null
                                    if (uploadErr == null) {
                                        MainActivity.getInstance().refreshAvatar(account)
                                        onAvatarUpdated()
                                        Toast.makeText(context, R.string.account_skin_uploaded, Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            Accounts.localizeErrorMessage(context, (uploadErr as? Exception) ?: Exception(uploadErr)),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                )
                LauncherButton(
                    text = stringResource(R.string.acct_delete),
                    enabled = !isBusy,
                    height = 30.dp,
                    onClick = { deleteCandidate = account }
                )
            }
        }
    }

    val targetDelete = deleteCandidate
    WindowDialog(
        show = targetDelete != null,
        title = stringResource(R.string.acct_delete),
        summary = targetDelete?.let { stringResource(R.string.acct_delete_confirm, it.character) },
        onDismissRequest = { deleteCandidate = null }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(
                text = stringResource(R.string.dialog_negative),
                onClick = { deleteCandidate = null },
                modifier = Modifier.weight(1f)
            )
            TextButton(
                text = stringResource(R.string.dialog_positive),
                onClick = {
                    targetDelete?.let { acc ->
                        if (acc is OfflineAccount) {
                            File(FCLPath.SKIN_DIR, "${acc.uuid}.png").delete()
                            File(FCLPath.SKIN_DIR, "${acc.uuid}_cape.png").delete()
                        }
                        Accounts.getAccounts().remove(acc)
                        onAvatarUpdated()
                    }
                    deleteCandidate = null
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MicrosoftLoginSection(
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors

    var isLoggingIn by remember { mutableStateOf(false) }
    var stageMessage by remember { mutableStateOf<String?>(null) }
    var deviceCode by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var useExternalBrowser by remember { mutableStateOf(false) }
    var activeTask by remember { mutableStateOf<TaskExecutor?>(null) }

    DisposableEffect(Unit) {
        val codeConsumer = Consumer<OAuthServer.GrantDeviceCodeEvent?> { event ->
            event?.let { ev ->
                Schedulers.androidUIThread().execute {
                    deviceCode = ev.userCode
                    copyText(context, ev.userCode)
                    stageMessage = context.getString(R.string.login_state_microsoft_wait_browser)
                }
            }
        }
        val browserConsumer = Consumer<OAuthServer.OpenBrowserEvent> { event ->
            if (useExternalBrowser) {
                openLink(context, event.url)
            } else {
                openLinkWithBuiltinWebView(context, event.url)
            }
        }
        Accounts.OAUTH_CALLBACK.onGrantDeviceCode.register(codeConsumer)
        Accounts.OAUTH_CALLBACK.onOpenBrowser.register(browserConsumer)

        onDispose {
            Accounts.OAUTH_CALLBACK.onGrantDeviceCode.unregister(codeConsumer)
            Accounts.OAUTH_CALLBACK.onOpenBrowser.unregister(browserConsumer)
            activeTask?.let {
                OAuth.IS_CANCELED = true
                it.cancel()
            }
        }
    }

    fun startLogin(externalBrowser: Boolean) {
        useExternalBrowser = externalBrowser
        isLoggingIn = true
        errorMessage = null
        deviceCode = null
        stageMessage = context.getString(R.string.launch_state_logging_in)

        val progressCb = AccountFactory.ProgressCallback { stageName ->
            Schedulers.androidUIThread().execute {
                stageMessage = loginStageText(context, stageName)
            }
        }

        activeTask = Task.supplyAsync {
            val selector = CharacterSelector { _, profiles -> profiles.firstOrNull() ?: throw NoSelectedCharacterException() }
            Accounts.FACTORY_MICROSOFT.create(selector, null, null, progressCb, null) as Account
        }.whenComplete(Schedulers.androidUIThread()) { account, exception ->
            isLoggingIn = false
            activeTask = null
            Accounts.OAUTH_CALLBACK.onLoginFinished.fireEvent(OAuthServer.LoginFinishedEvent(context))
            if (exception == null && account != null) {
                Accounts.addAccount(account)
                Accounts.setSelectedAccount(account)
                onSuccess()
            } else if (exception !is NoSelectedCharacterException && exception !is CancellationException) {
                errorMessage = Accounts.localizeErrorMessage(context, exception)
            }
        }.executor(true)
    }

    SectionCard(title = stringResource(R.string.acct_tab_microsoft)) {
        Text(
            text = stringResource(R.string.account_methods_microsoft_hint),
            color = colors.textSecondary,
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )

        val code = deviceCode
        if (!code.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.accent.copy(alpha = 0.09f))
                    .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.acct_ms_device_code),
                        color = colors.textSecondary,
                        fontSize = 11.5.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = code,
                        color = colors.accent,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                LauncherButton(
                    text = stringResource(R.string.acct_copy_code),
                    onClick = {
                        copyText(context, code)
                        Toast.makeText(context, R.string.acct_code_copied, Toast.LENGTH_SHORT).show()
                    },
                    height = 32.dp
                )
            }
        }

        if (isLoggingIn) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(
                text = stageMessage ?: stringResource(R.string.launch_state_logging_in),
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }

        val err = errorMessage
        if (!err.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1AE53935))
                    .border(1.dp, Color(0x55E53935), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = err,
                    color = Color(0xFFD32F2F),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoggingIn) {
                LauncherButton(
                    text = stringResource(R.string.pclf_cancel),
                    onClick = {
                        OAuth.IS_CANCELED = true
                        activeTask?.cancel()
                        isLoggingIn = false
                    },
                    height = 36.dp
                )
            } else {
                LauncherButton(
                    text = stringResource(R.string.acct_ms_builtin_btn),
                    accentStyle = true,
                    onClick = { startLogin(externalBrowser = false) },
                    height = 36.dp
                )
                LauncherButton(
                    text = stringResource(R.string.acct_ms_browser_btn),
                    onClick = { startLogin(externalBrowser = true) },
                    height = 36.dp
                )
            }
        }
    }
}

@Composable
private fun OfflineCreateSection(
    onRecommendMicrosoft: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    val trimmed = username.trim()
    val isStandardName = trimmed.isEmpty() ||
        (trimmed.length <= 16 && Regex("^[A-Za-z0-9_]+$").matches(trimmed))

    // Non-blocking official Microsoft account recommendation banner
    SectionCard(
        title = stringResource(R.string.acct_ms_recommend_title),
        trailingAction = {
            LauncherButton(
                text = stringResource(R.string.acct_go_microsoft),
                accentStyle = true,
                height = 28.dp,
                onClick = onRecommendMicrosoft
            )
        }
    ) {
        Text(
            text = stringResource(R.string.acct_ms_recommend_body),
            color = colors.textSecondary,
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )
    }

    SectionCard(title = stringResource(R.string.account_create_offline)) {
        TextField(
            value = username,
            onValueChange = { username = it },
            label = stringResource(R.string.acct_offline_name_hint),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (!isStandardName) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.acct_offline_name_warn),
                color = Color(0xFFE65100),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }

        Spacer(Modifier.height(14.dp))
        LauncherButton(
            text = stringResource(R.string.pclf_add_offline),
            accentStyle = true,
            enabled = trimmed.isNotEmpty() && !isCreating,
            height = 36.dp,
            onClick = {
                isCreating = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            val selector = CharacterSelector { _, profiles ->
                                profiles.firstOrNull() ?: throw NoSelectedCharacterException()
                            }
                            Accounts.FACTORY_OFFLINE.create(selector, trimmed, null, null, null) as Account
                        }
                    }
                    isCreating = false
                    result.onSuccess { acc ->
                        Accounts.addAccount(acc)
                        Accounts.setSelectedAccount(acc)
                        onSuccess()
                    }.onFailure { err ->
                        Toast.makeText(
                            context,
                            Accounts.localizeErrorMessage(context, (err as? Exception) ?: Exception(err)),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun ExternalLoginSection(
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()

    var servers by remember { mutableStateOf(config().authlibInjectorServers.orEmpty().toList()) }
    var selectedServer by remember { mutableStateOf(servers.firstOrNull()) }

    DisposableEffect(Unit) {
        val list = config().authlibInjectorServers
        val listener = InvalidationListener {
            val updated = list.orEmpty().toList()
            servers = updated
            if (selectedServer !in updated) {
                selectedServer = updated.firstOrNull()
            }
        }
        list.addListener(listener)
        onDispose { list.removeListener(listener) }
    }

    var showAddServerDialog by remember { mutableStateOf(false) }
    var deleteServerCandidate by remember { mutableStateOf<AuthlibInjectorServer?>(null) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var characterPickRequest by remember { mutableStateOf<CharacterPickRequest?>(null) }

    SectionCard(
        title = stringResource(R.string.acct_external_servers),
        trailingAction = {
            LauncherButton(
                text = "+ ${stringResource(R.string.account_add_server)}",
                height = 28.dp,
                onClick = { showAddServerDialog = true }
            )
        }
    ) {
        if (servers.isEmpty()) {
            Text(
                text = stringResource(R.string.acct_external_no_server),
                color = colors.textSecondary,
                fontSize = 12.5.sp
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                servers.forEach { server ->
                    val active = server == selectedServer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) colors.accent.copy(alpha = 0.08f) else Color.Transparent)
                            .border(
                                width = if (active) 1.5.dp else 1.dp,
                                color = if (active) colors.accent else colors.cardBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedServer = server }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = server.name,
                                color = if (active) colors.accent else colors.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = server.url,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        server.links["homepage"]?.let { url ->
                            Spacer(Modifier.width(6.dp))
                            LauncherButton(
                                text = stringResource(R.string.account_create_home),
                                height = 28.dp,
                                onClick = { openLink(context, url) }
                            )
                        }
                        server.links["register"]?.let { url ->
                            Spacer(Modifier.width(6.dp))
                            LauncherButton(
                                text = stringResource(R.string.account_create_register),
                                height = 28.dp,
                                onClick = { openLink(context, url) }
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        LauncherButton(
                            text = stringResource(R.string.acct_delete),
                            height = 28.dp,
                            onClick = { deleteServerCandidate = server }
                        )
                    }
                }
            }
        }
    }

    val activeServer = selectedServer
    SectionCard(title = stringResource(R.string.account_create_external)) {
        if (activeServer == null) {
            Text(
                text = stringResource(R.string.account_create_server_not_select),
                color = colors.textSecondary,
                fontSize = 12.5.sp
            )
        } else {
            Text(
                text = "${stringResource(R.string.account_injector_server)}: ${activeServer.name}",
                color = colors.accent,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            TextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.account_create_username),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.account_create_password),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    Text(
                        text = if (showPassword) "◡" else "◎",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { showPassword = !showPassword }
                            .padding(horizontal = 8.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            val err = errorMessage
            if (!err.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = err,
                    color = Color(0xFFD32F2F),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(Modifier.height(14.dp))
            LauncherButton(
                text = stringResource(R.string.account_login),
                accentStyle = true,
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoggingIn,
                height = 36.dp,
                onClick = {
                    isLoggingIn = true
                    errorMessage = null
                    scope.launch {
                        val factory = Accounts.getAccountFactoryByAuthlibInjectorServer(activeServer)
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                val selector = CharacterSelector { service, profiles ->
                                    val latch = CountDownLatch(1)
                                    var picked: GameProfile? = null
                                    Schedulers.androidUIThread().execute {
                                        characterPickRequest = CharacterPickRequest(service, profiles) { choice ->
                                            picked = choice
                                            characterPickRequest = null
                                            latch.countDown()
                                        }
                                    }
                                    latch.await()
                                    picked ?: throw NoSelectedCharacterException()
                                }
                                factory.create(selector, email.trim(), password, null, activeServer) as Account
                            }
                        }
                        isLoggingIn = false
                        result.onSuccess { acc ->
                            Accounts.addAccount(acc)
                            Accounts.setSelectedAccount(acc)
                            onSuccess()
                        }.onFailure { ex ->
                            if (ex !is NoSelectedCharacterException && ex !is CancellationException) {
                                errorMessage = Accounts.localizeErrorMessage(context, (ex as? Exception) ?: Exception(ex))
                            }
                        }
                    }
                }
            )
        }
    }

    if (showAddServerDialog) {
        AddExternalServerDialog(
            onDismiss = { showAddServerDialog = false },
            onAdded = { server ->
                if (!config().authlibInjectorServers.contains(server)) {
                    config().authlibInjectorServers.add(server)
                }
                selectedServer = server
                showAddServerDialog = false
            }
        )
    }

    val delServer = deleteServerCandidate
    WindowDialog(
        show = delServer != null,
        title = stringResource(R.string.acct_delete),
        summary = delServer?.let { stringResource(R.string.version_manage_remove_confirm, it.name) },
        onDismissRequest = { deleteServerCandidate = null }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(
                text = stringResource(R.string.dialog_negative),
                onClick = { deleteServerCandidate = null },
                modifier = Modifier.weight(1f)
            )
            TextButton(
                text = stringResource(R.string.dialog_positive),
                onClick = {
                    delServer?.let { config().authlibInjectorServers.remove(it) }
                    deleteServerCandidate = null
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    val pickReq = characterPickRequest
    WindowDialog(
        show = pickReq != null,
        title = stringResource(R.string.account_select_character),
        onDismissRequest = { pickReq?.onSelect(null) }
    ) {
        if (pickReq != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pickReq.profiles.forEach { profile ->
                    val profileAvatar = rememberProfileAvatar(pickReq.service, profile)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                            .clickable { pickReq.onSelect(profile) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.accent.copy(alpha = 0.12f))
                        ) {
                            if (profileAvatar != null) {
                                Image(
                                    bitmap = profileAvatar,
                                    contentDescription = profile.name,
                                    filterQuality = FilterQuality.None,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = profile.name,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    text = stringResource(R.string.dialog_negative),
                    onClick = { pickReq.onSelect(null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AddExternalServerDialog(
    onDismiss: () -> Unit,
    onAdded: (AuthlibInjectorServer) -> Unit
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    val scope = rememberCoroutineScope()

    var urlInput by remember { mutableStateOf("") }
    var resolvedServer by remember { mutableStateOf<AuthlibInjectorServer?>(null) }
    var isResolving by remember { mutableStateOf(false) }
    var resolveError by remember { mutableStateOf<String?>(null) }

    WindowDialog(
        show = true,
        title = stringResource(R.string.account_add_server),
        onDismissRequest = { if (!isResolving) onDismiss() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (resolvedServer == null) {
                TextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = stringResource(R.string.acct_external_add_server_hint),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                resolveError?.let { err ->
                    Text(
                        text = err,
                        color = Color(0xFFD32F2F),
                        fontSize = 12.sp
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        text = stringResource(R.string.dialog_negative),
                        enabled = !isResolving,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = stringResource(R.string.acct_external_resolve),
                        enabled = urlInput.isNotBlank() && !isResolving,
                        onClick = {
                            isResolving = true
                            resolveError = null
                            scope.launch {
                                val res = withContext(Dispatchers.IO) {
                                    runCatching { AuthlibInjectorServer.locateServer(urlInput.trim()) }
                                }
                                isResolving = false
                                res.onSuccess { srv ->
                                    resolvedServer = srv
                                }.onFailure { ex ->
                                    resolveError = when (ex) {
                                        is SSLException -> context.getString(R.string.account_failed_ssl)
                                        is IOException -> context.getString(R.string.account_failed_connect_injector_server)
                                        else -> "${ex.javaClass.simpleName}: ${ex.localizedMessage}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                val server = resolvedServer!!
                Text(
                    text = "${stringResource(R.string.account_add_server_name)}: ${server.name}",
                    color = colors.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${stringResource(R.string.account_add_server_url)}: ${server.url}",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        text = stringResource(R.string.dialog_negative),
                        onClick = { resolvedServer = null },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = stringResource(R.string.acct_external_confirm_add),
                        onClick = { onAdded(server) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberAccountAvatar(account: Account, nonce: Int): ImageBitmap? {
    val context = LocalContext.current
    var bmp by remember(account, nonce) { mutableStateOf<ImageBitmap?>(null) }
    DisposableEffect(account, nonce) {
        val px = ConvertUtils.dip2px(context, 46f)
        val binding = TexturesLoader.avatarBinding(account, px)
        bmp = binding.get()?.bitmap?.asImageBitmap()
        val listener = ChangeListener<android.graphics.drawable.BitmapDrawable> { _, _, newVal ->
            bmp = newVal?.bitmap?.asImageBitmap()
        }
        binding.addListener(listener)
        onDispose {
            binding.removeListener(listener)
            binding.dispose()
        }
    }
    return bmp
}

@Composable
private fun rememberProfileAvatar(service: YggdrasilService, profile: GameProfile): ImageBitmap? {
    val context = LocalContext.current
    var bmp by remember(service, profile.id) { mutableStateOf<ImageBitmap?>(null) }
    DisposableEffect(service, profile.id) {
        val px = ConvertUtils.dip2px(context, 30f)
        val binding = TexturesLoader.avatarBinding(service, profile.id, px)
        bmp = binding.get()?.bitmap?.asImageBitmap()
        val listener = ChangeListener<android.graphics.drawable.BitmapDrawable> { _, _, newVal ->
            bmp = newVal?.bitmap?.asImageBitmap()
        }
        binding.addListener(listener)
        onDispose {
            binding.removeListener(listener)
            binding.dispose()
        }
    }
    return bmp
}
