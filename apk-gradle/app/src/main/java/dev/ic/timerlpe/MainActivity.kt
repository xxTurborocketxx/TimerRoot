package dev.ic.timerlpe

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.NavigationBar
import io.github.suqi8.coui.kmp.basic.NavigationBarItem
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.TextField
import io.github.suqi8.coui.kmp.basic.TextFieldMode
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.TopAppBar
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurColors
import io.github.suqi8.coui.kmp.blur.LayerBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.blur.textureBlur
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Back
import io.github.suqi8.coui.kmp.icon.extended.Notes
import io.github.suqi8.coui.kmp.overlay.OverlayDialog
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import io.github.suqi8.coui.kmp.preference.SwitchPreference
import io.github.suqi8.coui.kmp.preference.WindowDropdownPreference
import io.github.suqi8.coui.kmp.shader.isRenderEffectSupported
import io.github.suqi8.coui.kmp.theme.ColorSchemeMode
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.ThemeController
import io.github.suqi8.coui.kmp.utils.PressFeedbackType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.channels.FileChannel

private const val PROJECT_URL = "https://github.com/xxTurborocketxx/TimerRoot"
private const val XDA_URL = "https://xdaforums.com/t/tool-timerroot-one-tap-root-for-motorola-edge-50-ultra-no-bootloader-unlock-vivo-iqoo.4808957/"

private data class LicenseText(
    val name: String,
    val url: String? = null,
    val asset: String? = null,
)

private data class LicenseItem(
    val name: String,
    val author: String? = null,
    val website: String? = null,
    val licenses: List<LicenseText> = emptyList(),
)

private enum class AboutPage { HOME, LICENSES }

