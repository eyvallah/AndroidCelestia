/*
 * MainActivity.kt
 *
 * Copyright (C) 2001-2020, Celestia Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package space.celestia.mobilecelestia

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.graphics.contains
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.coroutines.*
import org.json.JSONObject
import space.celestia.mobilecelestia.browser.*
import space.celestia.mobilecelestia.celestia.CelestiaFragment
import space.celestia.mobilecelestia.celestia.CelestiaView
import space.celestia.mobilecelestia.common.Cleanable
import space.celestia.mobilecelestia.common.Poppable
import space.celestia.mobilecelestia.control.BottomControlFragment
import space.celestia.mobilecelestia.control.CameraControlAction
import space.celestia.mobilecelestia.control.CameraControlContainerFragment
import space.celestia.mobilecelestia.control.CameraControlFragment
import space.celestia.mobilecelestia.core.*
import space.celestia.mobilecelestia.eventfinder.EventFinderContainerFragment
import space.celestia.mobilecelestia.eventfinder.EventFinderInputFragment
import space.celestia.mobilecelestia.eventfinder.EventFinderResultFragment
import space.celestia.mobilecelestia.favorite.*
import space.celestia.mobilecelestia.help.HelpAction
import space.celestia.mobilecelestia.help.HelpFragment
import space.celestia.mobilecelestia.info.InfoFragment
import space.celestia.mobilecelestia.info.model.*
import space.celestia.mobilecelestia.loading.LoadingFragment
import space.celestia.mobilecelestia.resource.AsyncListFragment
import space.celestia.mobilecelestia.resource.DestinationDetailFragment
import space.celestia.mobilecelestia.resource.ResourceFragment
import space.celestia.mobilecelestia.resource.model.ResourceCategory
import space.celestia.mobilecelestia.resource.model.ResourceItem
import space.celestia.mobilecelestia.resource.model.ResourceManager
import space.celestia.mobilecelestia.search.SearchFragment
import space.celestia.mobilecelestia.settings.*
import space.celestia.mobilecelestia.share.ShareAPI
import space.celestia.mobilecelestia.share.ShareAPIService
import space.celestia.mobilecelestia.share.URLCreationResponse
import space.celestia.mobilecelestia.share.URLResolultionResponse
import space.celestia.mobilecelestia.toolbar.ToolbarAction
import space.celestia.mobilecelestia.toolbar.ToolbarFragment
import space.celestia.mobilecelestia.utils.*
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(R.layout.activity_main),
    ToolbarFragment.Listener,
    InfoFragment.Listener,
    SearchFragment.Listener,
    BottomControlFragment.Listener,
    BrowserCommonFragment.Listener,
    CameraControlFragment.Listener,
    HelpFragment.Listener,
    FavoriteFragment.Listener,
    FavoriteItemFragment.Listener,
    SettingsItemFragment.Listener,
    SettingsMultiSelectionFragment.Listener,
    SettingsSingleSelectionFragment.Listener,
    SettingsCurrentTimeFragment.Listener,
    AboutFragment.Listener,
    AppStatusReporter.Listener,
    CelestiaFragment.Listener,
    SettingsDataLocationFragment.Listener,
    SettingsCommonFragment.Listener,
    SettingsCommonFragment.DataSource,
    EventFinderInputFragment.Listener,
    EventFinderResultFragment.Listener,
    SettingsLanguageFragment.Listener,
    SettingsLanguageFragment.DataSource,
    ResourceFragment.Listener,
    AsyncListFragment.Listener<Any>,
    DestinationDetailFragment.Listener {

    private val preferenceManager by lazy { PreferenceManager(this, "celestia") }
    private val settingManager by lazy { PreferenceManager(this, "celestia_setting") }
    private val legacyCelestiaParentPath by lazy { this.filesDir.absolutePath }
    private val celestiaParentPath by lazy { this.noBackupFilesDir.absolutePath }
    private val favoriteJsonFilePath by lazy { "${filesDir.absolutePath}/favorites.json" }

    private val core by lazy { CelestiaAppCore.shared() }
    private var currentSelection: CelestiaSelection? = null

    private val backStack: MutableList<Fragment> = ArrayList()

    private var interactionBlocked = false

    private var readyForInteraction = false
    private var scriptOrURLPath: String? = null

    private val celestiaConfigFilePath: String
        get() {
            val custom = customConfigFilePath
            if (custom != null)
                return custom
            return "$celestiaParentPath/$CELESTIA_DATA_FOLDER_NAME/$CELESTIA_CFG_NAME"
        }

    private val celestiaDataDirPath: String
        get() {
            val custom = customDataDirPath
            if (custom != null)
                return custom
            return "$celestiaParentPath/$CELESTIA_DATA_FOLDER_NAME"
        }

    private val fontDirPath: String
        get() = "$celestiaParentPath/$CELESTIA_FONT_FOLDER_NAME"

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        // We don't need to recover when we get killed
        super.onCreate(null)

        Log.d(TAG, "Creating MainActivity")

        if (!AppCenter.isConfigured()) {
            AppCenter.start(
                application, "d1108985-aa25-4fb5-9269-31a70a87d28e",
                Analytics::class.java, Crashes::class.java
            )

            Crashes.getMinidumpDirectory().thenAccept { path ->
                if (path != null) {
                    CrashHandler.setupNativeCrashesListener(path)
                }
            }
        }

        // Handle notch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        AppStatusReporter.shared().register(this)

        // Handle notch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val rootView = findViewById<View>(android.R.id.content).rootView
            rootView.setOnApplyWindowInsetsListener { _, insets ->
                insets.displayCutout?.let {
                    applyCutout(it)
                }
                return@setOnApplyWindowInsetsListener insets
            }
        }

        // Add fragments
        supportFragmentManager
            .beginTransaction()
            .add(R.id.loading_fragment_container, LoadingFragment.newInstance())
            .commitAllowingStateLoss()

        findViewById<View>(R.id.overlay_container).setOnTouchListener { _, e ->
            if (e.actionMasked == MotionEvent.ACTION_UP) {
                popLastFromBackStackAndShow()
            }
            return@setOnTouchListener true
        }

        findViewById<View>(R.id.interaction_filter).setOnTouchListener { v, e ->
            if (interactionBlocked) { return@setOnTouchListener true }

            val point = PointF(e.x, e.y)
            val safeAreaParam = v.resources.displayMetrics.density * 16 // reserve 16 DP on each side for system gestures
            val safeArea = RectF(safeAreaParam, safeAreaParam, v.width - safeAreaParam, v.height - safeAreaParam)
            if (safeArea.contains(point)) {
                // Pass through
                return@setOnTouchListener false
            }
            // Excluded
            return@setOnTouchListener true
        }

        val currentState = AppStatusReporter.shared().state
        if (currentState == AppStatusReporter.State.LOADING_FAILURE || currentState == AppStatusReporter.State.EXTERNAL_LOADING_FAILURE) {
            // Celestia loading failure in the original activity
            Log.d(TAG, "Previous loading failed, unrecoverable.")
            celestiaUnrecoveableLoadingFailed()
            return
        }

        handleIntent(intent)

        if (currentState == AppStatusReporter.State.NONE || currentState == AppStatusReporter.State.EXTERNAL_LOADING) {
            Log.d(TAG, "Start fresh loading")
            AppStatusReporter.shared().updateState(AppStatusReporter.State.EXTERNAL_LOADING)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    copyAssetIfNeeded()
                    if (!isActive) return@launch
                    createAddonFolder()
                    if (!isActive) return@launch
                    loadConfig()
                    if (!isActive) return@launch
                    with(Dispatchers.Main) {
                        loadConfigSuccess()
                    }
                } catch (error: Throwable) {
                    withContext(Dispatchers.Main) {
                        AppStatusReporter.shared().updateState(AppStatusReporter.State.EXTERNAL_LOADING_FAILURE)
                        loadConfigFailed(error)
                    }
                }
            }
        } else {
            Log.d(TAG, "Configuration already loaded")
            loadConfigSuccess()
            if (currentState == AppStatusReporter.State.SUCCESS) {
                Log.d(TAG, "Celestia already loaded")
                celestiaLoadingSucceeded()
            }
        }
    }

    override fun onDestroy() {
        AppStatusReporter.shared().unregister(this)

        Log.d(TAG, "Destroying MainActivity")

        super.onDestroy()
    }

    override fun onBackPressed() {
        var frag = supportFragmentManager.findFragmentById(R.id.normal_end_container)
        if (frag == null)
            frag = supportFragmentManager.findFragmentById(R.id.toolbar_end_container)

        if (frag is Poppable && frag.canPop()) {
            frag.popLast()
        } else {
            popLastFromBackStackAndShow()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        // Enables sticky immersive mode.
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    override fun celestiaLoadingProgress(status: String) {}

    override fun celestiaLoadingStateChanged(newState: AppStatusReporter.State) {
        if (newState == AppStatusReporter.State.SUCCESS) {
            celestiaLoadingSucceeded()
        } else if (newState == AppStatusReporter.State.EXTERNAL_LOADING_FAILURE) {
            celestiaUnrecoveableLoadingFailed()
        } else if (newState == AppStatusReporter.State.LOADING_FAILURE) {
            celestiaLoadingFailed()
        }
    }

    fun celestiaLoadingSucceeded() {
        lifecycleScope.launch {
            // hide the loading container
            findViewById<View>(R.id.loading_fragment_container).visibility = View.GONE

            // apply setting
            readSettings()

            // show onboard
            showWelcomeIfNeeded()

            // open url/script if present
            readyForInteraction = true
            runScriptOrOpenURLIfNeeded()
        }
    }

    fun celestiaUnrecoveableLoadingFailed() {
        AppStatusReporter.shared().updateStatus(CelestiaString("Please restart Celestia", ""))
    }

    fun celestiaLoadingFailed() {
        AppStatusReporter.shared().updateStatus(CelestiaString("Loading Celestia failed…", ""))
        val recoverable = customDataDirPath != null || customConfigFilePath != null
        if (!recoverable) {
            celestiaUnrecoveableLoadingFailed()
            return
        }
        lifecycleScope.launch {
            removeCelestiaFragment()
            showAlert(CelestiaString("Error loading data, fallback to original configuration.", "")) {
                // Fallback to default
                setConfigFilePath(null)
                setDataDirectoryPath(null)
                AppStatusReporter.shared().updateState(AppStatusReporter.State.EXTERNAL_LOADING_FINISHED)
                loadConfigSuccess()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val rootView = findViewById<View>(android.R.id.content).rootView
            rootView.rootWindowInsets.displayCutout?.let {
                applyCutout(it)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun applyCutout(cutout: DisplayCutout) {
        val density = resources.displayMetrics.density

        val ltr = resources.configuration.layoutDirection != View.LAYOUT_DIRECTION_RTL
        val safeInsetEnd = if (ltr) cutout.safeInsetRight else cutout.safeInsetLeft
        val safeInsetStart = if (ltr) cutout.safeInsetLeft else cutout.safeInsetRight

        val endView = findViewById<View>(R.id.normal_end_container)
        val toolbarView = findViewById<View>(R.id.toolbar_end_container)
        val bottomView = findViewById<View>(R.id.toolbar_bottom_container)

        val endNotch = findViewById<View>(R.id.end_notch)

        (endView.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.width = (300 * density).toInt() + safeInsetEnd
            endView.layoutParams = it
        }
        if (ltr)
            endView.setPadding(0, 0, safeInsetEnd, 0)
        else
            endView.setPadding(safeInsetEnd, 0, 0, 0)

        (toolbarView.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.width = (220 * density).toInt() + safeInsetEnd
            toolbarView.layoutParams = it
        }
        if (ltr)
            toolbarView.setPadding(0, 0, safeInsetEnd, 0)
        else
            toolbarView.setPadding(safeInsetEnd, 0, 0, 0)

        (bottomView.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.marginStart = safeInsetStart + (16 * density).toInt()
            it.bottomMargin = cutout.safeInsetBottom + (8 * density).toInt()
            bottomView.layoutParams = it
        }

        (endNotch.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.width = safeInsetEnd
            endNotch.layoutParams = it
        }
    }

    private fun removeCelestiaFragment() {
        supportFragmentManager.findFragmentById(R.id.celestia_fragment_container)?.let {
            supportFragmentManager.beginTransaction().hide(it).remove(it).commitAllowingStateLoss()
        }
    }

    private fun copyAssetIfNeeded() {
        AppStatusReporter.shared().updateStatus(CelestiaString("Copying data…", ""))
        if (preferenceManager[PreferenceManager.PredefinedKey.DataVersion] != CURRENT_DATA_VERSION) {
            // When version name does not match, copy the asset again
            copyAssetsAndRemoveOldAssets()
        }
    }

    private fun loadConfig() {
        availableInstalledFonts = mapOf(
            "ja" to Pair(
                FontHelper.FontCompat("$fontDirPath/NotoSansCJK-Regular.ttc", 0),
                FontHelper.FontCompat("$fontDirPath/NotoSansCJK-Bold.ttc", 0)
            ),
            "ko" to Pair(
                FontHelper.FontCompat("$fontDirPath/NotoSansCJK-Regular.ttc", 1),
                FontHelper.FontCompat("$fontDirPath/NotoSansCJK-Bold.ttc", 1)
            ),
            "zh_CN" to Pair(
                FontHelper.FontCompat("$fontDirPath/NotoSansCJK-Regular.ttc", 2),
                FontHelper.FontCompat("$fontDirPath/NotoSansCJK-Bold.ttc", 2)
            ),
            "zh_TW" to Pair(
                FontHelper.FontCompat("$fontDirPath/NotoSansCJK-Regular.ttc", 3),
                FontHelper.FontCompat("$fontDirPath/NotoSansCJK-Bold.ttc", 3)
            ),
            "ar" to Pair(
                FontHelper.FontCompat("$fontDirPath/NotoSansArabic-Regular.ttf", 0),
                FontHelper.FontCompat("$fontDirPath/NotoSansArabic-Bold.ttf", 0)
            )
        )
        defaultInstalledFont = Pair(
            FontHelper.FontCompat("$fontDirPath/NotoSans-Regular.ttf", 0),
            FontHelper.FontCompat("$fontDirPath/NotoSans-Bold.ttf", 0)
        )

        // Read custom paths here
        customConfigFilePath = preferenceManager[PreferenceManager.PredefinedKey.ConfigFilePath]
        customDataDirPath = preferenceManager[PreferenceManager.PredefinedKey.DataDirPath]

        val localeDirectory = File("${celestiaDataDirPath}/locale")
        if (localeDirectory.exists()) {
            val languageCodes = ArrayList((localeDirectory.listFiles { file ->
                return@listFiles file.isDirectory
            } ?: arrayOf()).map { file -> file.name })
            availableLanguageCodes = languageCodes.sorted()
        }

        languageOverride = preferenceManager[PreferenceManager.PredefinedKey.Language]
        enableMultisample = preferenceManager[PreferenceManager.PredefinedKey.MSAA] == "true"
        enableHiDPI = preferenceManager[PreferenceManager.PredefinedKey.FullDPI] == "true"

        // Load core library
        if (!celestiaLibraryLoaded) {
            System.loadLibrary("celestia")
            celestiaLibraryLoaded = true
        }

        AppStatusReporter.shared().updateState(AppStatusReporter.State.EXTERNAL_LOADING_FINISHED)
    }

    private fun showWelcomeIfNeeded() {
        if (preferenceManager[PreferenceManager.PredefinedKey.OnboardMessage] != "true") {
            preferenceManager[PreferenceManager.PredefinedKey.OnboardMessage] = "true"
            showHelp()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return

        Toast.makeText(this, CelestiaString("Opening external file or URL…", ""), Toast.LENGTH_SHORT).show()
        if (uri.scheme == "content") {
            handleContentURI(uri)
        } else if (uri.scheme == "cel") {
            requestOpenURL(uri.toString())
        } else if (uri.scheme == "https") {
            handleAppLink(uri)
        } else {
            // Cannot handle this URI scheme
            showAlert("Unknown URI scheme ${uri.scheme}")
        }
    }

    private fun handleAppLink(uri: Uri) {
        val path = uri.path ?: return
        val id = uri.getQueryParameter("id") ?: return
        val service = ShareAPI.shared.create(ShareAPIService::class.java)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = service.resolve(path, id).commonHandler(URLResolultionResponse::class.java)
                withContext(Dispatchers.Main) {
                    requestOpenURL(result.resolvedURL)
                }
            } catch (ignored: Throwable) {}
        }
    }

    private fun handleContentURI(uri: Uri) {
        // Content scheme, copy the resource to a temporary directory
        val itemName = uri.lastPathSegment
        // Check file name
        if (itemName == null) {
            showAlert("A filename needed to be present for ${uri.path}")
            return
        }
        // Check file type
        if (!itemName.endsWith(".cel") && !itemName.endsWith(".celx")) {
            showAlert("Celestia does not know how to open $itemName")
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val path = "${cacheDir.absolutePath}/$itemName"
                if (!FileUtils.copyUri(this@MainActivity, uri, path)) {
                    throw RuntimeException("Failed to open $itemName")
                }
                withContext(Dispatchers.Main) {
                    requestRunScript(path)
                }
            } catch (error: Throwable) {
                withContext(Dispatchers.Main) {
                    showError(error)
                }
            }
        }
    }

    private fun requestRunScript(path: String) {
        scriptOrURLPath = path
        if (readyForInteraction)
            runScriptOrOpenURLIfNeeded()
    }

    private fun requestOpenURL(url: String) {
        scriptOrURLPath = url
        if (readyForInteraction)
            runScriptOrOpenURLIfNeeded()
    }

    private fun runScriptOrOpenURLIfNeeded() {
        val uri = scriptOrURLPath ?: return

        // Clear existing
        scriptOrURLPath = null

        val isURL = uri.startsWith("cel://")
        showAlert(if (isURL) CelestiaString("Open URL?", "") else CelestiaString("Run Script?", "")) {
            CelestiaView.callOnRenderThread {
                if (isURL) {
                    core.goToURL(uri)
                } else {
                    core.runScript(uri)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun copyAssetsAndRemoveOldAssets() {
        try {
            // Remove old ones in filesDir, ignore any exception thrown
            File(legacyCelestiaParentPath, CELESTIA_DATA_FOLDER_NAME).deleteRecursively()
            File(legacyCelestiaParentPath, CELESTIA_FONT_FOLDER_NAME).deleteRecursively()
        } catch (ignored: Exception) {}
        AssetUtils.copyFileOrDir(this@MainActivity, CELESTIA_DATA_FOLDER_NAME, celestiaParentPath)
        AssetUtils.copyFileOrDir(this@MainActivity, CELESTIA_FONT_FOLDER_NAME, celestiaParentPath)
        preferenceManager[PreferenceManager.PredefinedKey.DataVersion] = CURRENT_DATA_VERSION
    }

    private fun readSettings() {
        val map = readDefaultSetting()
        val bools = HashMap<String, Boolean>()
        val ints = HashMap<String, Int>()
        val doubles = HashMap<String, Double>()

        fun getDefaultInt(key: String): Int? {
            val value = map[key]
            if (value is Int) {
                return value
            }
            return null
        }

        fun getDefaultDouble(key: String): Double? {
            val value = map[key]
            if (value is Double) {
                return value
            }
            return null
        }

        fun getDefaultBool(key: String): Boolean? {
            val value = getDefaultInt(key)
            if (value != null) {
                if (value == 1) { return true }
                if (value == 0) { return false }
            }
            return null
        }

        fun getCustomInt(key: String): Int? {
            val value = settingManager[PreferenceManager.CustomKey(key)] ?: return null
            return try {
                value.toInt()
            } catch (exp: NumberFormatException) {
                null
            }
        }

        fun getCustomBool(key: String): Boolean? {
            val value = getCustomInt(key)
            if (value != null) {
                if (value == 1) { return true }
                if (value == 0) { return false }
            }
            return null
        }

        fun getCustomDouble(key: String): Double? {
            val value = settingManager[PreferenceManager.CustomKey(key)] ?: return null
            return try {
                value.toDouble()
            } catch (exp: NumberFormatException) {
                null
            }
        }

        for (key in SettingsKey.allBooleanCases) {
            val def = getDefaultBool(key.valueString)
            if (def != null) {
                bools[key.valueString] = def
            }
            val cus = getCustomBool(key.valueString)
            if (cus != null)
                bools[key.valueString] = cus
        }

        for (key in SettingsKey.allIntCases) {
            val def = getDefaultInt(key.valueString)
            if (def != null) {
                ints[key.valueString] = def
            }
            val cus = getCustomInt(key.valueString)
            if (cus != null)
                ints[key.valueString] = cus
        }

        for (key in SettingsKey.allDoubleCases) {
            val def = getDefaultDouble(key.valueString)
            if (def != null) {
                doubles[key.valueString] = def
            }
            val cus = getCustomDouble(key.valueString)
            if (cus != null)
                doubles[key.valueString] = cus
        }

        for ((key, value) in bools) {
            core.setBooleanValueForField(key, value)
        }

        for ((key, value) in ints) {
            core.setIntValueForField(key, value)
        }

        for ((key, value) in doubles) {
            core.setDoubleValueForField(key, value)
        }
    }

    private fun readDefaultSetting(): Map<String, Any> {
        try {
            val jsonFileContent = AssetUtils.readFileToText(this, "defaults.json")
            val json = JSONObject(jsonFileContent)
            val map = HashMap<String, Any>()
            for (key in json.keys()) {
                map[key] = json[key]
            }
            return map
        } catch (ignored: Throwable) {}
        return mapOf()
    }

    private fun createAddonFolder() {
        try {
            var folder = getExternalFilesDir(CELESTIA_EXTRA_FOLDER_NAME)
            if (folder != null && (folder.exists() || folder.mkdir())) {
                addonPath = folder.absolutePath
            }
            folder = getExternalFilesDir(CELESTIA_SCRIPT_FOLDER_NAME)
            if (folder != null && (folder.exists() || folder.mkdir())) {
                extraScriptPath = folder.absolutePath
            }
        } catch (ignored: Throwable) {}
    }

    private fun loadConfigSuccess() {
        // Add gl fragment
        val celestiaFragment = CelestiaFragment.newInstance(
            celestiaDataDirPath,
            celestiaConfigFilePath,
            addonPath,
            enableMultisample,
            enableHiDPI,
            languageOverride
        )
        ResourceManager.shared.addonDirectory = addonPath
        supportFragmentManager
            .beginTransaction()
            .add(R.id.celestia_fragment_container, celestiaFragment)
            .commitAllowingStateLoss()
    }

    private fun loadConfigFailed(error: Throwable) {
        Log.e(TAG, "Initialization failed, $error")
        showError(error)
    }

    private fun showToolbar() {
        showEndFragment(ToolbarFragment.newInstance(listOf()), R.id.toolbar_end_container)
    }

    override fun onToolbarActionSelected(action: ToolbarAction) {
        executeToolbarAction(action)
    }

    private fun executeToolbarAction(action: ToolbarAction) {
        when (action) {
            ToolbarAction.Search -> {
                showSearch()
            }
            ToolbarAction.Time -> {
                showTimeControl()
            }
            ToolbarAction.Script -> {
                showScriptControl()
            }
            ToolbarAction.Browse -> {
                showBrowser()
            }
            ToolbarAction.Camera -> {
                showCameraControl()
            }
            ToolbarAction.Help -> {
                showHelp()
            }
            ToolbarAction.Favorite -> {
                showFavorite()
            }
            ToolbarAction.Setting -> {
                showSettings()
            }
            ToolbarAction.Share -> {
                showShare()
            }
            ToolbarAction.Home -> {
                CelestiaView.callOnRenderThread {
                    core.charEnter(CelestiaAction.Home.value)
                }
            }
            ToolbarAction.Event -> {
                showEventFinder()
            }
            ToolbarAction.Exit -> {
                moveTaskToBack(true)
            }
            ToolbarAction.Addons -> {
                showOnlineResource()
            }
        }
    }

    // Listeners...
    override fun onInfoActionSelected(action: InfoActionItem) {
        val selection = currentSelection ?: return
        when (action) {
            is InfoNormalActionItem -> {
                clearBackStack()
                core.simulation.selection = selection
                CelestiaView.callOnRenderThread { core.charEnter(action.item.value) }
            }
            is InfoSelectActionItem -> {
                clearBackStack()
                core.simulation.selection = selection
            }
            is InfoWebActionItem -> {
                clearBackStack()
                val url = selection.webInfoURL!!
                // show web info in browser
                openURL(url)
            }
            is SubsystemActionItem -> {
                addToBackStack()
                val entry = selection.`object` ?: return
                val browserItem = CelestiaBrowserItem(core.simulation.universe.getNameForSelection(selection), null, entry, core.simulation.universe)
                showEndFragment(SubsystemBrowserFragment.newInstance(browserItem))
                return
            }
            is AlternateSurfacesItem -> {
                val alternateSurfaces = selection.body?.alternateSurfaceNames ?: return
                val current = core.simulation.activeObserver.displayedSurface
                var currentIndex = 0
                if (current != "") {
                    val index = alternateSurfaces.indexOf(current)
                    if (index >= 0)
                        currentIndex = index + 1
                }
                val surfaces = ArrayList<String>()
                surfaces.add(CelestiaString("Default", ""))
                surfaces.addAll(alternateSurfaces)
                showSingleSelection(CelestiaString("Alternate Surfaces", ""), surfaces, currentIndex) { index ->
                    if (index == 0)
                        core.simulation.activeObserver.displayedSurface = ""
                    else
                        core.simulation.activeObserver.displayedSurface = alternateSurfaces[index - 1]
                }
            }
            is MarkItem -> {
                val markers = CelestiaFragment.availableMarkers
                showSingleSelection(CelestiaString("Mark", ""), markers, -1) { newIndex ->
                    if (newIndex >= CelestiaUniverse.MARKER_COUNT) {
                        core.simulation.universe.unmark(selection)
                    } else {
                        core.simulation.universe.mark(selection, newIndex)
                        core.showMarkers = true
                    }
                }
            }
        }
    }

    override fun onSearchItemSelected(text: String) {
        val sel = core.simulation.findObject(text)
        if (sel.isEmpty) {
            showAlert(CelestiaString("Object not found", ""))
            return
        }
        addToBackStack()
        showInfo(sel)
    }

    override fun onSearchItemSubmit(text: String) {
        onSearchItemSelected(text)
    }

    override fun onActionSelected(item: CelestiaAction) {
        CelestiaView.callOnRenderThread { core.charEnter(item.value) }
    }

    override fun onBottomControlHide() {
        hideOverlay(true)
    }

    override fun onBrowserItemSelected(item: BrowserItem) {
        if (!item.isLeaf) {
            val frag = supportFragmentManager.findFragmentById(R.id.normal_end_container)
            if (frag is BrowserRootFragment) {
                frag.pushItem(item.item)
            }
        } else {
            val obj = item.item.`object`
            if (obj != null) {
                val selection = CelestiaSelection.create(obj)
                if (selection != null) {
                    clearBackStack()
                    showInfo(selection)
                } else {
                    showAlert(CelestiaString("Object not found", ""))
                }
            } else {
                showAlert(CelestiaString("Object not found", ""))
            }
        }
    }

    override fun onCameraActionClicked(action: CameraControlAction) {
        CelestiaView.callOnRenderThread { core.simulation.reverseObserverOrientation() }
    }

    override fun onCameraActionStepperTouchDown(action: CameraControlAction) {
        CelestiaView.callOnRenderThread { core.keyDown(action.value) }
    }

    override fun onCameraActionStepperTouchUp(action: CameraControlAction) {
        CelestiaView.callOnRenderThread { core.keyUp(action.value) }
    }

    override fun onHelpActionSelected(action: HelpAction) {
        when (action) {
            HelpAction.RunDemo -> {
                CelestiaView.callOnRenderThread { core.charEnter(CelestiaAction.RunDemo.value) }
            }
            HelpAction.ShowDestinations -> {
                showDestinations()
            }
        }
    }

    override fun onHelpURLSelected(url: String) {
        openURL(url)
    }

    override fun addFavoriteItem(item: MutableFavoriteBaseItem) {
        val frag = supportFragmentManager.findFragmentById(R.id.normal_end_container)
        if (frag is FavoriteFragment && item is FavoriteBookmarkItem) {
            val bookmark = core.currentBookmark
            if (bookmark == null) {
                showAlert(CelestiaString("Cannot add object", ""))
                return
            }
            frag.add(FavoriteBookmarkItem(bookmark))
        }
    }

    private fun readFavorites() {
        var favorites = arrayListOf<BookmarkNode>()
        try {
            val myType = object : TypeToken<List<BookmarkNode>>() {}.type
            val str = FileUtils.readFileToText(favoriteJsonFilePath)
            val decoded = Gson().fromJson<ArrayList<BookmarkNode>>(str, myType)
            favorites = decoded
        } catch (ignored: Throwable) { }
        updateCurrentBookmarks(favorites)
    }

    override fun saveFavorites() {
        val favorites = getCurrentBookmarks()
        try {
            val myType = object : TypeToken<List<BookmarkNode>>() {}.type
            val str = Gson().toJson(favorites, myType)
            FileUtils.writeTextToFile(str, favoriteJsonFilePath)
        } catch (ignored: Throwable) { }
    }

    override fun onFavoriteItemSelected(item: FavoriteBaseItem) {
        if (item.isLeaf) {
            if (item is FavoriteScriptItem) {
                scriptOrURLPath = item.script.filename
                runScriptOrOpenURLIfNeeded()
            } else if (item is FavoriteBookmarkItem) {
                scriptOrURLPath = item.bookmark.url
                runScriptOrOpenURLIfNeeded()
            } else if (item is FavoriteDestinationItem) {
                val destination = item.destination
                val frag = supportFragmentManager.findFragmentById(R.id.normal_end_container)
                if (frag is FavoriteFragment) {
                    frag.pushFragment(DestinationDetailFragment.newInstance(destination))
                }
            }
        } else {
            val frag = supportFragmentManager.findFragmentById(R.id.normal_end_container)
            if (frag is FavoriteFragment) {
                frag.pushItem(item)
            }
        }
    }

    override fun onGoToDestination(destination: CelestiaDestination) {
        CelestiaView.callOnRenderThread { core.simulation.goTo(destination) }
    }

    override fun deleteFavoriteItem(index: Int) {
        val frag = supportFragmentManager.findFragmentById(R.id.normal_end_container)
        if (frag is FavoriteFragment) {
            frag.remove(index)
        }
    }

    override fun renameFavoriteItem(item: MutableFavoriteBaseItem) {
        showTextInput(CelestiaString("Rename", ""), item.title) { text ->
            val frag = supportFragmentManager.findFragmentById(R.id.normal_end_container)
            if (frag is FavoriteFragment) {
                frag.rename(item, text)
            }
        }
    }

    override fun onMainSettingItemSelected(item: SettingsItem) {
        val frag = supportFragmentManager.findFragmentById(R.id.normal_end_container)
        if (frag is SettingsFragment) {
            frag.pushMainSettingItem(item)
        }
    }

    override fun onMultiSelectionSettingItemChange(field: String, on: Boolean) {
        applyBooleanValue(on, field, true)
    }

    override fun onSingleSelectionSettingItemChange(field: String, value: Int) {
        applyIntValue(value, field, true)
    }

    override fun onCommonSettingSliderItemChange(field: String, value: Double) {
        applyDoubleValue(value, field, true)
    }

    override fun onCommonSettingActionItemSelected(action: Int) {
        CelestiaView.callOnRenderThread { core.charEnter(action) }
    }

    override fun onCommonSettingUnknownAction(id: String) {
        if (id == settingUnmarkAllID)
            core.simulation.universe.unmarkAll()
    }

    override fun onCommonSettingSwitchStateChanged(field: String, value: Boolean, volatile: Boolean) {
        applyBooleanValue(value, field, true, volatile)
    }

    private fun applyBooleanValue(value: Boolean, field: String, reloadSettings: Boolean = false, volatile: Boolean = false) {
        CelestiaView.callOnRenderThread {
            core.setBooleanValueForField(field, value)
            lifecycleScope.launch {
                if (!volatile)
                    settingManager[PreferenceManager.CustomKey(field)] = if (value) "1" else "0"
                if (reloadSettings)
                    reloadSettings()
            }
        }
    }

    private fun applyIntValue(value: Int, field: String, reloadSettings: Boolean = false, volatile: Boolean = false) {
        CelestiaView.callOnRenderThread {
            core.setIntValueForField(field, value)
            lifecycleScope.launch {
                if (!volatile)
                    settingManager[PreferenceManager.CustomKey(field)] = value.toString()
                if (reloadSettings)
                    reloadSettings()
            }
        }
    }

    private fun applyDoubleValue(value: Double, field: String, reloadSettings: Boolean = false, volatile: Boolean = false) {
        CelestiaView.callOnRenderThread {
            core.setDoubleValueForField(field, value)
            lifecycleScope.launch {
                if (!volatile)
                    settingManager[PreferenceManager.CustomKey(field)] = value.toString()
                if (reloadSettings)
                    reloadSettings()
            }
        }
    }

    override fun onCommonSettingPreferenceSwitchStateChanged(
        key: PreferenceManager.PredefinedKey,
        value: Boolean
    ) {
        preferenceManager[key] = if (value) "true" else "false"
    }

    override fun commonSettingPreferenceSwitchState(key: PreferenceManager.PredefinedKey): Boolean {
        return preferenceManager[key] == "true"
    }

    override fun commonSettingSliderValue(field: String): Double {
        return core.getDoubleValueForField(field)
    }

    override fun commonSettingSwitchState(field: String): Boolean {
        return core.getBooleanValueForPield(field)
    }

    override fun onCurrentTimeActionRequested(action: CurrentTimeAction) {
        when (action) {
            CurrentTimeAction.SetToCurrentTime -> {
                CelestiaView.callOnRenderThread { core.charEnter(CelestiaAction.CurrentTime.value) }
                reloadSettings()
            }
            CurrentTimeAction.PickDate -> {
                val format = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(),"yyyyMMddHHmmss")
                showDateInput(CelestiaString("Please enter the time in \"%s\" format.", "").format(format), format) { date ->
                    if (date == null) {
                        showAlert(CelestiaString("Unrecognized time string.", ""))
                        return@showDateInput
                    }
                    CelestiaView.callOnRenderThread {
                        core.simulation.time = date.julianDay
                        lifecycleScope.launch {
                            reloadSettings()
                        }
                    }
                }
            }
        }
    }

    override fun onAboutURLSelected(url: String) {
        openURL(url)
    }

    override fun onDataLocationNeedReset() {
        setConfigFilePath(null)
        setDataDirectoryPath(null)
        reloadSettings()
    }

    override fun onDataLocationRequested(dataType: DataType) {
        when (dataType) {
            DataType.Config -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.type = "*/*"
                intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
                startActivityForResult(intent, CONFIG_FILE_REQUEST)
            }
            DataType.DataDirectory -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
                startActivityForResult(intent, DATA_DIR_REQUEST)
            }
        }
    }

    override fun onSetOverrideLanguage(language: String?) {
        preferenceManager[PreferenceManager.PredefinedKey.Language] = language
        reloadSettings()
    }

    override fun currentLanguage(): String {
        val lang = CelestiaAppCore.getLocalizedString("LANGUAGE", "celestia")
        if (lang == "LANGUAGE")
            return "en"
        return lang
    }

    override fun currentOverrideLanguage(): String? {
        return preferenceManager[PreferenceManager.PredefinedKey.Language]
    }

    override fun availableLanguages(): List<String> {
        return availableLanguageCodes
    }

    override fun onSearchForEvent(objectName: String, startDate: Date, endDate: Date) {
        val body = core.simulation.findObject(objectName).`object` as? CelestiaBody
        if (body == null) {
            showAlert(CelestiaString("Object not found", ""))
            return
        }
        val finder = CelestiaEclipseFinder(body)
        val alert = showLoading(CelestiaString("Calculating…", "")) {
            finder.abort()
        }
        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) {
                finder.search(
                    startDate.julianDay,
                    endDate.julianDay,
                    CelestiaEclipseFinder.ECLIPSE_KIND_LUNAR or CelestiaEclipseFinder.ECLIPSE_KIND_SOLAR
                )
            }
            EventFinderResultFragment.eclipses = results
            if (alert.isShowing) alert.dismiss()
            val frag = supportFragmentManager.findFragmentById(R.id.normal_end_container)
            if (frag is EventFinderContainerFragment) {
                frag.showResult()
            }
        }
    }

    override fun onEclipseChosen(eclipse: CelestiaEclipseFinder.Eclipse) {
        CelestiaView.callOnRenderThread {
            core.simulation.goToEclipse(eclipse)
        }
    }

    private fun setDataDirectoryPath(path: String?) {
        preferenceManager[PreferenceManager.PredefinedKey.DataDirPath] = path
        customDataDirPath = path
    }

    private fun setConfigFilePath(path: String?) {
        preferenceManager[PreferenceManager.PredefinedKey.ConfigFilePath] = path
        customConfigFilePath = path
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data ?: return
        if (requestCode == CONFIG_FILE_REQUEST) {
            val path = RealPathUtils.getRealPath(this, uri)
            if (path == null) {
                showWrongPathProvided()
            } else {
                setConfigFilePath(path)
                reloadSettings()
            }
        } else if (requestCode == DATA_DIR_REQUEST) {
            val path = RealPathUtils.getRealPath(this, DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri)))
            if (path == null) {
                showWrongPathProvided()
            } else {
                setDataDirectoryPath(path)
                reloadSettings()
            }
        }
    }

    private fun showWrongPathProvided() {
        // TODO: Localization
        showAlert(CelestiaString("Unable to resolve path, please ensure that you have selected a path inside %s.", "").format(getExternalFilesDir(null)?.absolutePath ?: ""))
    }

    override fun celestiaFragmentDidRequestActionMenu() {
        showToolbar()
    }

    override fun celestiaFragmentDidRequestObjectInfo() {
        val selection = core.simulation.selection
        if (selection.isEmpty) { return }

        showInfo(selection)
    }

    private fun openURL(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    private fun reloadSettings() {
        val frag = supportFragmentManager.findFragmentById(R.id.normal_end_container)
        if (frag is SettingsFragment) {
            frag.reload()
        }
    }

    private fun hideOverlay(animated: Boolean = false, callback: (() -> Unit)? = null) {
        val ltr = resources.configuration.layoutDirection != View.LAYOUT_DIRECTION_RTL
        hideFragment(animated, R.id.normal_end_container, if (ltr) R.anim.exit_to_right else R.anim.exit_to_left) {
            hideFragment(animated, R.id.toolbar_end_container, if (ltr) R.anim.exit_to_right else R.anim.exit_to_left) {
                findViewById<View>(R.id.overlay_container).visibility = View.INVISIBLE
                findViewById<View>(R.id.end_notch).visibility = View.INVISIBLE
                hideFragment(animated, R.id.toolbar_bottom_container, R.anim.exit_to_bottom) {
                    findViewById<View>(R.id.bottom_container).visibility = View.INVISIBLE
                    if (callback != null)
                        callback()
                }
            }
        }
    }

    private fun hideFragment(animated: Boolean, containerID: Int, animationID: Int, completion: () -> Unit = {}) {
        val frag = supportFragmentManager.findFragmentById(containerID)
        val view = findViewById<View>(containerID)
        if (view == null || frag == null) {
            if (frag != null) {
                if (frag is Cleanable) frag.cleanUp()
                supportFragmentManager.beginTransaction().hide(frag).remove(frag).commitAllowingStateLoss()
            }

            if (view != null)
                view.visibility = View.INVISIBLE

            completion()
            return@hideFragment
        }

        val fragView = frag.view
        val executionBlock = {
            if (frag is Cleanable)
                frag.cleanUp()
            supportFragmentManager.beginTransaction().hide(frag).remove(frag).commitAllowingStateLoss()
            view.visibility = View.INVISIBLE
            completion()
        }

        if (fragView == null || !animated) {
            executionBlock()
        } else {
            val animation = AnimationUtils.loadAnimation(this, animationID)
            animation.setAnimationListener(object: Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationStart(animation: Animation?) {
                    interactionBlocked = true
                }

                override fun onAnimationEnd(animation: Animation?) {
                    interactionBlocked = false
                    executionBlock()
                }
            })
            fragView.startAnimation(animation)
        }
    }

    private fun showInfo(selection: CelestiaSelection) {
        CelestiaView.callOnRenderThread {
            // Fetch info at the Celestia thread to avoid race condition
            val overview = core.getOverviewForSelection(selection)
            val name = core.simulation.universe.getNameForSelection(selection)
            val hasWebInfo = selection.webInfoURL != null
            val hasAltSurface = (selection.body?.alternateSurfaceNames?.size ?: 0) > 0
            lifecycleScope.launch {
                currentSelection = selection
                showEndFragment(InfoFragment.newInstance(
                    InfoDescriptionItem(name, overview, hasWebInfo, hasAltSurface)
                ))
            }
        }
    }

    private fun showSearch() {
        showEndFragment(SearchFragment.newInstance())
    }

    private fun showBrowser() {
        showEndFragment(BrowserFragment.newInstance())
    }

    private fun showTimeControl() {
        showBottomFragment(
            BottomControlFragment.newInstance(
                listOf(
                    CelestiaAction.Slower,
                    CelestiaAction.PlayPause,
                    CelestiaAction.Faster,
                    CelestiaAction.Reverse
                )))
    }

    private fun showScriptControl() {
        showBottomFragment(
            BottomControlFragment.newInstance(
                listOf(
                    CelestiaAction.PlayPause,
                    CelestiaAction.CancelScript
                )))
    }

    private fun showCameraControl() {
        showEndFragment(CameraControlContainerFragment.newInstance())
    }

    private fun showHelp() {
        showEndFragment(HelpFragment.newInstance())
    }

    private fun showFavorite() {
        readFavorites()
        val scripts = CelestiaScript.getScriptsInDirectory("scripts", true)
        extraScriptPath?.let { path ->
            scripts.addAll(CelestiaScript.getScriptsInDirectory(path, true))
        }
        updateCurrentScripts(scripts)
        updateCurrentDestinations(core.destinations)
        showEndFragment(FavoriteFragment.newInstance(FavoriteRoot()))
    }

    private fun showDestinations() {
        updateCurrentDestinations(core.destinations)
        showEndFragment(FavoriteFragment.newInstance(FavoriteTypeItem(FavoriteType.Destination)))
    }

    private fun showSettings() {
        showEndFragment(SettingsFragment.newInstance())
    }

    private fun showShare() {
        val orig = core.currentURL
        val bytes = orig.toByteArray()
        val url = android.util.Base64.encodeToString(bytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
        val sel = core.simulation.selection
        val name = core.simulation.universe.getNameForSelection(sel)

        showTextInput(CelestiaString("Share", ""), name) { title ->
            Toast.makeText(this, CelestiaString("Generating sharing link…", ""), Toast.LENGTH_SHORT).show()
            val service = ShareAPI.shared.create(ShareAPIService::class.java)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val result = service.create(title, url, versionCode.toString()).commonHandler(URLCreationResponse::class.java)
                    withContext(Dispatchers.Main) {
                        ShareCompat.IntentBuilder
                            .from(this@MainActivity)
                            .setType("text/plain")
                            .setChooserTitle(name)
                            .setText(result.publicURL)
                            .startChooser()
                    }
                } catch (ignored: Throwable) {
                    withContext(Dispatchers.Main) {
                        showShareError()
                    }
                }
            }
        }
    }

    private fun showEventFinder() {
        showEndFragment(EventFinderContainerFragment.newInstance())
    }

    private fun showShareError() {
        showAlert(CelestiaString("Cannot share URL", ""))
    }

    // Resource
    private fun showOnlineResource() {
        showEndFragment(ResourceFragment.newInstance())
    }

    override fun onAsyncListItemSelected(item: Any) {
        val frag = supportFragmentManager.findFragmentById(R.id.normal_end_container)
        if (frag is ResourceFragment) {
            if (item is ResourceCategory) {
                frag.pushItem(item)
            } else if (item is ResourceItem) {
                frag.pushItem(item)
            }
        }
    }

    // Utilities
    private fun showEndFragment(fragment: Fragment, containerID: Int = R.id.normal_end_container) {
        val ref = WeakReference(fragment)
        hideOverlay(true) {
            ref.get()?.let {
                showEndFragmentDirect(it, containerID)
            }
        }
    }

    private fun showEndFragmentDirect(fragment: Fragment, containerID: Int = R.id.normal_end_container) {
        findViewById<View>(R.id.overlay_container).visibility = View.VISIBLE
        findViewById<View>(containerID).visibility = View.VISIBLE
        findViewById<View>(R.id.end_notch).visibility = View.VISIBLE

        val ltr = resources.configuration.layoutDirection != View.LAYOUT_DIRECTION_RTL

        val ani1 = if (ltr) R.anim.enter_from_right else R.anim.enter_from_left
        val ani2 = if (ltr) R.anim.exit_to_left else R.anim.exit_to_right
        val ani3 = if (ltr) R.anim.enter_from_left else R.anim.enter_from_right
        val ani4 = if (ltr) R.anim.exit_to_right else R.anim.exit_to_left

        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(ani1, ani2, ani3, ani4)
            .add(containerID, fragment)
            .commitAllowingStateLoss()
    }

    private fun showBottomFragment(fragment: Fragment) {
        val ref = WeakReference(fragment)
        hideOverlay(true) {
            ref.get()?.let {
                showBottomFragmentDirect(it)
            }
        }
    }

    private fun showBottomFragmentDirect(fragment: Fragment) {
        findViewById<View>(R.id.bottom_container).visibility = View.VISIBLE
        findViewById<View>(R.id.toolbar_bottom_container).visibility = View.VISIBLE
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.enter_from_bottom, R.anim.exit_to_top, R.anim.enter_from_top, R.anim.exit_to_bottom)
            .add(R.id.toolbar_bottom_container, fragment)
            .commitAllowingStateLoss()
    }

    private fun addToBackStack() {
        val frag = supportFragmentManager.findFragmentById(R.id.normal_end_container) ?: return
        backStack.add(frag)
    }

    private fun clearBackStack() {
        backStack.clear()
    }

    private fun popLastFromBackStackAndShow() {
        if (backStack.size == 0) {
            hideOverlay(true)
            return
        }
        val frag = backStack.last()
        backStack.removeAt(backStack.size - 1)
        showEndFragment(frag)
    }

    companion object {
        private const val CURRENT_DATA_VERSION = "17"
        // 17: 1.2 Beta
        // 16: 1.1 Migrate from filesDir to noBackupFilesDir
        // 15: 1.0.5
        // 14: 1.0.4
        // 13: 1.0.3
        // 12: 1.0.2
        // 11: 1.0.1
        // 10: 1.0
        // < 10: 1.0 Beta X

        private const val CELESTIA_DATA_FOLDER_NAME = "CelestiaResources"
        private const val CELESTIA_FONT_FOLDER_NAME = "fonts"
        private const val CELESTIA_CFG_NAME = "celestia.cfg"
        private const val CELESTIA_EXTRA_FOLDER_NAME = "CelestiaResources/extras"
        private const val CELESTIA_SCRIPT_FOLDER_NAME = "CelestiaResources/scripts"

        private const val DATA_DIR_REQUEST = 1
        private const val CONFIG_FILE_REQUEST = 2

        private const val TAG = "MainActivity"

        var customDataDirPath: String? = null
        var customConfigFilePath: String? = null
        private var addonPath: String? = null
        private var extraScriptPath: String? = null
        private var languageOverride: String? = null
        private var enableMultisample = false
        private var enableHiDPI = false

        var availableInstalledFonts: Map<String, Pair<FontHelper.FontCompat, FontHelper.FontCompat>> = mapOf()
        var defaultInstalledFont: Pair<FontHelper.FontCompat, FontHelper.FontCompat>? = null

        private var availableLanguageCodes: List<String> = listOf()

        init {
            System.loadLibrary("nativecrashhandler")
        }
    }
}