class MainActivity : ComponentActivity() {
    private val logs = mutableStateListOf<String>()
    private val timeFmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)

    private var busy by mutableStateOf(false)
    private var authorized by mutableStateOf(false)
    private var binderReady by mutableStateOf(false)
    private var bound by mutableStateOf(false)
    private var bindingInFlight = false
    private var kernelVersion by mutableStateOf("Unknown")
    private var deviceModel by mutableStateOf("Unknown")
    private var selinuxText by mutableStateOf("Unknown")
    private var ksuIndex by mutableStateOf(0)
    private var safeMode by mutableStateOf(false)
    private var tab by mutableStateOf(AppTab.HOME)
    private var showManual by mutableStateOf(false)
    private var runStage by mutableStateOf("")
    private var runStageIndex = -1
    /* True once the core log proves root (LPE-PROOF / module resident).
     * The core can still exit 1 when only the final "am start" of the
     * manager fails - that is success, not a failed run. */
    private var rootConfirmed = false
    private var offsetPrompt by mutableStateOf<String?>(null)
    private var loadPrompt by mutableStateOf<String?>(null)
    private var missPrompt by mutableStateOf(false)
    /* v25: out-of-scope confirm. The startup device notice stays purely
     * informational; the Run button additionally asks for one explicit
     * confirmation per session before the core starts on a device or
     * kernel that is not known-supported. */
    private var scopePending by mutableStateOf(false)
    private var scopeReasons by mutableStateOf("")
    private var scopeConfirmed by mutableStateOf(false)
    private var scopePrompt by mutableStateOf<String?>(null)
    private var missHold = false
    private var rerunPrompt by mutableStateOf(false)
    /* v44: prompt state for the post-root soft reboot (framework restart). */
    private var softRebootPrompt by mutableStateOf(false)
    /* v52: Stage 1 Shizuku onboarding - a guided dialog, not an own
     * adb client (that later stage changes the binding design first). */
    private var shizukuSetupPrompt by mutableStateOf(false)
    /* v48: one-shot guard for the legacy root-state recovery probe. */
    private var recoveryBusy = false
    /* v4: Compose-visible mirror of the persisted run flag. A
     * SharedPreferences read is not tracked state, so the status
     * card stayed stale until the app restarted; the mirror is
     * refreshed at every entry point (resume, run start, run end). */
    private var rootedNow by mutableStateOf(false)
    /* v4: one-shot guards so auto-opening never fights the user. */
    private var shizukuHintShown = false
    private var managerOpenedThisRun = false
    private var runStartMs = 0L

    /* Wall-clock stamps let a saved log read as a timeline; the stamp
     * is a display prefix only - the stage parsers and the outcome
     * tracking still see the raw line. */
    private val logTimeFmt =
        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.ROOT)

    private fun stampTime(line: String): String =
        logTimeFmt.format(java.util.Date()) + " " + line
    private var notice by mutableStateOf<Notice?>(null)
    private var parseMenu by mutableStateOf(false)
    private var otaUrlPrompt by mutableStateOf(false)
    private var otaUrlText by mutableStateOf("")
    private var showDonate by mutableStateOf(false)

    private var service: IIcService? = null
    private var logPath: String? = null
    private var logOff = 0
    private var polling = false
    private var runStamp = ""
    /** Which step of the boot.img parser was running when it failed. */
    private var parseStage = "Reading file"

    private val prefs by lazy { getSharedPreferences("ic_timer", MODE_PRIVATE) }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == REQ_SHIZUKU) {
            authorized = result == PackageManager.PERMISSION_GRANTED
            addLog(
                if (authorized) "Shizuku permission: granted" else "Shizuku permission: denied($result)"
            )
            if (authorized) bindUserService()
            refreshAuth()
        }
    }

    private val binderListener = Shizuku.OnBinderReceivedListener {
        binderReady = Shizuku.pingBinder()
        refreshAuth()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        binderReady = false
        bound = false
        service = null
        bindingInFlight = false
        refreshAuth()
    }

    private val parsePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            addLog("[parse offsets] cancelled")
            return@registerForActivityResult
        }
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        parseSelected(uri)
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IIcService.Stub.asInterface(binder)
            bound = true
            bindingInFlight = false
            lifecycleScope.launch {
                val id = withContext(Dispatchers.IO) {
                    runCatching { service?.identity() }.getOrNull() ?: "Failed to read identity"
                }
                val dev = withContext(Dispatchers.IO) {
                    runCatching {
                        service?.runCmd(
                            "getprop ro.product.model; " +
                                "getprop ro.build.display.id; uname -r"
                        )
                    }.getOrNull() ?: ""
                }
                addLog("[service] $id")
                if (dev.isNotBlank()) addLog("[device] $dev")
                refreshStaticState()
                recoverRootState()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)
        /* libextract.so is staged by the installer in nativeLibraryDir; it is
         * executed (not dlopen'd) for REPLACE_ZSTD payload operations. */
        runCatching { ZstdNative.init(File(applicationInfo.nativeLibraryDir), cacheDir) }
        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        binderReady = Shizuku.pingBinder()
        refreshAuth()
        ksuIndex = prefs.getInt("ksu_index", 0).coerceIn(0, KSU_NAMES.lastIndex)
        safeMode = prefs.getBoolean("safe_mode", false)
        betaChannel = prefs.getBoolean("beta_channel", false)
        kernelVersion = runCatching { android.system.Os.uname().release }.getOrDefault("Unknown")
        deviceModel = resolveDeviceName()
        refreshStaticState()
        checkDeviceAndKernel()
        if (authorized && binderReady) bindUserService()
        /* v15: silent update check against the latest GitHub release; the
         * dialog only appears for a newer version that was not skipped. */
        lifecycleScope.launch(Dispatchers.IO) {
            val upd = runCatching { Updater.fetchLatest(betaChannel) }.getOrNull()
            if (upd != null) postUi {
                latestSeen = upd.version
                latestPrerelease = upd.prerelease
                if (upd.version == BuildConfig.VERSION_NAME) installedPrerelease = upd.prerelease
                latestNotes = upd.notes
                if (Updater.isNewer(BuildConfig.VERSION_NAME, upd.version) &&
                    prefs.getString("update_skip_tag", null) != upd.tag
                ) updatePrompt = upd
            }
        }

        setContent {
            val themeController = remember { ThemeController(colorSchemeMode = ColorSchemeMode.System) }
            COUITheme(themeController) {
                val appBackdrop = rememberAppBackdrop()
                Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = {
                                val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                                (slideInHorizontally(tween(240)) { it / 4 * dir } + fadeIn(tween(240))) togetherWith
                                    (slideOutHorizontally(tween(240)) { -it / 4 * dir } + fadeOut(tween(240)))
                            },
                            label = "tab",
                            modifier = Modifier.fillMaxSize()
                        ) { t ->
                            when (t) {
                                AppTab.LOGS -> LogScreen(state = logs.toList(), backdrop = appBackdrop)
                                AppTab.ABOUT -> AboutScreen(backdrop = appBackdrop)
                                AppTab.HOME -> HomeScreen(backdrop = appBackdrop)
                            }
                        }
                        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                            BlurredBar(backdrop = appBackdrop) {
                                BottomBar(selected = tab, onSelect = { tab = it })
                            }
                        }
                        CleanupDialog()
                        OffsetPromptDialog()
                        LoadPromptDialog()
                        ScopePromptDialog()
                        MissPromptDialog()
                        SoftRebootPromptDialog()
                        ShizukuSetupDialog()
                        RerunPromptDialog()
                        ParseMenuDialog()
                        OtaUrlDialog()
                        NoticeDialog()
                        UpdateDialog()
                        UpdateMismatchDialog()
                        ChangelogDialog()
                        DonateDialog(show = showDonate, onDismiss = { showDonate = false })
                        AnimatedVisibility(
                            visible = showManual,
                            enter = slideInHorizontally(tween(240)) { it } + fadeIn(tween(240)),
                            exit = slideOutHorizontally(tween(240)) { it } + fadeOut(tween(240))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                ManualScreen(onBack = { showManual = false })
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAuth()
        if (authorized && !bound) bindUserService()
        refreshStaticState()
        refreshRunDone()
        if (bound) recoverRootState()
        /* v44: greet the user once per boot after a soft reboot; the
         * marker survives the app process being killed by the framework
         * restart, so the note cannot live in doSoftReboot alone. */
        if (prefs.getLong("soft_reboot_id", 0L) == bootId() &&
            prefs.getLong("soft_reboot_logged_id", 0L) != bootId()) {
            prefs.edit().putLong("soft_reboot_logged_id", bootId()).apply()
            addLog("[i] back after a soft reboot - check module status in the manager")
        }
        if (!binderReady && !shizukuHintShown) {
            shizukuHintShown = true
            addLog("[i] Shizuku is not running - open it with the button below; the manual and log tabs work without it")
        }
    }

    /* ------------------------------------------------------------------ */
    /* Home                                                                */
    /* ------------------------------------------------------------------ */

    @Composable
    private fun rememberAppBackdrop(): LayerBackdrop? {
        if (!isRenderEffectSupported()) return null
        val surfaceColor = COUITheme.colorScheme.surface
        return rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }

    @Composable
    private fun BlurredBar(backdrop: LayerBackdrop?, content: @Composable () -> Unit) {
        Box(
            modifier = if (backdrop != null) {
                Modifier.textureBlur(
                    backdrop = backdrop,
                    shape = RectangleShape,
                    blurRadius = 25f,
                    colors = BlurColors(
                        blendColors = listOf(
                            BlendColorEntry(color = COUITheme.colorScheme.surface.copy(0.87f))
                        )
                    )
                )
            } else {
                Modifier
            }
        ) {
            content()
        }
    }

    @Composable
    private fun HomeScreen(backdrop: LayerBackdrop?) {
        val scrollBehavior = COUIScrollBehavior()
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
            topBar = {
                BlurredBar(backdrop = backdrop) {
                    TopAppBar(
                        title = "TimerRoot",
                        scrollBehavior = scrollBehavior,
                        color = Color.Transparent
                    )
                }
            },
            popupHost = { }
        ) { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier
                    )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(
                        top = inner.calculateTopPadding() + 12.dp,
                        bottom = 12.dp + 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        StatusCard(
                            granted = binderReady && authorized && bound,
                            rooted = rootedNow,
                            attempting = busy
                        )
                    }
                    item {
                        if (!binderReady) {
                            TextButton(
                                text = "Open Shizuku",
                                onClick = {
                                    addLog("[shizuku] opening Shizuku - start its server via wireless debugging, then come back")
                                    openManager()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    }
                    item {
                        if (!binderReady) {
                            TextButton(
                                text = "Shizuku setup",
                                onClick = {
                                    addLog("[shizuku] setup dialog opened")
                                    shizukuSetupPrompt = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    }
                    item {
                        TextButton(
                            text = when {
                                busy -> runStage.ifBlank { "Running..." }
                                runStage.isNotEmpty() -> runStage
                                !binderReady -> "Shizuku not running - open it above"
                                !authorized -> "Authorize Shizuku"
                                runDoneThisBoot() -> "Reboot required (already ran)"
                                else -> "One-tap jailbreak"
                            },
                            onClick = {
                                if (busy) return@TextButton
                                when {
                                    !binderReady -> {
                                        addLog("[shizuku] start Shizuku first (wireless debugging / ADB); do not start it as root")
                                        openManager()
                                    }
                                    !authorized -> Shizuku.requestPermission(REQ_SHIZUKU)
                                    else -> if (runDoneThisBoot()) {
                                        addLog("[run] refusing to rerun in this boot - reboot before rerunning (one run per boot)")
                                        rerunPrompt = true
                                    } else maybeWarnThenRun()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (runDoneThisBoot()) ButtonDefaults.textButtonColors(
                                color = COUITheme.colorScheme.errorContainer,
                                textColor = COUITheme.colorScheme.onErrorContainer
                            ) else ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                    item {
                        /* v44: modules late-loaded after boot only hook
                         * processes started afterwards; offer a framework
                         * restart (soft reboot) once root is live. */
                        if (rootedNow) {
                            TextButton(
                                text = "Soft reboot - apply modules",
                                onClick = { softRebootPrompt = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                WindowDropdownPreference(
                                    title = "ROOT manager",
                                    items = KSU_NAMES,
                                    selectedIndex = ksuIndex,
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.Tag,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    },
                                    onSelectedIndexChange = {
                                        ksuIndex = it
                                        prefs.edit().putInt("ksu_index", it).apply()
                                    }
                                )
                                HorizontalDivider()
                                SwitchPreference(
                                    title = "Safe mode",
                                    checked = safeMode,
                                    onCheckedChange = {
                                        safeMode = it
                                        prefs.edit().putBoolean("safe_mode", it).apply()
                                    },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.Security,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                ArrowPreference(
                                    title = "User manual",
                                    summary = "Rules, supported models and FAQ",
                                    onClick = { showManual = true },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                            }
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                ArrowPreference(
                                    title = "Parse offsets",
                                    summary = "Parse offsets from a full package or boot.img",
                                    onClick = { parseOffsets() },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.Tune,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                ArrowPreference(
                                    title = "Clean up",
                                    summary = "Clean this tool's temp files / clean KSU",
                                    onClick = { cleanup() },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.BugReport,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                ArrowPreference(
                                    title = "Diagnostics",
                                    summary = "Print device identity, symbol tables and manager status",
                                    onClick = { diag() },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                            }
                        }
                    }
                    item {
                        InfoCard(
                            kernelVersion = kernelVersion,
                            exploitLabel = "CVE-2026-98260 (POSIX CPU timer UAF)",
                            deviceModel = deviceModel,
                            fingerprint = Build.FINGERPRINT,
                            selinux = selinuxText,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusCard(granted: Boolean, rooted: Boolean, attempting: Boolean) {
        val green = if (isSystemInDarkTheme()) Color(0xFF1A3825) else Color(0xFFDFFAE4)
        val greenTint = Color(0xFF36D167)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = if (granted) {
                CardDefaults.defaultColors(color = green)
            } else {
                CardDefaults.defaultColors()
            },
            showIndication = false,
            pressFeedbackType = PressFeedbackType.Tilt
        ) {
            Box(Modifier.fillMaxWidth().height(140.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(27.dp, 31.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = if (granted) greenTint else Color(0xFF9AA0A6)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp, 14.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column {
                        Text(
                            text = if (!granted) "Shizuku not connected"
                            else if (attempting) "Jailbreak attempt running..."
                            else if (rooted) "Jailbreak active this boot"
                            else "Shizuku connected",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (!granted) "Start and authorize Shizuku via wireless debugging / ADB - the manual and log tabs work without it"
                            else if (attempting) "The exploit is running; the status updates when the run ends"
                            else if (rooted) "Root is live until the next reboot; rerun is blocked"
                            else "Shell user service ready; jailbreak can be attempted",
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun InfoText(
        icon: ImageVector,
        title: String,
        content: String,
        bottomPadding: Dp = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp),
                tint = COUITheme.colorScheme.onSurface
            )
            Column {
                Text(
                    text = title,
                    fontSize = COUITheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = COUITheme.colorScheme.onSurface
                )
                Text(
                    text = content,
                    fontSize = COUITheme.textStyles.body2.fontSize,
                    color = COUITheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }

    @Composable
    private fun InfoCard(
        kernelVersion: String,
        exploitLabel: String,
        deviceModel: String,
        fingerprint: String,
        selinux: String,
        modifier: Modifier = Modifier
    ) {
        Card(modifier = modifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoText(Icons.Filled.DeveloperBoard, "Kernel version", kernelVersion)
                InfoText(Icons.Filled.BugReport, "Exploit method", exploitLabel)
                InfoText(Icons.Filled.Smartphone, "Device model", deviceModel)
                InfoText(Icons.Filled.Fingerprint, "OS fingerprint", fingerprint)
                InfoText(Icons.Filled.Security, "SELinux status", selinux, bottomPadding = 0.dp)
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Logs                                                                */
    /* ------------------------------------------------------------------ */

    @Composable
    private fun LogScreen(state: List<String>, backdrop: LayerBackdrop?) {
        val clipboard = LocalClipboard.current
        val clipScope = rememberCoroutineScope()
        Scaffold(
            topBar = {
                BlurredBar(backdrop = backdrop) {
                    TopAppBar(
                        title = "Log",
                        color = Color.Transparent,
                        navigationIcon = {
                            Row {
                                IconButton(onClick = { shareLog() }) {
                                    Icon(
                                        imageVector = Icons.Filled.IosShare,
                                        contentDescription = "Save and share the log",
                                        tint = COUITheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = { logs.clear() }) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Clear log",
                                        tint = COUITheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    )
                }
            },
            popupHost = { }
        ) { inner ->
            if (state.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(inner),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No log yet",
                        fontSize = 14.sp,
                        color = COUITheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            } else {
                val listState = rememberLazyListState()
                LaunchedEffect(state.size) {
                    /* Follow the tail only when the reader already sits
                     * at the bottom; scrolling up to read stays put. */
                    val info = listState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                    if (info.totalItemsCount == 0 ||
                        last >= info.totalItemsCount - 2
                    ) {
                        listState.scrollToItem(state.lastIndex)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = inner.calculateTopPadding() + 12.dp,
                            bottom = 12.dp + 100.dp,
                            start = 12.dp,
                            end = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(state) { line ->
                            val dark = isSystemInDarkTheme()
                            val contentColor = logLineColor(line, dark)
                            Text(
                                buildAnnotatedString {
                                    if (line.length > 9 &&
                                        line[2] == ':' && line[5] == ':'
                                    ) {
                                        pushStyle(
                                            SpanStyle(
                                                fontSize = 10.sp,
                                                color = contentColor.copy(alpha = 0.55f)
                                            )
                                        )
                                        append(line.substring(0, 8))
                                        append(' ')
                                        pop()
                                        append(line.substring(9))
                                    } else {
                                        append(line)
                                    }
                                },
                                color = contentColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (line.contains("========"))
                                    FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.clickable {
                                    clipScope.launch {
                                        clipboard.setClipEntry(
                                            ClipEntry(ClipData.newPlainText("log", line))
                                        )
                                    }
                                    Toast.makeText(
                                        this@MainActivity, "Copied", Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ManualScreen(onBack: () -> Unit) {
        BackHandler(onBack = onBack)
        val context = LocalContext.current
        val markdown = remember {
            runCatching {
                context.assets.open("manual.md").bufferedReader().use { it.readText() }
            }.getOrElse { "# User Manual\n\nFailed to read the manual." }
        }
        val blocks = remember(markdown) { parseManualMarkdown(markdown) }
        Scaffold(popupHost = { }) { inner ->
            val codeBackground = COUITheme.colorScheme.onSurface.copy(alpha = 0.08f)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = inner.calculateTopPadding() + 16.dp,
                    start = 20.dp,
                    end = 20.dp,
                    bottom = inner.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(blocks) { _, block ->
                    MdBlockView(block, codeBackground)
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* About                                                               */
    /* ------------------------------------------------------------------ */

    @Composable
    private fun AboutScreen(backdrop: LayerBackdrop?) {
        var page by remember { mutableStateOf(AboutPage.HOME) }
        var selected by remember { mutableStateOf<LicenseItem?>(null) }
        BackHandler(enabled = selected != null || page == AboutPage.LICENSES) {
            if (selected != null) selected = null else page = AboutPage.HOME
        }
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState == AboutPage.LICENSES) {
                    (slideInHorizontally(tween(240)) { it } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(240)) { -it / 4 } + fadeOut(tween(240)))
                } else {
                    (slideInHorizontally(tween(240)) { -it / 4 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(240)) { it } + fadeOut(tween(240)))
                }
            },
            label = "aboutPage"
        ) { current ->
            if (current == AboutPage.HOME) {
                AboutHomeScreen(
                    backdrop = backdrop,
                    onLicenses = { page = AboutPage.LICENSES }
                )
            } else {
                LicenseListScreen(
                    backdrop = backdrop,
                    onBack = { page = AboutPage.HOME },
                    onSelect = { selected = it }
                )
            }
        }
        LicenseDetailDialog(selected, onDismiss = { selected = null })
    }

    @Composable
    private fun AboutHomeScreen(
        backdrop: LayerBackdrop?,
        onLicenses: () -> Unit
    ) {
        val uriHandler = LocalUriHandler.current
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
            topBar = {
                BlurredBar(backdrop = backdrop) {
                    TopAppBar(title = "About", color = Color.Transparent)
                }
            },
            popupHost = { }
        ) { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = inner.calculateTopPadding() + 12.dp,
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 12.dp + 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "TimerRoot",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                    fontSize = 13.sp,
                                    color = COUITheme.colorScheme.onSurfaceVariantSummary
                                )
                                Text(
                                    text = "This software is free and open source",
                                    fontSize = 13.sp,
                                    color = COUITheme.colorScheme.onSurfaceVariantSummary
                                )
                                Text(
                                    text = "by Coolapk @散花神木 (Sanhua Shenmu)",
                                    fontSize = 13.sp,
                                    color = COUITheme.colorScheme.onSurfaceVariantSummary
                                )
                                Text(
                                    text = "Edge 50 Ultra port by TurboRCT",
                                    fontSize = 13.sp,
                                    color = COUITheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                ArrowPreference(
                                    title = "View source code",
                                    summary = "View the source on GitHub",
                                    onClick = { uriHandler.openUri(PROJECT_URL) },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.Code,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                ArrowPreference(
                                    title = "XDA thread",
                                    summary = "Discussion, support and feedback",
                                    onClick = { uriHandler.openUri(XDA_URL) },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.Group,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                ArrowPreference(
                                    title = "Share a device report",
                                    summary = "For support requests in the XDA thread",
                                    onClick = { shareDeviceReport() },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.IosShare,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                ArrowPreference(
                                    title = "Feedback",
                                    summary = "Report issues on GitHub Issues",
                                    onClick = { uriHandler.openUri("$PROJECT_URL/issues") },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.BugReport,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                ArrowPreference(
                                    title = "Open source licenses",
                                    summary = "Licenses of this project and the open-source projects it references",
                                    onClick = onLicenses,
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.Copyright,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                ArrowPreference(
                                    title = "Check for updates",
                                    summary = when {
                                        updateChecking -> "Checking..."
                                        latestSeen != null ->
                                            "Current ${BuildConfig.VERSION_NAME}${channelTag(installedPrerelease)} - latest $latestSeen${channelTag(latestPrerelease)}"
                                        else ->
                                            "Current ${BuildConfig.VERSION_NAME} - check GitHub for a newer release"
                                    },
                                    onClick = { manualUpdateCheck() },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.SystemUpdate,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                SwitchPreference(
                                    title = "Beta channel",
                                    checked = betaChannel,
                                    onCheckedChange = {
                                        betaChannel = it
                                        prefs.edit().putBoolean("beta_channel", it).apply()
                                    },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.RocketLaunch,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                ArrowPreference(
                                    title = "What's new",
                                    summary = when {
                                        latestSeen != null ->
                                            "Latest release: v$latestSeen${channelTag(latestPrerelease)} - tap for the notes"
                                        else ->
                                            "Tap to fetch the latest release notes"
                                    },
                                    onClick = { manualChangelogCheck() },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Notes,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DonateDialog(show: Boolean, onDismiss: () -> Unit) {
        OverlayDialog(
            show = show,
            title = "Support the developer",
            onDismissRequest = onDismiss,
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    DonateQr(label = "Donation QR", assetPath = "donate.png", modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Close",
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        )
    }

    @Composable
    private fun DonateQr(label: String, assetPath: String, modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val bitmap = remember(assetPath) {
            runCatching {
                context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
            }.getOrNull()
        }
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            Text(
                text = label,
                fontSize = 13.sp,
                color = COUITheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }

    @Composable
    private fun LicenseListScreen(
        backdrop: LayerBackdrop?,
        onBack: () -> Unit,
        onSelect: (LicenseItem) -> Unit
    ) {
        Scaffold(
            topBar = {
                BlurredBar(backdrop = backdrop) {
                    TopAppBar(
                        title = "Open source licenses",
                        color = Color.Transparent,
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = COUIIcons.Back,
                                    contentDescription = "Back",
                                    tint = COUITheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                }
            },
            popupHost = { }
        ) { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = inner.calculateTopPadding() + 12.dp,
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 12.dp + 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { SectionLabel("This project") }
                    items(licenseItems) { item ->
                        LicenseCard(item) { onSelect(item) }
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionLabel(text: String) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = COUITheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
    }

    @Composable
    private fun LicenseChip(text: String) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(COUITheme.colorScheme.secondaryContainer)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = text,
                fontSize = 11.sp,
                color = COUITheme.colorScheme.onSecondaryContainer
            )
        }
    }

    @Composable
    private fun LicenseCard(item: LicenseItem, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    item.author?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            fontSize = 13.sp,
                            color = COUITheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                    if (item.licenses.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item.licenses.take(3).forEach { LicenseChip(it.name) }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LicenseDetailDialog(item: LicenseItem?, onDismiss: () -> Unit) {
        var lastItem by remember { mutableStateOf(item) }
        if (item != null) lastItem = item
        val display = lastItem ?: return
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val contents = remember(display) {
            display.licenses.map { license ->
                val text = license.asset?.let { asset ->
                    runCatching {
                        context.assets.open(asset).bufferedReader().use { it.readText() }
                    }.getOrNull()
                }
                license to text
            }
        }
        OverlayDialog(
            show = item != null,
            title = display.name,
            onDismissRequest = onDismiss,
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    if (display.licenses.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            display.licenses.forEach { LicenseChip(it.name) }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    SelectionContainer(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (contents.isEmpty()) {
                                Text("No license text declared", fontSize = 13.sp)
                            }
                            contents.forEach { (license, body) ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = license.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = COUITheme.colorScheme.primary
                                    )
                                    Text(
                                        text = body ?: "License text not bundled",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        display.website?.let { url ->
                            TextButton(
                                modifier = Modifier.weight(1f),
                                text = "Visit homepage",
                                onClick = { uriHandler.openUri(url) }
                            )
                        }
                        TextButton(
                            modifier = Modifier.weight(1f),
                            text = "Close",
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        )
    }

    @Composable
    private fun BottomBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
        NavigationBar(color = Color.Transparent) {
            AppTab.entries.forEach { t ->
                NavigationBarItem(
                    selected = t == selected,
                    onClick = { onSelect(t) },
                    icon = t.icon,
                    label = t.label
                )
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Backend                                                             */
    /* ------------------------------------------------------------------ */

    /** Returns the bound user service, or tells the user what is still missing. */
    private fun requireService(action: String): IIcService? {
        val s = service
        if (s != null) return s
        val msg = when {
            !binderReady -> "Shizuku not running: start Shizuku first via wireless debugging / ADB"
            !authorized -> "Shizuku not authorized: grant permission first"
            else -> "User service not bound: wait a moment or reopen the app"
        }
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        addLog("[$action] $msg")
        return null
    }

    private fun refreshAuth() {
        authorized = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        binderReady = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
    }

    private fun bindUserService() {
        if (bound || bindingInFlight) return
        bindingInFlight = true
        runCatching {
            val cn = ComponentName(this, IcService::class.java)
            val args = Shizuku.UserServiceArgs(cn)
                .daemon(true)
                .debuggable(false)
                .version(1)
                .tag("ic-lpe")
                .processNameSuffix(":icsvc")
            Shizuku.bindUserService(args, conn)
            addLog("[bind] starting the Shizuku shell user service...")
        }.onFailure {
            bindingInFlight = false
            addLog("[bind] failed:\n" + explainError(it, "binding the Shizuku user service"))
        }
    }

    private fun openManager() {
        val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent != null) {
            runCatching { startActivity(intent) }
        } else {
            Toast.makeText(this, "Shizuku not installed", Toast.LENGTH_SHORT).show()
        }
    }

    /* v52: open the developer options for the wireless-debugging pairing
     * step; vendor builds may hide the screen from other apps. */
    private fun openDeveloperOptions() {
        val enabled = Settings.Global.getInt(
            contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) != 0
        try {
            if (enabled) {
                addLog("[shizuku] opening developer options")
                startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            } else {
                addLog("[shizuku] developer options are disabled - enable them in Settings > About phone, tap Build number 7x, then come back")
                startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
            }
        } catch (e: android.content.ActivityNotFoundException) {
            addLog("[shizuku] developer options are not available on this build")
        }
    }

    /* v4: after a successful run the spoofed manager cannot be brought
     * up by the core (its "am start" fails), so launch it from here
     * instead of leaving the user to find it manually. */
    private fun openKsuManager() {
        val pkg = installedKsuPkg() ?: return
        addLogOnMain("[i] opening the ROOT manager ($pkg) - root is live")
        val intent = packageManager.getLaunchIntentForPackage(pkg)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent != null) runCatching { startActivity(intent) }
    }

    /* The manager candidate chain in one place, so the post-run
     * heads-up notification resolves the same package as the open. */
    private fun installedKsuPkg(): String? {
        val selected = KSU_PKGS[ksuIndex]
        return (if (selected.isNotEmpty()) listOf(selected) else emptyList())
            .plus(KSU_PKGS.filter { it.isNotEmpty() })
            /* The installed manager is the spoof build under a
     * randomized package id that intentionally is not part of
     * the dropdown list; try it last. */
            .plus(listOf("kqvdie.zeykvw.hpbcyd"))
            .firstOrNull { packageManager.getLaunchIntentForPackage(it) != null }
    }

    /* When the run ends while this app is backgrounded, the in-app
     * manager open can be blocked by the background activity launch
     * restriction; a notification with a PendingIntent is not. */
    private fun notifyRootLive() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                "root_live", "Jailbreak status",
                NotificationManager.IMPORTANCE_DEFAULT))
        val pkg = installedKsuPkg()
        val intent = (if (pkg != null)
            packageManager.getLaunchIntentForPackage(pkg)
        else
            packageManager.getLaunchIntentForPackage(packageName))
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: return
        val pi = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val note = Notification.Builder(this, "root_live")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("TimerRoot")
            .setContentText("Root is live - tap to open the ROOT manager")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(1, note) }
    }

    /* Saves the timeline to the public Download folder (falling back
     * to the app's own files dir) and hands the text to the share
     * sheet at the same time. */
    @Suppress("DEPRECATION")
    private fun shareLog() {
        if (logs.isEmpty()) {
            Toast.makeText(this, "Log is empty", Toast.LENGTH_SHORT).show()
            return
        }
        val text = logs.joinToString("\n")
        val stamp = java.text.SimpleDateFormat(
            "yyyyMMdd-HHmmss", java.util.Locale.ROOT
        ).format(java.util.Date())
        val saved = runCatching {
            val out = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS),
                "TimerRoot-log-$stamp.txt")
            out.parentFile?.mkdirs()
            out.writeText(text)
            out
        }.getOrElse {
            runCatching {
                val alt = File(
                    getExternalFilesDir(null),
                    "TimerRoot-log-$stamp.txt")
                alt.writeText(text)
                alt
            }.getOrNull()
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_TITLE, "TimerRoot log")
        }
        runCatching { startActivity(Intent.createChooser(send, "Share log")) }
        Toast.makeText(
            this,
            if (saved != null) "Saved to ${saved.path}"
            else "Share sheet only (saving failed)",
            Toast.LENGTH_LONG
        ).show()
    }

    /* Reads at a glance: green = proof/success, red = failure,
     * muted = app info lines; banners stay in the theme color and
     * print bold. Dark mode uses lighter shades for contrast. */
    private fun logLineColor(line: String, dark: Boolean): Color {
        val s = if (line.length > 9 && line[2] == ':' && line[5] == ':')
            line.substring(9) else line
        return when {
            s.contains("========") -> Color.Unspecified
            s.contains("LPE-PROOF") || s.contains("uid=0") ||
                s.contains("module resident") || s.contains("root is live") ||
                s.contains("ROOT_SHELL_OK") || s.contains("Done")
            -> if (dark) Color(0xFF81C784) else Color(0xFF2E7D32)
            s.contains("Failed") || s.contains("failed") ||
                s.contains("errno=") || s.contains("refus") ||
                s.contains("panic")
            -> if (dark) Color(0xFFEF9A9A) else Color(0xFFB3261E)
            s.startsWith("[i] ") || s.startsWith("[preflight]") ||
                s.startsWith("[bind]") || s.startsWith("[shizuku]")
            -> if (dark) Color(0xFF9AA0A6) else Color(0xFF5F6368)
            else -> Color.Unspecified
        }
    }

    private fun refreshStaticState() {
        lifecycleScope.launch {
            val selinux = withContext(Dispatchers.IO) {
                runCatching {
                    val s = service
                    if (s != null) s.runCmd("getenforce") else shellGetEnforce()
                }.getOrNull()?.trim().orEmpty()
            }
            selinuxText = mapSelinux(selinux)
        }
    }

    private fun shellGetEnforce(): String = runCatching {
        val p = Runtime.getRuntime().exec("getenforce")
        p.inputStream.bufferedReader().use { it.readText() }
    }.getOrDefault("")

    private fun mapSelinux(raw: String): String = when (raw.trim().lowercase()) {
        "enforcing" -> "enforcing"
        "permissive" -> "permissive"
        "disabled" -> "disabled"
        "" -> "Unknown"
        else -> raw.trim()
    }

    /**
     * The chain was validated on vivo/iQOO 6.1.145 kernels and on the
     * Motorola Edge 50 Ultra (ctwo) GKI 6.1.157 kernel, whose offsets are
     * built into the core (anchor table ctwo_4220_links for build
     * A171SUVH.42-20-1).  Anything else gets a startup notice (not a
     * hard block): the tool still runs, but the user is told the result
     * is not expected to be meaningful.
     */
    private fun checkDeviceAndKernel() {
        val brand = getSystemProperty("ro.product.brand")
        val manufacturer = getSystemProperty("ro.product.manufacturer")
        val vendor = "$brand $manufacturer".lowercase()
        val isVivo = vendor.contains("vivo") || vendor.contains("iqoo")
        val isMoto = vendor.contains("motorola")
        val gkiImage = kernelVersion.contains("6.1.157-android14-11-gb3d4cdadf339-ab16160610")
        val kernelOk = kernelVersion.contains("6.1.145") || (isMoto && kernelVersion.contains("6.1.157")) || gkiImage
        if ((isVivo || isMoto || gkiImage) && kernelOk) return
        val reasons = buildList {
            if (!isVivo && !isMoto)
                add("Not a vivo / iQOO / Motorola device (${brand.ifBlank { manufacturer }})")
            if (!kernelOk) add("Kernel is not supported ($kernelVersion)")
        }
        scopePending = true
        scopeReasons = reasons.joinToString("\n· ")
        notice = Notice(
            "Device notice",
            buildString {
                append("This tool only fits vivo / iQOO models with kernel 6.1.145, the Motorola Edge 50 Ultra with kernel 6.1.157, or any device running the identical GKI kernel image (6.1.157-android14-11-gb3d4cdadf339-ab16160610).\n\n")
                append("Detected:\n· ")
                append(reasons.joinToString("\n· "))
                append("\n\nYou can still continue, but privilege escalation will most likely fail, ")
                append("and writes to wrong addresses may leave the device misbehaving. Confirm the risk yourself.")
            }
        )
        addLog("[device] out of scope: ${reasons.joinToString("; ")}")
    }

    private fun resolveDeviceName(): String = runCatching {
        val brand = getSystemProperty("ro.product.brand")
        val model = getSystemProperty("ro.product.model")
        val device = getSystemProperty("ro.product.device")
        listOf(brand, model, device).filter { it.isNotBlank() }
            .joinToString(" ").ifBlank { "Unknown" }
    }.getOrDefault("Unknown")

    private fun getSystemProperty(key: String): String = try {
        val cls = Class.forName("android.os.SystemProperties")
        val get = cls.getMethod("get", String::class.java)
        get.invoke(null, key) as? String ?: ""
    } catch (_: Throwable) {
        ""
    }

    /**
     * A one-tap device report for support requests: everything the XDA
     * thread needs to judge a port (props, kernel, versions) - readable
     * without root.
     */
    private fun deviceReportText(): String = buildString {
        val brand = getSystemProperty("ro.product.brand")
        val manufacturer = getSystemProperty("ro.product.manufacturer")
        val model = getSystemProperty("ro.product.model")
        val device = getSystemProperty("ro.product.device")
        val buildId = getSystemProperty("ro.build.display.id")
        append("TimerRoot device report\n")
        append("app: TimerRoot ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
        append("device: $brand $manufacturer $model ($device)\n")
        append("build: $buildId\n")
        append("android: ${Build.VERSION.RELEASE}\n")
        append("kernel: $kernelVersion\n")
    }

    private fun shareDeviceReport() {
        val text = deviceReportText()
        addLog("[device] report: ${text.replace("\n", " | ")}")
        runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(send, "Share device report"))
        }
    }

    private fun managerInstalled(pkg: String): Boolean =
        runCatching { packageManager.getPackageInfo(pkg, 0) }.isSuccess

    /**
     * Android 11+ hides packages from apps unless they are declared in
     * <queries> (or QUERY_ALL_PACKAGES is held).  The shell user that Shizuku
     * gives us sees every installed package, so it is the reliable fallback
     * and also covers managers whose id we do not know yet.
     */
    private fun installedPackagesShell(s: IIcService): Set<String> =
        runCatching {
            s.runCmd("pm list packages 2>/dev/null")
                .lineSequence()
                .map { it.trim().removePrefix("package:") }
                .filter { it.isNotEmpty() }
                .toSet()
        }.getOrDefault(emptySet())

    private fun managerPresent(pkg: String, shell: Set<String>): Boolean =
        pkg.isNotEmpty() && (
            managerInstalled(pkg) ||
                pkg in shell ||
                (KSU_ALIASES[pkg] ?: emptyList()).any { managerInstalled(it) || it in shell }
            )

    /* One run per boot: every run leaves a zombie behind and consumes the
     * exploit's one-shot state, so a second attempt in the same boot cannot
     * succeed and can panic the kernel (verified the hard way).  Block the
     * button for the rest of this boot.  v46: the boot identity is the
     * kernel boot time (wall clock minus elapsedRealtime), not
     * Settings.Global.BOOT_COUNT - a framework restart re-runs
     * system_server and bumps BOOT_COUNT while the kernel, the zombie and
     * the one-shot state never rebooted, so after a soft reboot the app
     * re-armed the run button inside the same kernel boot. */
    private fun bootId(): Long =
        (System.currentTimeMillis() - SystemClock.elapsedRealtime()) / 1000

    private fun runDoneThisBoot(): Boolean =
        prefs.getLong("run_done_id", 0L) == bootId()

    private fun markRunBoot() {
        prefs.edit().putLong("run_done_id", bootId()).remove("run_ok_id").apply()
        refreshRunDone()
    }

    /* v9: persist that this boot's run actually reached root. */
    private fun markRunOk() {
        prefs.edit().putLong("run_ok_id", bootId()).apply()
    }

    /* v4/v9: push the persisted run flags into Compose state. rootedNow
     * means "a run reached root this boot" (run_ok_boot), not merely
     * "a run happened" - a failed attempt must not show as active. */
    private fun refreshRunDone() {
        rootedNow = runSucceededThisBoot()
    }

    private fun runSucceededThisBoot(): Boolean =
        prefs.getLong("run_ok_id", 0L) == bootId()

    private fun runCore() {
        val s = requireService("run") ?: return
        val selectedPkg = KSU_PKGS[ksuIndex]
        val selectedName = KSU_NAMES[ksuIndex]
        val safe = safeMode
        val stamp = (System.currentTimeMillis() / 1000).toString()
        val bin = "$TMP/ic-timer-a$stamp"
        val script = "$TMP/ic-lpe-a$stamp.sh"
        val logFile = "$TMP/ic-app-a$stamp.log"
        /* The manager dropdown has no "off" entry any more: every choice runs
         * the KSU handoff, and an empty package means "auto-detect". */
        val handoff = "$TMP/ksu-h$stamp.sh"
        val proof = DEFAULT_CMD
        val extraEnv = "IC_SYMS_FILE=/data/local/tmp/ic-syms.txt" +
            (if (safe) "\nIC_SAFE_MODE=1" else "") +
            "\nIC_KSU=1"

        busy = true
        rootConfirmed = false
        managerOpenedThisRun = false
        runStageIndex = 0
        runStage = "1/${RUN_STAGES.size} ${RUN_STAGES.first().first}"
        runStamp = stamp
        runStartMs = SystemClock.elapsedRealtime()
        addLog(
            "======== run start (boot age ${runStartMs / 1000}s) ========")
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.POST_NOTIFICATIONS), 7)
        }
        lifecycleScope.launch {
            val refused = withContext(Dispatchers.IO) {
                try {
                    val id0 = s.identity()
                    if (!id0.startsWith("uid=2000 ") || !id0.contains("ctx=u:r:shell")) {
                        addLogOnMain(
                            "[run] refusing to run: the user service identity is not shell\n$id0\n" +
                                "Start Shizuku via ADB / wireless debugging (not as root), then tap \"Authorize Shizuku\"."
                        )
                        return@withContext true
                    }
                    /* The endgame hands root to a manager's own ksud; without
                     * one there is nothing to hand off to, so ask before
                     * starting anything.  The check is done from the shell,
                     * not only from the app: package visibility would hide
                     * every manager from an app targeting Android 11+. */
                    val shellPkgs = installedPackagesShell(s)
                    val selectedOk = managerPresent(selectedPkg, shellPkgs)
                    if (KSU_PKGS.none { managerPresent(it, shellPkgs) } &&
                        !managerPresent("kqvdie.zeykvw.hpbcyd", shellPkgs)) {
                        addLogOnMain("[run] no KSU manager installed - proof mode without handoff")
                    }
                    /* The selected manager is optional: the handoff script
                     * tries every installed manager anyway.  When the selected
                     * one is missing, fall back to auto-detect instead of
                     * refusing a run that would actually have succeeded. */
                    val ksuPkg = if (selectedPkg.isEmpty() || selectedOk) selectedPkg else ""
                    if (selectedPkg.isNotEmpty() && !selectedOk) {
                        addLogOnMain(
                            "[run] the selected manager \"$selectedName\" ($selectedPkg) is not installed, " +
                                "falling back to an installed manager automatically"
                        )
                    }
                    /* Pre-clean the KSU daemon area before the late-load so the
                     * manager never spawns a stale /data/adb/ksud again (known
                     * superuser-tab hang after reruns).  With any manager
                     * installed, run the pushed handoff script as the root
                     * payload: it re-extracts a fresh ksud from the manager
                     * APK and late-loads that manager's own module (empty
                     * package = try every known manager; arg 4 = 1 leaves
                     * the manager launch to the core).  The only manager the
                     * script cannot find by itself is the spoof build, which
                     * is not in its KNOWN list, so pass that one explicitly.
                     * Without any manager there is nothing to hand off to:
                     * proof payload only, skip the script's ksud fallbacks. */
                    val handoffPkg = when {
                        ksuPkg.isNotEmpty() -> ksuPkg
                        managerPresent("kqvdie.zeykvw.hpbcyd", shellPkgs) -> "kqvdie.zeykvw.hpbcyd"
                        else -> ""
                    }
                    val anyManager = ksuPkg.isNotEmpty() ||
                        KSU_PKGS.any { it.isNotEmpty() && managerPresent(it, shellPkgs) } ||
                        managerPresent("kqvdie.zeykvw.hpbcyd", shellPkgs)
                    val payload =
                        "rm -f /data/adb/ksud /data/adb/ksu/bin/busybox /data/adb/ksu/bin/bootctl /data/adb/ksu/bin/resetprop 2>/dev/null; " +
                            (if (anyManager)
                                "sh '$handoff' '$handoffPkg' '' ${if (safe) "1" else "0"} 1 2>&1; "
                            else "") + proof

                    s.truncate(script)
                    push(s, SCHEME_ASSET, script)
                    s.makeExec(script)

                    s.truncate(bin)
                    push(s, "ic-timer", bin)
                    s.makeExec(bin)

                    s.truncate(handoff)
                    push(s, "ksu_handoff.sh", handoff)
                    s.makeExec(handoff)

                    /* Preflight: ask the core whether this build has offsets
                     * (built-in anchor table / kallsyms / ic-syms.txt).  A run
                     * without them cannot do anything but fail, so stop here
                     * and point the user at the boot.img parser. */
                    val info = s.runCmd("$bin info 2>&1")
                    addLogOnMain("[info] " + info.trim())
                    val haveCore = Regex("have_core=(\\d)").find(info)?.groupValues?.get(1)
                    val haveFake = Regex("have_fake=(\\d)").find(info)?.groupValues?.get(1)
                    if (haveCore == null) {
                        postUi {
                            offsetPrompt = "Cannot confirm the kernel offsets for the current version (the kernel may be unsupported).\n\n" +
                                "Tap \"Parse offsets\" and pick this model's boot.img or full package to generate the symbol table."
                        }
                        return@withContext true
                    }
                    if (haveCore != "1" || haveFake != "1") {
                        postUi {
                            offsetPrompt = "No offsets for the current version; parse them manually.\n\n" +
                                "Tap \"Parse offsets\", pick this model's boot.img or full package, generate " +
                                "/data/local/tmp/ic-syms.txt, then tap one-tap jailbreak."
                        }
                        return@withContext true
                    }

                    s.truncate(logFile)
                    val r = s.start(script, bin, logFile, ATTEMPTS, payload, extraEnv)
                    markRunBoot()
                    addLogOnMain(
                        "[run] scheme: $SCHEME_ASSET  KSU: ${KSU_NAMES[ksuIndex]}\n$r"
                    )
                    logPath = logFile
                    logOff = 0
                    startPolling()
                    false
                } catch (t: Throwable) {
                    addLogOnMain("[run] failed:\n" + explainError(t, "running the privilege-escalation chain"))
                    true
                }
            }
            /* busy stays true until the core prints "[core] exit=" so the
             * button keeps showing the live stage for the whole run. */
            if (refused) {
                busy = false
                runStage = ""
            }
        }
    }

    private fun startPolling() {
        if (polling) return
        polling = true
        lifecycleScope.launch(Dispatchers.IO) {
            while (polling) {
                val s = service
                val path = logPath
                if (s == null || path == null) break
                try {
                    val len = s.length(path)
                    /* Defensive: if a writer ever shrinks or rewrites this
                     * file, sync to the current end instead of re-delivering
                     * rewritten regions at the stale monotonic offset. */
                    if (len < logOff) logOff = len
                    while (logOff < len) {
                        val want = minOf(64 * 1024, len - logOff)
                        val b = s.read(path, logOff, want)
                        if (b == null || b.isEmpty()) break
                        logOff += b.size
                        val text = String(b, Charsets.UTF_8)
                        withContext(Dispatchers.Main) { raw(text) }
                    }
                } catch (_: Throwable) {
                }
                delay(300)
            }
            polling = false
        }
    }

    private fun diag() {
        val s = requireService("diagnostics") ?: return
        lifecycleScope.launch {
            val out = withContext(Dispatchers.IO) {
                runCatching {
                    s.runCmd(
                        "id; cat /proc/self/attr/current; getenforce; " +
                            "echo ---; uname -r; " +
                            "getprop ro.product.device; getprop ro.product.model; " +
                            "echo ---; ls -l /data/local/tmp/ | head -20; " +
                            "echo ---; head -n 2 /data/local/tmp/ic-syms.txt " +
                            "2>/dev/null; ls -d /data/adb/ksu 2>/dev/null; " +
                            "echo ---; pm list packages 2>/dev/null | " +
                            "grep -Ei 'kernelsu|sukisu|resukisu|bakasu|sakisu|" +
                            "sevenk|vivoksu|ksunext|fina'"
                    )
                }.getOrElse { "Diagnostics failed:\n" + explainError(it, "diagnostics") }
            }
            addLog("[diagnostics]\n$out")
        }
    }

    private fun parseOffsets() {
        requireService("parse offsets") ?: return
        parseMenu = true
    }

    @Composable
    private fun ParseMenuDialog() {
        OverlayDialog(
            show = parseMenu,
            title = "Parse offsets",
            onDismissRequest = { parseMenu = false },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Text(
                        "Pick boot.img / a full package from files, or enter an OTA link to download only the boot partition.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Pick from files (boot.img / full package)",
                        onClick = {
                            parseMenu = false
                            runCatching {
                                parsePicker.launch(arrayOf("*/*"))
                            }.onFailure {
                                addLog(
                                    "[parse offsets] cannot open the file picker:\n" +
                                        explainError(it, "opening the file picker")
                                )
                            }
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Download the full package from a link",
                        onClick = {
                            parseMenu = false
                            otaUrlText = prefs.getString("ota_url", "") ?: ""
                            otaUrlPrompt = true
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Cancel",
                        onClick = { parseMenu = false }
                    )
                }
            }
        )
    }

    @Composable
    private fun OtaUrlDialog() {
        OverlayDialog(
            show = otaUrlPrompt,
            title = "Full-package link",
            onDismissRequest = { otaUrlPrompt = false },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Text(
                        "Paste the OTA's http(s) link. Only the boot partition is downloaded, not the whole package.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    TextField(
                        value = otaUrlText,
                        onValueChange = { otaUrlText = it },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundMode = TextFieldMode.Rectangle,
                        label = "OTA link",
                        singleLine = true
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Start",
                        onClick = {
                            val url = otaUrlText.trim()
                            if (!url.startsWith("http://", true) &&
                                !url.startsWith("https://", true)
                            ) {
                                Toast.makeText(
                                    this@MainActivity, "Enter an http(s) link",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                otaUrlPrompt = false
                                prefs.edit().putString("ota_url", url).apply()
                                parseFromUrl(url)
                            }
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Cancel",
                        onClick = { otaUrlPrompt = false }
                    )
                }
            }
        )
    }

    private fun parseFromUrl(url: String) {
        val s = requireService("parse offsets") ?: return
        runStage = ""
        busy = true
        lifecycleScope.launch {
            val log = StringBuilder()
            fun note(l: String) {
                log.append(l).append('\n')
                addLogOnMain(l)
            }
            try {
                withContext(Dispatchers.IO) {
                    note("[parse] extracting boot.img from the link: $url")
                    val file = OtaUrlExtractor.extractBoot(url, cacheDir, ::note)
                    try {
                        runParseFile(s, file, "boot.img (from link)", ::note)
                    } finally {
                        file.delete()
                    }
                }
                val out = log.toString()
                addLog("[parse offsets]\n$out")
                if (!out.contains("parse_rc=0")) {
                    val tail = out.lines().filter { it.isNotBlank() }.takeLast(14)
                        .joinToString("\n")
                    notice = Notice(
                        "Parse failed",
                        "No symbol table generated (the parser returned non-zero).\n\nLast log lines:\n$tail"
                    )
                }
            } catch (a: ParseAbort) {
                addLog("[parse offsets] ${a.message}")
                notice = Notice("Cannot parse", a.message ?: "Unknown error")
            } catch (t: Throwable) {
                val why = explainError(t, "extracting boot.img from the link")
                addLog("[parse offsets] failed:\n$why")
                notice = Notice("Parse failed", why)
            } finally {
                busy = false
            }
        }
    }

    /** Verifies and pushes an already-extracted boot.img, then runs the syms parser. */
    private fun runParseFile(
        s: IIcService,
        file: File,
        label: String,
        note: (String) -> Unit,
    ) {
        parseStage = "Parsing the downloaded boot.img"
        val stamp = (System.currentTimeMillis() / 1000).toString()
        val bin = "$TMP/ic-syms-a$stamp"
        val img = "$TMP/ic-boot-a$stamp.img"
        val syms = "$TMP/ic-syms.txt"
        s.truncate(bin)
        push(s, "ic-timer", bin)
        s.makeExec(bin)
        s.truncate(img)
        FileInputStream(file).use { fin ->
            val ch = fin.channel
            val size = ch.size()
            val head = readAt(ch, 0, 8)
            note("[parse] file: $label  $size bytes")
            if (!(head.size >= 8 &&
                    String(head, 0, 8, Charsets.US_ASCII) == "ANDROID!")) {
                throw ParseAbort("What was extracted from the link is not an Android boot image (no ANDROID! header).")
            }
            val hdr = readAt(ch, 0, 64)
            val ksize = u32(hdr, 8)
            val hver = u32(hdr, 40)
            note("[parse] type: Android boot image v$hver, kernel $ksize bytes")
            if (ksize == 0L) {
                throw ParseAbort(
                    "What was extracted is init_boot / vendor_boot (no kernel); offsets cannot be parsed."
                )
            }
            checkBootIdentity(ch, size, note)
            val pushed = pushChannel(s, img, ch, 0, size)
            note("[parse] boot.img pushed: $pushed bytes")
        }
        parseStage = "Generating the symbol table"
        note(
            s.runCmd(
                "ls -l $img 2>&1; $bin syms $img $syms 2>&1; " +
                    "echo parse_rc=$?; sed -n '1,3p' $syms 2>/dev/null; rm -f $img"
            )
        )
    }

    /** Thrown when the picked file cannot be used; the message goes to a dialog. */
    private class ParseAbort(message: String) : Exception(message)

    private data class Notice(val title: String, val message: String)

    private fun parseSelected(uri: Uri) {
        val s = requireService("parse offsets") ?: return
        val name = uri.lastPathSegment ?: "the selected file"
        runStage = ""
        busy = true
        lifecycleScope.launch {
            try {
                val out = withContext(Dispatchers.IO) { runParse(s, uri, name) }
                addLog("[parse offsets]\n$out")
                if (!out.contains("parse_rc=0")) {
                    val tail = out.lines().filter { it.isNotBlank() }.takeLast(14)
                        .joinToString("\n")
                    notice = Notice(
                        "Parse failed",
                        "No symbol table generated (the parser returned non-zero).\n\n" +
                            "Common causes: the selected image does not match this device's kernel version, or this kernel build " +
                            "is not yet supported by the parser.\n\nLast log lines:\n$tail"
                    )
                }
            } catch (a: ParseAbort) {
                addLog("[parse offsets] ${a.message}")
                notice = Notice("Cannot parse", a.message ?: "Unknown error")
            } catch (t: Throwable) {
                val why = explainError(t)
                addLog("[parse offsets] failed ($parseStage):\n$why")
                notice = Notice("Parse failed", why)
            } finally {
                busy = false
            }
        }
    }

    /**
     * Every user-visible failure gets a plain-Chinese explanation first; the
     * raw exception stays at the end so bug reports still carry the detail.
     */
    private fun explainError(t: Throwable, stage: String = parseStage): String {
        val detail = t.message?.takeIf { it.isNotBlank() } ?: t.javaClass.name
        val hint = when {
            t is NoSuchFieldError || t is NoSuchMethodError ->
                "The zstd data inside the full package cannot be decoded: the built-in decoder is missing or incompatible with this system. " +
                    "Make sure the full APK is installed (do not remove lib/arm64-v8a/libextract.so), " +
                    "or reinstall this version and retry."
            t is OutOfMemoryError ->
                "Out of memory. Decompressing a full package takes a lot of RAM; close background apps and retry, " +
                    "or just pick the already-extracted boot.img (not the full package)."
            t is SecurityException ->
                "No permission to read the selected file. Copy it to the Downloads folder and pick it again."
            t is java.io.FileNotFoundException ->
                "The file no longer exists or cannot be opened; it may have been moved, deleted, or is unreadable. Pick it again."
            t is java.nio.channels.ClosedChannelException ->
                "The file read was interrupted (the file may be held by another app or the picker has closed). Pick it again."
            t is java.util.zip.ZipException ->
                "The archive is corrupted or incomplete; re-download the full package."
            t is java.io.IOException ->
                "Error while reading the file: $detail\nMake sure the file is intact (copy it to Downloads and retry)."
            t is IllegalArgumentException ->
                "The file content is not what was expected: $detail"
            else ->
                "An unexpected error occurred while reading or parsing: $detail"
        }
        return "$hint\n\nFailed at: $stage\nTechnical info: ${t.javaClass.simpleName}: $detail"
    }

    private fun runParse(s: IIcService, uri: Uri, name: String): String {
        parseStage = "Opening the selected file"
        val stamp = (System.currentTimeMillis() / 1000).toString()
        val bin = "$TMP/ic-syms-a$stamp"
        val img = "$TMP/ic-boot-a$stamp.img"
        val syms = "$TMP/ic-syms.txt"
        val log = StringBuilder()
        fun note(l: String) {
            log.append(l).append('\n')
        }

        s.truncate(bin)
        push(s, "ic-timer", bin)
        s.makeExec(bin)
        s.truncate(img)

        val pfd = contentResolver.openFileDescriptor(uri, "r")
            ?: throw ParseAbort("Cannot open the selected file (the picker returned no readable handle)")
        val ch = FileInputStream(pfd.fileDescriptor).channel
        try {
            val size = ch.size()
            val head = readAt(ch, 0, 8)
            note("[parse] file: $name  $size bytes")
            when {
                head.size >= 4 && head[0] == 'P'.code.toByte() &&
                    head[1] == 'K'.code.toByte() -> {
                    note("[parse] type: ZIP (full package)")
                    parseStage = "Extracting the boot partition from the full package"
                    extractFromZip(s, ch, size, img, ::note)
                }
                head.size >= 8 && String(head, 0, 8, Charsets.US_ASCII) == "ANDROID!" -> {
                    val hdr = readAt(ch, 0, 64)
                    val ksize = u32(hdr, 8)
                    val hver = u32(hdr, 40)
                    note("[parse] type: Android boot image v$hver, kernel $ksize bytes")
                    parseStage = "Parsing the boot image"
                    if (ksize == 0L) {
                        throw ParseAbort(
                            "The selected image is init_boot / vendor_boot (no kernel); offsets cannot be parsed.\n\n" +
                                "Pick the boot.img from this model's firmware, or select the full OTA package directly."
                        )
                    }
                    if (size <= 64) throw ParseAbort("Abnormal boot image size ($size bytes)")
                    checkBootIdentity(ch, size, ::note)
                    val pushed = pushChannel(s, img, ch, 0, size)
                    note("[parse] boot.img pushed: $pushed bytes")
                }
                else -> throw ParseAbort(
                    "Unrecognized file: pick a boot.img (Android boot image) " +
                        "or a full OTA package (.zip).\n\ninit_boot / vendor_boot / other images are not supported."
                )
            }
            parseStage = "Generating the symbol table"
            val r = s.runCmd(
                "ls -l $img 2>&1; $bin syms $img $syms 2>&1; " +
                    "echo parse_rc=$?; sed -n '1,3p' $syms 2>/dev/null; rm -f $img"
            )
            note(r)
            return log.toString()
        } finally {
            try {
                ch.close()
            } catch (_: Throwable) {
            }
            try {
                pfd.close()
            } catch (_: Throwable) {
            }
        }
    }

    private data class Fingerprint(
        val vendor: String,
        val device: String,
        val incremental: String,
    )

    private fun parseFingerprint(s: String): Fingerprint? {
        val parts = s.split('/')
        if (parts.size < 5) return null
        val inc = parts[4].substringBefore(':')
        if (parts[0].isBlank() || parts[1].isBlank() || inc.isBlank()) return null
        return Fingerprint(parts[0].trim(), parts[1].trim(), inc.trim())
    }

    /**
     * Tells whether a bare boot.img belongs to this device *without* the OTA
     * package: boot v3/v4 images carry the build fingerprint
     * (com.android.build.boot.fingerprint) written at build time, which shares
     * vendor/device/incremental with ro.build.fingerprint.  Falls back to the
     * kernel version banner when the image has no bootconfig.
     */
    private fun checkBootIdentity(ch: FileChannel, size: Long, note: (String) -> Unit) {
        val imgFpStr = scanAsciiValue(ch, size, "com.android.build.boot.fingerprint")
        val devFpStr = getSystemProperty("ro.build.fingerprint")
        val imgFp = imgFpStr?.let { parseFingerprint(it) }
        val devFp = parseFingerprint(devFpStr)
        if (imgFp != null && devFp != null) {
            note("[parse] boot fingerprint: $imgFpStr")
            note("[parse] device fingerprint: $devFpStr")
            /* Same build = same vendor and same build number.  The product
             * token may legitimately differ by a hardware suffix (the package
             * says PD2339 while the device reports PD2339M); requiring it made
             * a correct image fail. */
            val sameVendor = imgFp.vendor.equals(devFp.vendor, ignoreCase = true)
            val same = sameVendor && imgFp.incremental == devFp.incremental
            if (!same && !sameVendor && imgFp.incremental == devFp.incremental) {
                /* POCO/Redmi/Xiaomi rebrands sell one OS build under two
                 * vendors (POCO/malachitep vs the boot image's Xiaomi/hal_*),
                 * so the vendor token differs while the build number is the
                 * same; the kernel is the same, so proceed. */
                note("[parse] rebrand detected: same build ${imgFp.incremental} under vendor ${imgFp.vendor}/${devFp.vendor} - proceeding")
                note("[parse] version check passed (same OS build, rebranded vendor)")
                return
            }
            if (!same) {
                /* v20: the offsets live in the kernel image, not in the OS
                 * build.  A different build is only fatal when the kernel
                 * itself differs; compare kernel banners before refusing. */
                val imgBanner = scanKernelBanner(ch, size)
                val kernelSame = imgBanner != null &&
                    (imgBanner == kernelVersion || kernelVersion.contains(imgBanner))
                if (!kernelSame) {
                    throw ParseAbort(
                        "The selected boot.img is not the same build as this device.\n\n" +
                            "Image: ${imgFp.vendor}/${imgFp.device} ... ${imgFp.incremental}\n" +
                            "Device: ${devFp.vendor}/${devFp.device} ... ${devFp.incremental}\n\n" +
                            "Kernel in the image: ${imgBanner ?: "unreadable"}; this device: $kernelVersion.\n\n" +
                            "Pick the boot.img or full package matching the current system."
                    )
                }
                note("[parse] build fingerprint mismatch - image: ${imgFp.vendor}/${imgFp.incremental}, device: ${devFp.vendor}/${devFp.incremental}")
                note("[parse] kernel matches ($imgBanner) - proceeding anyway; a same-build image is still recommended")
            } else {
                note("[parse] version check passed (boot fingerprint matches)")
            }
            return
        }
        val banner = scanKernelBanner(ch, size)
        if (banner != null) {
            note("[parse] image kernel: $banner, device kernel: $kernelVersion")
            if (banner != kernelVersion && !kernelVersion.contains(banner)) {
                throw ParseAbort(
                    "The boot.img's kernel ($banner) does not match this device's kernel ($kernelVersion).\n\n" +
                        "Pick the boot.img or full package matching the current system."
                )
            }
            note("[parse] version check passed (kernel version only)")
            return
        }
        note("[parse] cannot read the build fingerprint or kernel version from the boot.img; skipping the version check")
    }

    private fun extractFromZip(
        s: IIcService,
        ch: FileChannel,
        size: Long,
        img: String,
        note: (String) -> Unit,
    ) {
        val zip = ZipReader(ch, size)
        val meta = zip.find("META-INF/com/android/metadata")
        if (meta != null) {
            val text = zip.readSmall(meta, 64 * 1024)?.toString(Charsets.UTF_8)
            if (text != null) {
                val kv = HashMap<String, String>()
                text.lineSequence().forEach { line ->
                    val i = line.indexOf('=')
                    if (i > 0) kv[line.substring(0, i).trim()] = line.substring(i + 1).trim()
                }
                val pkgDev = kv["pre-device"].orEmpty()
                val pkgVer = kv["post-version"].orEmpty()
                note("[parse] package info: device=$pkgDev version=$pkgVer")
                checkPackageMatch(pkgDev, pkgVer)
            }
        } else {
            note("[parse] no META-INF/com/android/metadata in the package; skipping the device/version check")
        }

        val payload = zip.find("payload.bin")
        val bootEntry = zip.find("boot.img") ?: zip.find("boot/boot.img")
        when {
            payload != null -> {
                if (payload.method != 0) {
                    throw ParseAbort(
                        "payload.bin is stored compressed inside the zip - not the official full-package format. " +
                            "Use an official full package (payload.bin must be Stored/uncompressed)."
                    )
                }
                note("[parse] payload.bin: ${payload.compressedSize} bytes @ ${payload.dataOffset}")
                val plan = BootExtractor.manifest(ch, payload.dataOffset)
                note("[parse] boot partition: ${plan.size} bytes, ${plan.ops.size} ops, block=${plan.blockSize}")
                BootExtractor.extract(ch, payload.dataOffset, plan, note) { off, bytes, from, len ->
                    var done = 0
                    while (done < len) {
                        val n = minOf(CHUNK, len - done)
                        s.put(img, (off + done).toInt(), bytes.copyOfRange(from + done, from + done + n))
                        done += n
                    }
                }
            }
            bootEntry != null -> {
                val pushed = pushEntry(s, img, zip, bootEntry)
                note("[parse] boot.img in package: $pushed bytes")
            }
            else -> throw ParseAbort("This zip has neither payload.bin nor boot.img; it is not a usable full package.")
        }
    }

    private fun checkPackageMatch(pkgDev: String, pkgVer: String) {
        val devId = getSystemProperty("ro.product.device")
        val model = getSystemProperty("ro.product.model")
        val buildId = getSystemProperty("ro.build.display.id")
        val deviceStr = listOf(devId, model, buildId).filter { it.isNotBlank() }.joinToString(" ")
        val version = Regex("""\d+\.\d+\.\d+\.\d+""").find(pkgVer)?.value
        val mismatch = when {
            pkgDev.isNotEmpty() && !deviceStr.contains(pkgDev, ignoreCase = true) ->
                "Device mismatch: this package is for $pkgDev, this device is $deviceStr"
            version != null && !buildId.contains(version) ->
                "Version mismatch: this package is $pkgVer, the current system is $buildId"
            else -> null
        }
        if (mismatch != null) {
            throw ParseAbort(
                "$mismatch\n\nOffset generation refused: running the write primitive with the wrong version's addresses sends the writes to " +
                    "wrong kernel addresses. Use the firmware package / boot.img matching this device and version."
            )
        }
        addLog("[parse] device/version check passed: $deviceStr")
    }

    private fun pushChannel(s: IIcService, dst: String, ch: FileChannel, start: Long, length: Long): Long {
        var off = 0L
        while (off < length) {
            val want = minOf(CHUNK.toLong(), length - off).toInt()
            val buf = readAt(ch, start + off, want)
            if (buf.isEmpty()) break
            s.put(dst, off.toInt(), buf)
            off += buf.size
        }
        return off
    }

    private fun pushEntry(s: IIcService, dst: String, zip: ZipReader, e: ZipEntryInfo): Long {
        zip.open(e).use { input ->
            var off = 0L
            val buf = ByteArray(CHUNK)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                s.put(dst, off.toInt(), if (n == buf.size) buf else buf.copyOf(n))
                off += n
            }
            return off
        }
    }

    private fun cleanup() {
        cleanupDialog = requireService("clean up") ?: return
    }

    /** Deletes this tool's /data/local/tmp leftovers; works before any root. */
    private fun doTempCleanup(s: IIcService) {
        lifecycleScope.launch {
            val out = withContext(Dispatchers.IO) {
                runCatching {
                    s.runCmd(
                        "rm -f /data/local/tmp/ic-timer-a* /data/local/tmp/ic-lpe-a* " +
                            "/data/local/tmp/ic-app-a* /data/local/tmp/ksu-h* " +
                            "/data/local/tmp/ic-safe-h* /data/local/tmp/ic-syms-a* " +
                            "/data/local/tmp/ic-boot-a* /data/local/tmp/ic-clean-a* " +
                            "/data/local/tmp/ksu_stage.sh /data/local/tmp/ic-payload* 2>&1; " +
                            "echo ---; ls -l /data/local/tmp/ 2>/dev/null | " +
                            "grep -E 'ic-|ksu|payload' | head -20"
                    )
                }.getOrElse { "Cleanup failed:\n" + explainError(it, "cleaning temp files") }
            }
            addLog("[cleanup] this tool's temp files\n$out")
            Toast.makeText(
                this@MainActivity,
                "This tool's temp files cleaned",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private var cleanupDialog by mutableStateOf<IIcService?>(null)

    @Composable
    private fun CleanupDialog() {
        val s = cleanupDialog
        OverlayDialog(
            show = s != null,
            title = "Clean up",
            onDismissRequest = { cleanupDialog = null },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Text(
                        "\"Clean this tool's temp files\" deletes the processes, scripts and log copies this tool left in /data/local/tmp " +
                            "at any time before privilege escalation; it does not affect the already-parsed " +
                            "ic-syms.txt.\n\n" +
                            "\"Clean KSU\" deletes /data/adb/ksu, /data/adb/modules, " +
                            "/data/adb/modules_update and other KSU data. A loaded kernel module disappears only after " +
                            "a reboot; reboot before escalating again after cleaning. The manager APK is kept by default.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Clean this tool's temp files",
                        onClick = {
                            cleanupDialog = null
                            s?.let { doTempCleanup(it) }
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Clean KSU (keep the manager)",
                        onClick = {
                            cleanupDialog = null
                            s?.let { doCleanup(it, false) }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Uninstall the manager too",
                        onClick = {
                            cleanupDialog = null
                            s?.let { doCleanup(it, true) }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Cancel",
                        onClick = { cleanupDialog = null }
                    )
                }
            }
        )
    }

    @Composable
    private fun NoticeDialog() {
        val n = notice
        OverlayDialog(
            show = n != null,
            title = n?.title ?: "",
            onDismissRequest = { notice = null },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    SelectionContainer {
                        Text(n?.message ?: "", fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Got it",
                        onClick = { notice = null },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        )
    }

    @Composable
    private fun OffsetPromptDialog() {
        val msg = offsetPrompt
        OverlayDialog(
            show = msg != null,
            title = "Missing offsets",
            onDismissRequest = { offsetPrompt = null },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Text(msg ?: "", fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Parse offsets",
                        onClick = {
                            offsetPrompt = null
                            parseOffsets()
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Cancel",
                        onClick = { offsetPrompt = null }
                    )
                }
            }
        )
    }

    @Composable
    private fun SoftRebootPromptDialog() {
        OverlayDialog(
            show = softRebootPrompt,
            title = "Soft reboot",
            onDismissRequest = { softRebootPrompt = false },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Text(
                        "Some modules only affect processes that start after " +
                            "them - Zygisk is the usual example - and ask for a " +
                            "reboot after installation.\n\n" +
                            "This restarts the Android framework instead of the " +
                            "phone: apps close and come back, but root and the " +
                            "jailbreak stay alive because the kernel keeps " +
                            "running. A normal reboot would need a new exploit " +
                            "run - do not power off.\n\n" +
                            "Things that run only at real boot (post-fs-data " +
                            "scripts) cannot run in jailbreak mode either way.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Restart framework now",
                        onClick = { doSoftReboot() },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        )
    }

    /* v52: Stage 1 of the Shizuku onboarding - guide only.  The dialog
     * walks the user through the wireless-debugging start inside the
     * Shizuku app; this app deliberately runs no adb client of its own
     * (that later stage would change the binding design first). */
    @Composable
    private fun ShizukuSetupDialog() {
        OverlayDialog(
            show = shizukuSetupPrompt,
            title = "Shizuku setup",
            onDismissRequest = { shizukuSetupPrompt = false },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Text(
                        "Shizuku must be started on this phone after every reboot.\n\n" +
                            "1. Open Shizuku and tap Start via Wireless debugging.\n\n" +
                            "2. When asked, pair: Shizuku opens Wireless debugging in the " +
                            "developer options - tap Pair device with pairing code and " +
                            "enter the code in Shizuku.\n\n" +
                            "3. Back here, authorize TimerRoot when Shizuku is running.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Open Shizuku",
                        onClick = {
                            addLog("[shizuku] opening Shizuku - start its server via wireless debugging, then come back")
                            openManager()
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Developer options",
                        onClick = { openDeveloperOptions() },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        )
    }

    /* v44: run the framework restart.  The sequence lives entirely in one
     * shell command: this app process dies with the framework restart,
     * but the Shizuku user service (own app_process, not a zygote child)
     * survives it and finishes the command anyway - graceful ctl.restart,
     * wait, verify, kill fallback. */

    /* v48: self-heal the run state across an app update.  Versions before
     * 0.31.3 key the run flags to the system BOOT_COUNT, which the kernel
     * boot ids cannot map back onto - updating right after a run showed a
     * fresh boot in the app while the exploit's one-shot state was
     * already consumed in this kernel boot.  When legacy flags exist but
     * the new state does not, probe the truth over the shell service:
     * live root restores this boot's flags, a definitive no restores the
     * honest fresh state, and an undecidable probe keeps the flags for
     * the next resume.  Each decision runs at most once. */
    private fun recoverRootState() {
        if (recoveryBusy || !bound || runSucceededThisBoot()) return
        if (!prefs.contains("run_boot") && !prefs.contains("run_ok_boot")) return
        val s = service ?: return
        recoveryBusy = true
        lifecycleScope.launch {
            val id = withContext(Dispatchers.IO) {
                runCatching { s.identity() }.getOrNull() ?: ""
            }
            val out = if (id.startsWith("uid=2000 ")) withContext(Dispatchers.IO) {
                runCatching {
                    s.runCmd("SU=/data/adb/ksu/bin/su; [ -x \"\$SU\" ] || SU=/system/bin/su; " +
                        "[ -x \"\$SU\" ] || SU=su; \"\$SU\" -c id 2>&1")
                }.getOrNull() ?: ""
            } else ""
            if (out.contains("uid=0")) {
                prefs.edit()
                    .putLong("run_done_id", bootId())
                    .putLong("run_ok_id", bootId())
                    .remove("run_boot")
                    .remove("run_ok_boot")
                    .apply()
                addLogOnMain("[migrate] live root re-detected - run state restored for this boot")
            } else if (out.isNotEmpty()) {
                /* No live root now.  If the legacy attempt happened in
                 * this same system boot, the kernel one-shot is consumed:
                 * keep the run blocked for this boot, never re-armed. */
                val legacy = prefs.getInt("run_boot", -1)
                val currentBoot = runCatching {
                    Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT)
                }.getOrDefault(-1)
                if (legacy >= 0 && legacy == currentBoot) {
                    prefs.edit()
                        .putLong("run_done_id", bootId())
                        .remove("run_boot")
                        .remove("run_ok_boot")
                        .apply()
                    addLogOnMain("[migrate] no live root, but this boot already saw an attempt - reboot before running again")
                } else {
                    prefs.edit().remove("run_boot").remove("run_ok_boot").apply()
                    addLogOnMain("[migrate] stale legacy flags cleared - starting fresh")
                }
            }
            /* an empty probe stays undecidable: keep the legacy flags and
             * retry on the next resume */
            recoveryBusy = false
            withContext(Dispatchers.Main) { refreshRunDone() }
        }
    }

    private fun doSoftReboot() {
        val s = requireService("soft reboot") ?: return
        softRebootPrompt = false
        prefs.edit().putLong("soft_reboot_id", bootId()).apply()
        addLog("[softreboot] restarting the Android framework; root and jailbreak stay up")
        lifecycleScope.launch(Dispatchers.IO) {
            val cmd = "SU=/data/adb/ksu/bin/su; [ -x \"\$SU\" ] || SU=/system/bin/su; " +
                "[ -x \"\$SU\" ] || SU=su; \"\$SU\" -c '" +
                "B=\$(pidof system_server); " +
                "setprop ctl.restart zygote; " +
                "sleep 12; " +
                "C=\$(pidof system_server); " +
                "if [ -n \"\$B\" ] && [ \"\$B\" = \"\$C\" ]; then " +
                "kill -9 \$C; echo fallback-kill; " +
                "else echo zygote-restarted; fi' 2>&1"
            val out = try {
                s.runCmd(cmd).trim()
            } catch (t: Throwable) {
                "error $t"
            }
            addLogOnMain("[softreboot] done ($out)")
        }
    }

    @Composable
    private fun RerunPromptDialog() {
        OverlayDialog(
            show = rerunPrompt,
            title = "Reboot required",
            onDismissRequest = { rerunPrompt = false },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Text(
                        "A run already happened in this boot.\n\n" +
                            "The exploit leaves a zombie behind and consumes its one-shot state, " +
                            "so a second attempt in the same boot cannot succeed and can panic the kernel.\n\n" +
                            "Reboot the phone, then tap one-tap jailbreak again.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Got it",
                        onClick = { rerunPrompt = false },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        )
    }

    /* ------------------------------------------------------------------ */
    /* Updater (v15)                                                       */
    /* ------------------------------------------------------------------ */

    private var updatePrompt by mutableStateOf<Updater.UpdateInfo?>(null)
    private var updateProgress by mutableStateOf("")
    private var updateMismatch by mutableStateOf<Updater.UpdateInfo?>(null)
    private var latestSeen by mutableStateOf<String?>(null)
    private var latestPrerelease by mutableStateOf<Boolean?>(null)
    private var installedPrerelease by mutableStateOf<Boolean?>(null)
    private var betaChannel by mutableStateOf(false)
    private var updateChecking by mutableStateOf(false)
    private var latestNotes by mutableStateOf<String?>(null)
    private var changelogPrompt by mutableStateOf<String?>(null)

    @Composable
    private fun UpdateDialog() {
        val upd = updatePrompt
        OverlayDialog(
            show = upd != null,
            title = "Update available",
            onDismissRequest = { if (updateProgress.isEmpty()) updatePrompt = null },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Text(
                        "TimerRoot ${upd?.version}${channelTag(upd?.prerelease)} is available (installed: " +
                            "${BuildConfig.VERSION_NAME})\n\n" +
                            "The APK is downloaded from the GitHub release and " +
                            "installed only when it is signed with the same key " +
                            "as the installed app.",
                        fontSize = 13.sp
                    )
                    if (!upd?.notes.isNullOrBlank()) {
                        Text(
                            "What's new in v${upd?.version}${channelTag(upd?.prerelease)}:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        /* v30: long notes scroll inside a bounded region, so
                         * the buttons below stay reachable. */
                        Column(
                            modifier = Modifier
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            NotesText(upd.notes)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (updateProgress.isNotEmpty()) {
                        Text(updateProgress, fontSize = 13.sp)
                    } else {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Install now",
                            onClick = { upd?.let(::installUpdate) },
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Later",
                            onClick = { updatePrompt = null }
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Never remind me again",
                            onClick = {
                                prefs.edit().putString("update_skip_tag", upd?.tag).apply()
                                updatePrompt = null
                            }
                        )
                    }
                }
            }
        )
    }

    @Composable
    private fun UpdateMismatchDialog() {
        val upd = updateMismatch
        OverlayDialog(
            show = upd != null,
            title = "Signature mismatch",
            onDismissRequest = { updateMismatch = null },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Text(
                        "The downloaded APK is signed with a different key " +
                            "than the installed app, so it cannot install as " +
                            "an update.\n\n" +
                            "A fresh install wipes TimerRoot data, including " +
                            "the one-run-per-boot lock - reboot before the " +
                            "next ritual run after installing.\n\n" +
                            "The APK was saved to Download/TimerRoot-${upd?.tag}.apk:\n\n" +
                            "1. Tap Uninstall below and confirm.\n" +
                            "2. Install the saved APK from your file manager " +
                            "(or adb install).\n" +
                            "3. Reboot, then run the ritual as usual.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Uninstall",
                        onClick = {
                            updateMismatch = null
                            runCatching {
                                startActivity(
                                    Intent(
                                        Intent.ACTION_DELETE,
                                        Uri.parse("package:$packageName")
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Cancel",
                        onClick = { updateMismatch = null }
                    )
                }
            }
        )
    }

    /* Download the release APK into the cache dir, compare signatures and
     * either hand it to the system installer (same key) or fall back to
     * the guided uninstall/reinstall flow (different key - only possible
     * for pre-v0.26.0 debug builds; since then debug and release share
     * the keystore). */
    private fun installUpdate(upd: Updater.UpdateInfo) {
        val file = File(cacheDir, "updates/TimerRoot-${upd.tag}.apk")
        if (file.isFile && upd.size > 0 && file.length() == upd.size) {
            afterDownload(upd, file)
            return
        }
        updateProgress = "Downloading..."
        lifecycleScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                Updater.download(upd.url, file) { done, total ->
                    postUi {
                        updateProgress = if (total > 0)
                            "Downloading... ${done * 100 / total} %"
                        else
                            "Downloading... ${done / (1024 * 1024)} MB"
                    }
                }
            }.isSuccess
            postUi {
                updateProgress = ""
                if (!ok) {
                    updatePrompt = null
                    Toast.makeText(
                        this@MainActivity,
                        "Download failed - check the connection",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    afterDownload(upd, file)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun afterDownload(upd: Updater.UpdateInfo, file: File) {
        if (Updater.signaturesMatch(this, file)) {
            installApk(file)
        } else {
            /* cacheDir dies with the app, so park the APK in Download for
             * the manual reinstall that follows the uninstall. */
            val saved = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "TimerRoot-${upd.tag}.apk"
            )
            val savedOk = runCatching { file.copyTo(saved, overwrite = true) }.isSuccess
            updatePrompt = null
            if (savedOk) {
                updateMismatch = upd
            } else {
                Toast.makeText(
                    this,
                    "Could not save the APK to Download",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /* v34: with the shell service bound the update installs directly, the
     * way adb does it: relay the APK into /data/local/tmp (the same relay
     * that already moves the core binary) and let the shell context drive
     * the package manager directly - no confirmation dialog, no
     * unknown-sources detour. Any failure falls back to the system
     * installer intent. */
    private fun installApk(file: File) {
        val s = service
        if (s == null) {
            installApkViaIntent(file)
            return
        }
        updateProgress = "Installing..."
        lifecycleScope.launch(Dispatchers.IO) {
            val out = runCatching {
                val dst = "$TMP/TimerRoot-update.apk"
                s.truncate(dst)
                file.inputStream().use { input ->
                    val buf = ByteArray(CHUNK)
                    var off = 0
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        val chunk = if (n == buf.size) buf else buf.copyOf(n)
                        s.put(dst, off, chunk)
                        off += n
                    }
                }
                s.runCmd("pm install -r $dst 2>&1; rm -f $dst")
            }.getOrNull()
            postUi {
                updateProgress = ""
                if (out != null && out.contains("Success")) {
                    addLog("[update] direct install ok - the app should restart")
                } else {
                    addLog("[update] direct install failed ($out) - falling back")
                    Toast.makeText(
                        this@MainActivity,
                        "Direct install failed - opening the system installer",
                        Toast.LENGTH_LONG
                    ).show()
                    installApkViaIntent(file)
                }
            }
        }
    }

    private fun installApkViaIntent(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.updatefileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (packageManager.canRequestPackageInstalls()) {
            runCatching { startActivity(intent) }
        } else {
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            Toast.makeText(
                this,
                "Allow installing from TimerRoot, then tap Install now again",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /* v17c: channel label suffix - the channel comes from the GitHub
     * prerelease flag of the release, not from the version shape. */
    private fun channelTag(prerelease: Boolean?): String =
        if (prerelease == null) "" else " (${Updater.channelOf(prerelease)})"

    private fun manualUpdateCheck() {
        if (updateChecking) return
        updateChecking = true
        lifecycleScope.launch(Dispatchers.IO) {
            val upd = runCatching { Updater.fetchLatest(betaChannel) }.getOrNull()
            postUi {
                updateChecking = false
                if (upd == null) {
                    Toast.makeText(
                        this@MainActivity,
                        "Update check failed - offline or rate-limited",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    latestSeen = upd.version
                    latestPrerelease = upd.prerelease
                    if (upd.version == BuildConfig.VERSION_NAME) installedPrerelease = upd.prerelease
                    latestNotes = upd.notes
                    if (Updater.isNewer(BuildConfig.VERSION_NAME, upd.version)) {
                        updatePrompt = upd
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "TimerRoot is up to date ${BuildConfig.VERSION_NAME}${channelTag(installedPrerelease)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    /* v16d: release notes with tappable links - the GitHub release body
     * carries URLs (the full-changelog compare link) that plain text alone
     * would leave inert.  v27: structured rendering - line by line, so
     * bullet lists get spacing and a slightly larger font instead of one
     * wall of 12sp text. */
    @Composable
    private fun NotesText(text: String) {
        val linkColor = COUITheme.colorScheme.primary
        SelectionContainer {
            Column {
                text.lines().forEachIndexed { idx, line ->
                    if (line.isBlank()) return@forEachIndexed
                    /* v30: markdown bullet lead-ins with a double-asterisk
                     * pair render as a bold lead, asterisks removed. */
                    val lead = Regex("^- \\*\\*(.+?)\\*\\*(.*)$").find(line)
                    val plain = if (lead != null)
                        "- " + lead.groupValues[1] + lead.groupValues[2]
                    else
                        line
                    val boldEnd = if (lead != null) 2 + lead.groupValues[1].length else 0
                    val annotated = remember(line, linkColor) {
                        buildAnnotatedString {
                            append(plain)
                            for (m in Regex("https?://[^ ]+").findAll(plain)) {
                                val url = m.value.trimEnd('.', ')', ',', ';')
                                addLink(
                                    LinkAnnotation.Url(
                                        url,
                                        TextLinkStyles(
                                            SpanStyle(
                                                color = linkColor,
                                                textDecoration = TextDecoration.Underline
                                            )
                                        )
                                    ),
                                    m.range.first,
                                    m.range.first + url.length
                                )
                            }
                            if (lead != null && boldEnd > 2) {
                                addStyle(SpanStyle(fontWeight = FontWeight.Bold), 2, boldEnd)
                            }
                        }
                    }
                    if (idx > 0) {
                        val prevBlank = text.lines().getOrNull(idx - 1).isNullOrBlank()
                        Spacer(Modifier.height(if (prevBlank) 10.dp else 3.dp))
                    }
                    Text(
                        annotated,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = if (plain.startsWith("- "))
                            Modifier.padding(start = 10.dp)
                        else
                            Modifier
                    )
                }
            }
        }
    }

    @Composable
    private fun ChangelogDialog() {
        val notes = changelogPrompt
        OverlayDialog(
            show = notes != null,
            title = if (latestSeen.isNullOrBlank()) "What's new"
            else "What's new in v$latestSeen${channelTag(latestPrerelease)}",
            onDismissRequest = { changelogPrompt = null },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    /* v30: the title names the release; long notes scroll
                     * inside a bounded region so the button stays put. */
                    Column(
                        modifier = Modifier
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        NotesText(notes ?: "")
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Got it",
                        onClick = { changelogPrompt = null },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        )
    }

    /* v16: show the latest release's notes on demand; the silent startup
     * check usually already filled latestNotes, so this is instant.
     * v27: the cache shows instantly AND every tap refreshes, so a
     * just-published release shows up without an app restart. */
    private fun manualChangelogCheck() {
        latestNotes?.takeIf { it.isNotBlank() }?.let { changelogPrompt = it }
        if (updateChecking) return
        updateChecking = true
        lifecycleScope.launch(Dispatchers.IO) {
            val upd = runCatching { Updater.fetchLatest(betaChannel) }.getOrNull()
            postUi {
                updateChecking = false
                if (upd == null) {
                    if (changelogPrompt == null) {
                        Toast.makeText(
                            this@MainActivity,
                            "Could not load the release notes - offline or rate-limited",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    latestSeen = upd.version
                    latestPrerelease = upd.prerelease
                    if (upd.version == BuildConfig.VERSION_NAME) installedPrerelease = upd.prerelease
                    latestNotes = upd.notes
                    if (upd.notes.isBlank()) {
                        Toast.makeText(
                            this@MainActivity,
                            "The latest release has no notes",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        changelogPrompt = upd.notes
                    }
                }
            }
        }
    }

    /* Field lesson (Nov 15): a fake_uid timing miss leaves the core holding
     * a zombie and demanding a reboot; that outcome lived only in the
     * scrolling log and looked like a plain failure. Catch the core's own
     * markers and turn them into button text plus a dialog. */
    private fun trackOutcome(line: String) {
        if (line.contains("[core] start")) missHold = false
        if (line.contains("burst hit=0") || line.contains("holding zombie")) missHold = true
        if (missHold && line.contains("[core] exit=")) {
            postUi {
                missPrompt = true
                runStage = "Miss - reboot before rerun"
            }
        }
    }

    /* The timing race of the fake-cred stages misses far more often while
     * the system churns (boot storm, telephony ANR waves) or right after
     * boot. /proc/loadavg and /proc/uptime are world-readable, so the app
     * can ask for a quiet window without touching Shizuku. */
    private val quietLoadLimit = 6.0
    private val quietBootSeconds = 150L

    /* v3 lesson (live, boot 10): the load check never fired because the
     * app process itself could not read the load average and v1/v2
     * silently measured -1 even under heavy storm load. The load now
     * comes from the shell user service - the same context that runs
     * the core - and an unreadable load warns instead of running. */
    private fun systemLoad1(s: IIcService): Double = runCatching {
        s.runCmd("cat /proc/loadavg").trim().split(" ").firstOrNull()?.toDoubleOrNull() ?: -1.0
    }.getOrNull() ?: -1.0

    /* v1 lesson: /proc/uptime prints "123.45 678.90" and parsing the
     * decimal first token as an integer always failed, so the age read as
     * -1 and the fresh-boot check never fired. Uptime now comes from the
     * system clock - no file access, no parsing, no failure mode. */
    private fun bootAgeSeconds(): Long = SystemClock.elapsedRealtime() / 1000

    private fun maybeWarnThenRun() {
        if (scopePending && !scopeConfirmed) {
            scopePrompt = buildString {
                append("This device or kernel is not known-supported: the run is out of ")
                append("scope and privilege escalation will most likely fail; writes to wrong ")
                append("addresses may leave the device misbehaving.\n\n")
                append("Detected:\n· ")
                append(scopeReasons)
                append("\n\nOnly continue if you are porting on purpose and accept the risk.")
            }
            addLog("[preflight] out of scope - run held for explicit confirmation")
            return
        }
        val s = requireService("preflight") ?: return
        lifecycleScope.launch {
            val (load, age) = withContext(Dispatchers.IO) {
                systemLoad1(s) to bootAgeSeconds()
            }
            addLog("[preflight] 1-min load $load, boot age ${age}s")
            val loadBusy = load >= quietLoadLimit
            val bootFresh = age in 0..quietBootSeconds
            val loadUnknown = load < 0
            if (!loadBusy && !bootFresh && !loadUnknown) {
                runCore()
                return@launch
            }
            val parts = mutableListOf<String>()
            if (loadUnknown) parts.add("The system load could not be measured (safe-guard warning).")
            if (loadBusy) parts.add("System load is high (1-min load ${"%.1f".format(load)}, quiet limit ${"%.1f".format(quietLoadLimit)}).")
            if (bootFresh) parts.add("This boot is only ${age}s old (settle time ${quietBootSeconds}s).")
            loadPrompt = "The exploit's timing race is far more likely to miss on a busy or freshly " +
                "booted system: a miss burns the one run of this boot, a bad hit can panic the kernel.\n\n" +
                parts.joinToString("\n\n") +
                "\n\nIf you can, wait for a quiet window, then run."
        }
    }
    @Composable
    private fun LoadPromptDialog() {
        OverlayDialog(
            show = loadPrompt != null,
            title = "System is busy",
            onDismissRequest = { loadPrompt = null },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Text(loadPrompt ?: "", fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Run anyway",
                        onClick = {
                            loadPrompt = null
                            runCore()
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Wait for a quiet window",
                        onClick = { loadPrompt = null }
                    )
                }
            }
        )
    }

    /* v25: the out-of-scope confirm dialog. The confirm button re-enters
     * the preflight flow, so the busy-load check still runs afterwards. */
    @Composable
    private fun ScopePromptDialog() {
        OverlayDialog(
            show = scopePrompt != null,
            title = "Out of scope - confirm to run",
            onDismissRequest = { scopePrompt = null },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Text(scopePrompt ?: "", fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Run anyway",
                        onClick = {
                            scopeConfirmed = true
                            scopePrompt = null
                            maybeWarnThenRun()
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Not today",
                        onClick = { scopePrompt = null }
                    )
                }
            }
        )
    }

    @Composable
    private fun MissPromptDialog() {
        OverlayDialog(
            show = missPrompt,
            title = "Timing miss - reboot required",
            onDismissRequest = { missPrompt = false },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Text(
                        "The core missed its timing window (fake_uid stage) and is holding a zombie " +
                            "as the one-run-per-boot marker.\n\n" +
                            "A rerun in this boot cannot succeed and can panic the kernel. " +
                            "Reboot the phone, let it settle for 2-3 minutes, then tap one-tap jailbreak again.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Got it",
                        onClick = { missPrompt = false },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        )
    }
    private fun postUi(block: () -> Unit) {
        runOnUiThread(block)
    }

    private fun doCleanup(s: IIcService, apk: Boolean) {
        lifecycleScope.launch {
            val out = withContext(Dispatchers.IO) {
                try {
                    val stamp = (System.currentTimeMillis() / 1000).toString()
                    val script = "$TMP/ic-clean-a$stamp.sh"
                    s.truncate(script)
                    push(s, "cleanup.sh", script)
                    s.makeExec(script)
                    s.runCmd("sh $script --logs" + (if (apk) " --apk" else "") + " 2>&1")
                } catch (t: Throwable) {
                    "Cleanup failed:\n" + explainError(t, "cleaning temp files")
                }
            }
            addLog("[cleanup]\n$out")
        }
    }

    private fun push(s: IIcService, asset: String, dst: String) {
        assets.open(asset).use { input ->
            /* Shell assets must reach the phone with LF endings.  A Windows
             * build machine with core.autocrlf=true ships them with CRLF, and
             * Android's mksh then fails on the blank line (a lone \r is read
             * as a command) and on "case ... in\r" ("syntax error: unexpected
             * 'in'"), which aborts the whole KSU handoff.  Binary assets are
             * streamed untouched. */
            if (asset.endsWith(".sh")) {
                val bytes = input.readBytes().toString(Charsets.UTF_8)
                    .replace("\r\n", "\n")
                    .toByteArray(Charsets.UTF_8)
                var off = 0
                while (off < bytes.size) {
                    val n = minOf(CHUNK, bytes.size - off)
                    s.put(dst, off, bytes.copyOfRange(off, off + n))
                    off += n
                }
                return
            }
            val buf = ByteArray(CHUNK)
            var off = 0
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                val chunk = if (n == buf.size) buf else buf.copyOf(n)
                s.put(dst, off, chunk)
                off += n
            }
        }
    }

    private fun addLog(s: String) {
        addLogOnMain(s)
    }

    private fun addLogOnMain(s: String) {
        runOnUiThread { raw(s + "\n") }
    }

    private fun raw(s: String) {
        partialLine.append(s)
        val cut = partialLine.lastIndexOf('\n')
        if (cut < 0) return
        val complete = partialLine.substring(0, cut)
        partialLine.delete(0, cut + 1)
        complete.split('\n').forEach { line0 ->
            val line =
                if (line0.contains("am start failed; handoff incomplete") ||
                    line0.contains("am force-stop failed"))
                    "[i] manager autostart skipped (opening the installed manager instead - root is live)"
                else line0
            if (line.isNotEmpty()) {
                logs.add(stampTime(line))
                updateStage(line)
            }
        }
        while (logs.size > MAX_LOG_LINES) logs.removeAt(0)
    }

    /**
     * Maps the core's log lines onto RUN_STAGES so the main button can show
     * the live phase.  Only forward moves count: most stages retry, and the
     * "plant" lines of a later stage repeat earlier text.
     */
    private fun updateStage(line: String) {
        trackOutcome(line)
        if (ROOT_PROOF_MARKERS.any { line.contains(it) }) rootConfirmed = true
        if (line.contains("[core] exit=")) {
            val rc = line.substringAfter("[core] exit=").trim()
            runStage = when {
                rc == "0" -> "Done"
                rootConfirmed -> "Done (manager autostart incomplete)"
                else -> "Failed (exit=$rc)"
            }
            busy = false
            if (rootConfirmed) markRunOk()
            refreshRunDone()
            refreshStaticState()
            if (rootConfirmed && !managerOpenedThisRun) {
                managerOpenedThisRun = true
                openKsuManager()
            }
            if (rootConfirmed) notifyRootLive()
            addLog(
                "======== run end after ${
                    (SystemClock.elapsedRealtime() - runStartMs) / 1000}s: " +
                    "$runStage ========")
            // Keep the outcome on the button for a moment, then go back idle.
            lifecycleScope.launch {
                delay(4000)
                if (!busy) runStage = ""
            }
            return
        }
        if (!busy) return
        RUN_STAGES.forEachIndexed { idx, stage ->
            if (idx > runStageIndex && stage.second.any { line.contains(it) }) {
                runStageIndex = idx
                runStage = "${idx + 1}/${RUN_STAGES.size} ${stage.first}"
            }
        }
    }

    private val partialLine = StringBuilder()

    private enum class AppTab(val label: String, val icon: ImageVector) {
        HOME("Home", Icons.Filled.Home),
        LOGS("Logs", COUIIcons.Notes),
        ABOUT("About", Icons.Filled.Info)
    }

    private companion object {
        private const val REQ_SHIZUKU = 1001
        private const val TMP = "/data/local/tmp"
        private const val CHUNK = 256 * 1024
        private const val ATTEMPTS = 16
        private const val MAX_LOG_LINES = 20_000
        private const val DEFAULT_CMD =
            "id; cat /proc/self/attr/current; getenforce; echo LPE-PROOF"
        private const val SCHEME_ASSET = "run_core.sh"

        /** Ordered phases of a run; the button shows "<n>/<total> <label>". */
        private val RUN_STAGES: List<Pair<String, List<String>>> = listOf(
            "Start" to listOf("[core] start"),
            "Env check" to listOf("[anchors]", "[syms]"),
            "Calibrate memory" to listOf("[ids] trace", "[base] made"),
            "Leak cred" to listOf("[leak] child exec"),
            "Locate cred" to listOf("[cred] attempt="),
            "Set SELinux" to listOf("stage selinux", "set SELinux permissive"),
            "Zero-write vr.ko" to listOf("vr: nulling", "stage vr_sys_exit_funcs"),
            "Fake cred" to listOf("stage fake_cred"),
            "Escalate uid" to listOf("stage fake_uid"),
            "Escalate euid" to listOf("stage fake_euid"),
            "Run payload" to listOf("exec payload", "[fullshell]", "plant verified"),
            "Done" to listOf("handoff OK", "ROOT_SHELL_OK", "LPE-PROOF", "module resident"),
        )

        /* Log lines that prove root was reached, whatever the final exit
         * code says (the manager autostart can still fail afterwards). */
        private val ROOT_PROOF_MARKERS = listOf(
            "LPE-PROOF", "uid=0(root)", "module resident"
        )

        private val KSU_NAMES = listOf(
            "Auto-detect", "KernelSU", "KernelSU-Next", "SukiSU Ultra",
            "BakaSU", "ReSukiSU (old pkg name)", "SakiSU", "FinaSU", "7kimisu",
            "7kimisu (alternate pkg name)", "vivoKSU"
        )
        /* Keep this list in step with KNOWN in assets/ksu_handoff.sh: the
         * script can only hand off to managers it knows, so a manager that
         * is missing here is also unreachable there. 7kimisu ships two ids. */
        private val KSU_PKGS = listOf(
            "",
            "me.weishu.kernelsu", "com.rifsxd.ksunext", "com.sukisu.ultra",
            "org.bakasu.bakasu", "com.resukisu.resukisu",
            "com.sakisu.sakisu", "me.yuki.fina", "com.sevenk.core",
            "com.sevenkimimasy.love", "me.inkdye.vivoksu"
        )
        /** Selecting one id of a two-id manager also accepts the other id. */
        private val KSU_ALIASES = mapOf(
            "com.sevenk.core" to listOf("com.sevenkimimasy.love"),
            "com.sevenkimimasy.love" to listOf("com.sevenk.core")
        )

        private val licenseItems = listOf(
            LicenseItem(
                name = "TimerRoot (this project)",
                author = "Edge 50 Ultra port by TurboRCT; upstream kzktor/ic-timer-lpe, by Coolapk @散花神木 (Sanhua Shenmu)",
                website = PROJECT_URL,
                licenses = listOf(LicenseText("GPL-3.0", asset = "licenses/GPL-3.0.txt"))
            ),
            LicenseItem(
                name = "YuKongA/ghostlock-app",
                author = "YuKongA, Apache-2.0; this project's full-package decompression and " +
                    "boot.img extraction code references this project",
                website = "https://github.com/YuKongA/ghostlock-app",
                licenses = listOf(
                    LicenseText("Apache-2.0", asset = "licenses/Apache-2.0.txt")
                )
            ),
            LicenseItem(
                name = "molanp/CVE-2026-98260-vivo-s60-poc",
                author = "S60 PoC, MIT-licensed reference implementation",
                website = "https://github.com/molanp/CVE-2026-98260-vivo-s60-poc",
                licenses = listOf(LicenseText("MIT", asset = "licenses/MIT.txt"))
            ),
            LicenseItem(
                name = "woshigenm/CVE-2026-98260",
                author = "MIT-licensed reference implementation",
                website = "https://github.com/woshigenm/CVE-2026-98260",
                licenses = listOf(LicenseText("MIT", asset = "licenses/MIT.txt"))
            ),
            LicenseItem(
                name = "whooslizi/infinitycore-x200pro",
                author = "GPL-3.0-licensed reference implementation",
                website = "https://github.com/whooslizi/infinitycore-x200pro",
                licenses = listOf(LicenseText("GPL-3.0", asset = "licenses/GPL-3.0.txt"))
            ),
            LicenseItem(
                name = "nisu123f/dada-cve-AN00",
                author = "Reference implementation",
                website = "https://github.com/nisu123f/dada-cve-AN00"
            ),
            LicenseItem(
                name = "kpgc10kai/TimerRoot",
                author = "Reference implementation",
                website = "https://github.com/kpgc10kai/TimerRoot"
            ),
            LicenseItem(
                name = "Yunnijian/Shark",
                author = "UI port source, GPL-3.0",
                website = "https://github.com/Yunnijian/Shark",
                licenses = listOf(LicenseText("GPL-3.0", asset = "licenses/GPL-3.0.txt"))
            )
        )
    }
}
