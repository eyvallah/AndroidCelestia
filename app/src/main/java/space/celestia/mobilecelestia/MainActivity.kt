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

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.LayoutDirection
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Guideline
import androidx.core.animation.addListener
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.view.*
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
import space.celestia.mobilecelestia.common.SheetLayout
import space.celestia.mobilecelestia.common.EdgeInsets
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
import space.celestia.mobilecelestia.goto.GoToContainerFragment
import space.celestia.mobilecelestia.goto.GoToInputFragment
import space.celestia.mobilecelestia.help.HelpAction
import space.celestia.mobilecelestia.help.HelpFragment
import space.celestia.mobilecelestia.info.InfoFragment
import space.celestia.mobilecelestia.info.model.*
import space.celestia.mobilecelestia.loading.LoadingFragment
import space.celestia.mobilecelestia.resource.AsyncListFragment
import space.celestia.mobilecelestia.resource.DestinationDetailFragment
import space.celestia.mobilecelestia.resource.ResourceFragment
import space.celestia.mobilecelestia.resource.ResourceItemFragment
import space.celestia.mobilecelestia.resource.model.ResourceCategory
import space.celestia.mobilecelestia.resource.model.ResourceItem
import space.celestia.mobilecelestia.resource.model.ResourceManager
import space.celestia.mobilecelestia.search.SearchContainerFragment
import space.celestia.mobilecelestia.search.SearchFragment
import space.celestia.mobilecelestia.search.hideKeyboard
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
import java.net.URL
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
    DestinationDetailFragment.Listener,
    GoToInputFragment.Listener,
    ResourceItemFragment.Listener {

    private val preferenceManager by lazy { PreferenceManager(this, "celestia") }
    private val settingManager by lazy { PreferenceManager(this, "celestia_setting") }
    private val legacyCelestiaParentPath by lazy { this.filesDir.absolutePath }
    private val celestiaParentPath by lazy { this.noBackupFilesDir.absolutePath }
    private val favoriteJsonFilePath by lazy { "${filesDir.absolutePath}/favorites.json" }

    private val core by lazy { CelestiaAppCore.shared() }

    private var interactionBlocked = false

    private var readyForInteraction = false
    private var scriptOrURLPath: String? = null

    private val defaultConfigFilePath by lazy { "$defaultDataDirectoryPath/$CELESTIA_CFG_NAME" }
    private val defaultDataDirectoryPath by lazy { "$celestiaParentPath/$CELESTIA_DATA_FOLDER_NAME" }

    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    private lateinit var directoryChooserLauncher: ActivityResultLauncher<Intent>

    private val celestiaConfigFilePath: String
        get() {
            val custom = customConfigFilePath
            if (custom != null)
                return custom
            return defaultConfigFilePath
        }

    private val celestiaDataDirPath: String
        get() {
            val custom = customDataDirPath
            if (custom != null)
                return custom
            return defaultDataDirectoryPath
        }

    private val fontDirPath: String
        get() = "$celestiaParentPath/$CELESTIA_FONT_FOLDER_NAME"

    private var currentGoToData: GoToInputFragment.GoToData? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        val reporter = AppStatusReporter.shared()
        val currentState = reporter.state

        val savedState = if (currentState == AppStatusReporter.State.NONE) null else savedInstanceState

        super.onCreate(savedState)

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

        reporter.register(this)

        // Handle notch
        ViewCompat.setOnApplyWindowInsetsListener( findViewById<View>(android.R.id.content).rootView, { _, insets ->
            updateConfiguration(resources.configuration, insets?.displayCutout)
            return@setOnApplyWindowInsetsListener insets
        })

        findViewById<View>(R.id.menu_overlay).setOnTouchListener { _, e ->
            if (e.actionMasked == MotionEvent.ACTION_UP) {
                hideOverlay(true)
            }
            return@setOnTouchListener true
        }

        findViewById<View>(R.id.close_button).setOnClickListener {
            hideOverlay(true)
        }

        findViewById<View>(R.id.interaction_filter).setOnTouchListener { _, _ ->
            if (interactionBlocked) { return@setOnTouchListener true }
            // Pass through
            return@setOnTouchListener false
        }

        if (currentState == AppStatusReporter.State.LOADING_FAILURE || currentState == AppStatusReporter.State.EXTERNAL_LOADING_FAILURE) {
            celestiaLoadingFailed()
            return
        }

        if (currentState == AppStatusReporter.State.NONE || currentState == AppStatusReporter.State.EXTERNAL_LOADING)  {
            loadExternalConfig(savedState)
        } else if (currentState == AppStatusReporter.State.LOADING) {
            // Do nothing
        } else if (currentState == AppStatusReporter.State.LOADING_SUCCESS) {
            celestiaLoadingSucceeded()
        } else if (currentState == AppStatusReporter.State.FINISHED) {
            celestiaLoadingFinished()
        }

        if (savedState != null) {
            val toolbarVisible = savedState.getBoolean(TOOLBAR_VISIBLE_TAG, false)
            val menuVisible = savedState.getBoolean(MENU_VISIBLE_TAG, false)
            val bottomSheetVisible = savedState.getBoolean(BOTTOM_SHEET_VISIBLE_TAG, false)
            currentGoToData = savedState.getSerializable(GO_TO_DATA_TAG) as? GoToInputFragment.GoToData

            findViewById<View>(R.id.toolbar_overlay).visibility = if (toolbarVisible) View.VISIBLE else View.GONE
            findViewById<View>(R.id.toolbar_container).visibility = if (toolbarVisible) View.VISIBLE else View.GONE

            findViewById<View>(R.id.menu_overlay).visibility = if (menuVisible) View.VISIBLE else View.GONE
            findViewById<View>(R.id.menu_container).visibility = if (menuVisible) View.VISIBLE else View.GONE

            findViewById<View>(R.id.bottom_sheet_overlay).visibility = if (bottomSheetVisible) View.VISIBLE else View.GONE
            findViewById<View>(R.id.bottom_sheet_card).visibility = if (bottomSheetVisible) View.VISIBLE else View.GONE
        }

        fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val uri = it.data?.data ?: return@registerForActivityResult
            val path = RealPathUtils.getRealPath(this, uri)
            if (path == null) {
                showWrongPathProvided()
            } else {
                setConfigFilePath(path)
                reloadSettings()
            }
        }

        directoryChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val uri = it.data?.data ?: return@registerForActivityResult
            val path = RealPathUtils.getRealPath(this, DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri)))
            if (path == null) {
                showWrongPathProvided()
            } else {
                setDataDirectoryPath(path)
                reloadSettings()
            }
        }

        val rootView = findViewById<View>(android.R.id.content).rootView
        updateConfiguration(resources.configuration, ViewCompat.getRootWindowInsets(rootView)?.displayCutout)

        handleIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(MENU_VISIBLE_TAG, findViewById<View>(R.id.menu_container).visibility == View.VISIBLE)
        outState.putBoolean(BOTTOM_SHEET_VISIBLE_TAG, findViewById<View>(R.id.bottom_sheet_card).visibility == View.VISIBLE)
        outState.putBoolean(TOOLBAR_VISIBLE_TAG, findViewById<View>(R.id.toolbar_container).visibility == View.VISIBLE)
        outState.putSerializable(GO_TO_DATA_TAG, currentGoToData)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        AppStatusReporter.shared().unregister(this)

        Log.d(TAG, "Destroying MainActivity")

        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val rootView = findViewById<View>(android.R.id.content).rootView
        updateConfiguration(newConfig, ViewCompat.getRootWindowInsets(rootView)?.displayCutout)
    }

    override fun onBackPressed() {
        var frag = supportFragmentManager.findFragmentById(R.id.bottom_sheet_card)
        if (frag is Poppable && frag.canPop()) {
            frag.popLast()
        } else {
            hideOverlay(true)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }


    private fun updateConfiguration(configuration: Configuration, displayCutout: DisplayCutoutCompat? = null) {
        val density = resources.displayMetrics.density
        var safeInsetEnd = 0
        var safeInsetStart = 0
        var safeInsetBottom = 0
        var safeInsetTop = 0
        val isRTL = configuration.layoutDirection == LayoutDirection.RTL

        if (displayCutout != null) {
            safeInsetStart = if (isRTL) displayCutout.safeInsetRight else displayCutout.safeInsetLeft
            safeInsetEnd = if (isRTL) displayCutout.safeInsetLeft else displayCutout.safeInsetRight
            safeInsetBottom = displayCutout.safeInsetBottom
            safeInsetTop = displayCutout.safeInsetTop
        }

        val toolbarContainer = findViewById<View>(R.id.toolbar_overlay)
        val menuWidthGuideLine = findViewById<Guideline>(R.id.menu_width_guideline)

        menuWidthGuideLine.setGuidelineEnd((220 * density).toInt() + safeInsetEnd)

        val toolbarParams = toolbarContainer.layoutParams as FrameLayout.LayoutParams
        toolbarParams.leftMargin = if (isRTL) safeInsetEnd else safeInsetStart
        toolbarParams.rightMargin = if (isRTL) safeInsetStart else safeInsetEnd
        toolbarParams.topMargin = safeInsetTop
        toolbarParams.bottomMargin = safeInsetBottom

        val bottomSheetContainer = findViewById<SheetLayout>(R.id.bottom_sheet_overlay)
        bottomSheetContainer.edgeInsets = EdgeInsets(displayCutout)
        bottomSheetContainer.postInvalidate()
    }

    private fun loadExternalConfig(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.loading_fragment_container, LoadingFragment.newInstance())
                .commitAllowingStateLoss()
        }
        AppStatusReporter.shared().updateState(AppStatusReporter.State.EXTERNAL_LOADING)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                copyAssetIfNeeded()
                if (!isActive) return@launch
                createAddonFolder()
                if (!isActive) return@launch
                loadConfig()
                if (!isActive) return@launch
                withContext(Dispatchers.Main) {
                    loadConfigSuccess()
                }
            } catch (error: Throwable) {
                withContext(Dispatchers.Main) {
                    AppStatusReporter.shared().updateState(AppStatusReporter.State.EXTERNAL_LOADING_FAILURE)
                    loadConfigFailed(error)
                }
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, findViewById(R.id.main_container)).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun celestiaLoadingProgress(status: String) {}

    override fun celestiaLoadingStateChanged(newState: AppStatusReporter.State) {
        if (newState == AppStatusReporter.State.FINISHED) {
            celestiaLoadingFinished()
        } else if (newState == AppStatusReporter.State.LOADING_SUCCESS) {
            celestiaLoadingSucceeded()
        } else if (newState == AppStatusReporter.State.EXTERNAL_LOADING_FAILURE || newState == AppStatusReporter.State.LOADING_FAILURE) {
            celestiaLoadingFailed()
        }
    }

    fun celestiaLoadingSucceeded() {
        CelestiaView.callOnRenderThread {
            readSettings()

            AppStatusReporter.shared().updateState(AppStatusReporter.State.FINISHED)
        }
    }

    fun celestiaLoadingFinished() {
        lifecycleScope.launch {
            supportFragmentManager.findFragmentById(R.id.loading_fragment_container)?.let {
                supportFragmentManager.beginTransaction().hide(it).remove(it).commitAllowingStateLoss()
            }
            findViewById<View>(R.id.loading_fragment_container).visibility = View.GONE

            ResourceManager.shared.addonDirectory = addonPath

            readyForInteraction = true
            runScriptOrOpenURLIfNeeded()

            // Show onboarding if needed
            showWelcomeIfNeeded()
        }
    }

    fun celestiaLoadingFailed() {
        AppStatusReporter.shared().updateStatus(CelestiaString("Loading Celestia failed…", ""))
        lifecycleScope.launch {
            removeCelestiaFragment()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val rootView = findViewById<View>(android.R.id.content).rootView
        updateConfiguration(resources.configuration, ViewCompat.getRootWindowInsets(rootView)?.displayCutout)
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

        var languageFromSettings = preferenceManager[PreferenceManager.PredefinedKey.Language]
        if (languageFromSettings == null) {
            val locale = Locale.getDefault()
            var lang = locale.language
            var country = locale.country
            if (lang == "zh") {
                // Special handling for Chinese script
                // Basically mapping zh_CN => zh_Hans
                //                   zh_TW => zh_Hant
                if (locale.script.contains("Hans"))
                    country = "CN"
                else if (locale.script.contains("Hant"))
                    country = "TW"
                // Is it possible for script to be empty?
                // Singapore uses Hans, Hong Kong, Macao uses Hant
                else if (country == "SG")
                    country = "CN"
                else if (country == "HK" || country == "MO")
                    country = "TW"
            }
            var localeString = "${lang}_${country}"
            if (availableLanguageCodes.contains(localeString)) {
                language = localeString
            } else {
                localeString = lang
                if (availableLanguageCodes.contains(localeString)) {
                    language = localeString
                } else {
                    language = "en"
                }
            }
        } else {
            languageOverride = languageFromSettings
            language = languageFromSettings
        }

        enableMultisample = preferenceManager[PreferenceManager.PredefinedKey.MSAA] == "true"
        enableHiDPI = preferenceManager[PreferenceManager.PredefinedKey.FullDPI] != "false" // default on
    }

    private fun showWelcomeIfNeeded() {
        if (preferenceManager[PreferenceManager.PredefinedKey.OnboardMessage] != "true") {
            preferenceManager[PreferenceManager.PredefinedKey.OnboardMessage] = "true"
            showHelp()
        }
    }

    override public fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        runOnUiThread {
            handleIntent(intent)
        }
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
            language
        )
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
        showMenuFragment(ToolbarFragment.newInstance(listOf()))
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
                hideOverlay {
                    showShare()
                }
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
            ToolbarAction.Paperplane -> {
                showGoTo()
            }
        }
    }

    // Listeners...
    override fun onInfoActionSelected(action: InfoActionItem, selection: CelestiaSelection) {
        when (action) {
            is InfoNormalActionItem -> {
                core.simulation.selection = selection
                CelestiaView.callOnRenderThread { core.charEnter(action.item.value) }
            }
            is InfoSelectActionItem -> {
                core.simulation.selection = selection
            }
            is InfoWebActionItem -> {
                val url = selection.webInfoURL!!
                // show web info in browser
                openURL(url)
            }
            is SubsystemActionItem -> {
                val entry = selection.`object` ?: return
                val browserItem = CelestiaBrowserItem(core.simulation.universe.getNameForSelection(selection), null, entry, core.simulation.universe)
                showBottomSheetFragment(SubsystemBrowserFragment.newInstance(browserItem))
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

    override fun onInfoLinkMetaDataClicked(url: URL) {
        openURL(url.toString())
    }

    override fun onSearchItemSelected(text: String) {
        hideKeyboard()

        val savedGoToData = currentGoToData
        if (savedGoToData != null) {
            savedGoToData.objectName = text
            showGoTo(savedGoToData)
            return
        }

        val sel = core.simulation.findObject(text)
        if (sel.isEmpty) {
            showAlert(CelestiaString("Object not found", ""))
            return
        }

        val frag = supportFragmentManager.findFragmentById(R.id.bottom_sheet)
        if (frag is SearchContainerFragment) {
            frag.pushSearchResult(sel)
        }
    }

    override fun onSearchItemSubmit(text: String) {
        onSearchItemSelected(text)
    }

    override fun onActionSelected(item: CelestiaAction) {
        CelestiaView.callOnRenderThread { core.charEnter(item.value) }
    }

    override fun objectExistsWithName(name: String): Boolean {
        return !core.simulation.findObject(name).isEmpty
    }

    override fun onGoToObject(name: String) {
        val sel = core.simulation.findObject(name)
        if (sel.isEmpty) {
            showAlert(CelestiaString("Object not found", ""))
            return
        }
        CelestiaView.callOnRenderThread {
            core.simulation.selection = sel
            core.charEnter(CelestiaAction.GoTo.value)
        }
    }

    override fun onBottomControlHide() {
        hideOverlay(true)
    }

    override fun onBrowserItemSelected(item: BrowserItem) {
        val frag = supportFragmentManager.findFragmentById(R.id.bottom_sheet) as? BrowserRootFragment ?: return
        if (!item.isLeaf) {
            frag.pushItem(item.item)
        } else {
            val obj = item.item.`object`
            if (obj != null) {
                val selection = CelestiaSelection.create(obj)
                if (selection != null) {
                    frag.showInfo(selection)
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
        val frag = supportFragmentManager.findFragmentById(R.id.bottom_sheet)
        if (frag is FavoriteFragment && item is FavoriteBookmarkItem) {
            val bookmark = core.currentBookmark
            if (bookmark == null) {
                showAlert(CelestiaString("Cannot add object", ""))
                return
            }
            frag.add(FavoriteBookmarkItem(bookmark))
        }
    }

    override fun shareFavoriteItem(item: MutableFavoriteBaseItem) {
        if (item is FavoriteBookmarkItem && item.bookmark.isLeaf) {
            shareURL(item.bookmark.url, item.bookmark.name)
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
                val frag = supportFragmentManager.findFragmentById(R.id.bottom_sheet)
                if (frag is FavoriteFragment) {
                    frag.pushFragment(DestinationDetailFragment.newInstance(destination))
                }
            }
        } else {
            val frag = supportFragmentManager.findFragmentById(R.id.bottom_sheet)
            if (frag is FavoriteFragment) {
                frag.pushItem(item)
            }
        }
    }

    override fun onEditGoToObject(goToData: GoToInputFragment.GoToData) {
        currentGoToData = goToData
        showBottomSheetFragment(SearchContainerFragment.newInstance())
    }

    override fun onGoToDestination(destination: CelestiaDestination) {
        CelestiaView.callOnRenderThread { core.simulation.goTo(destination) }
    }

    override fun deleteFavoriteItem(index: Int) {
        val frag = supportFragmentManager.findFragmentById(R.id.bottom_sheet)
        if (frag is FavoriteFragment) {
            frag.remove(index)
        }
    }

    override fun renameFavoriteItem(item: MutableFavoriteBaseItem) {
        showTextInput(CelestiaString("Rename", ""), item.title) { text ->
            val frag = supportFragmentManager.findFragmentById(R.id.bottom_sheet)
            if (frag is FavoriteFragment) {
                frag.rename(item, text)
            }
        }
    }

    override fun onMainSettingItemSelected(item: SettingsItem) {
        val frag = supportFragmentManager.findFragmentById(R.id.bottom_sheet)
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

    override fun commonSettingPreferenceSwitchState(key: PreferenceManager.PredefinedKey): Boolean? {
        return when (preferenceManager[key]) {
            "true" -> true
            "false" -> false
            else -> null
        }
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
                if (intent.resolveActivity(packageManager) != null) {
                    fileChooserLauncher.launch(intent)
                } else
                    showUnsupportedAction()
            }
            DataType.DataDirectory -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
                if (intent.resolveActivity(packageManager) != null) {
                    directoryChooserLauncher.launch(intent)
                } else
                    showUnsupportedAction()
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
            val frag = supportFragmentManager.findFragmentById(R.id.bottom_sheet)
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

    private fun showWrongPathProvided() {
        showAlert(CelestiaString("Unable to resolve path, please ensure that you have selected a path inside %s.", "").format(getExternalFilesDir(null)?.absolutePath ?: ""))
    }

    private fun showUnsupportedAction() {
        showAlert(CelestiaString("Unsupported action.", ""))
    }

    override fun celestiaFragmentDidRequestActionMenu() {
        showToolbar()
    }

    override fun celestiaFragmentDidRequestObjectInfo() {
        val selection = core.simulation.selection
        if (selection.isEmpty) { return }

        showInfo(selection)
    }

    override fun provideFallbackConfigFilePath(): String {
        return defaultConfigFilePath
    }

    override fun provideFallbackDataDirectoryPath(): String {
        return defaultDataDirectoryPath
    }

    override fun celestiaFragmentLoadingFromFallback() {
        lifecycleScope.launch {
            showAlert(CelestiaString("Error loading data, fallback to original configuration.", ""))
        }
    }

    private fun openURL(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(packageManager) != null)
            startActivity(intent)
        else
            showUnsupportedAction()
    }

    private fun reloadSettings() {
        val frag = supportFragmentManager.findFragmentById(R.id.bottom_sheet)
        if (frag is SettingsFragment) {
            frag.reload()
        }
    }

    private fun hideOverlay(animated: Boolean = false, completion: (() -> Unit)? = null) {
        hideMenu(animated) {
            hideToolbar(animated) {
                hideBottomSheet(animated) {
                    if (completion != null)
                        completion()
                }
            }
        }
    }

    private fun hideMenu(animated: Boolean, completion: (() -> Unit)? = null) {
        hideView(animated, R.id.menu_container, true) {
            findViewById<View>(R.id.menu_overlay).visibility = View.INVISIBLE
            val fragment = supportFragmentManager.findFragmentById(R.id.menu_container)
            if (fragment != null)
                supportFragmentManager.beginTransaction().hide(fragment).remove(fragment).commitAllowingStateLoss()
            if (completion != null)
                completion()
        }
    }

    private fun hideToolbar(animated: Boolean, completion: (() -> Unit)? = null) {
        hideView(animated, R.id.toolbar_container, false) {
            findViewById<View>(R.id.toolbar_overlay).visibility = View.INVISIBLE
            val fragment = supportFragmentManager.findFragmentById(R.id.toolbar_container)
            if (fragment != null)
                supportFragmentManager.beginTransaction().hide(fragment).remove(fragment).commitAllowingStateLoss()
            if (completion != null)
                completion()
        }
    }

    private fun hideBottomSheet(animated: Boolean, completion: (() -> Unit)? = null) {
        hideView(animated, R.id.bottom_sheet_card, false) {
            findViewById<View>(R.id.bottom_sheet_overlay).visibility = View.INVISIBLE
            val fragment = supportFragmentManager.findFragmentById(R.id.bottom_sheet)
            if (fragment != null)
                supportFragmentManager.beginTransaction().hide(fragment).remove(fragment).commitAllowingStateLoss()
            if (completion != null)
                completion()
        }
    }

    private fun showView(animated: Boolean, viewID: Int, horizontal: Boolean, completion: () -> Unit = {}) {
        val view = findViewById<View>(viewID)

        val parent = view.parent as? View
        if (parent == null) return

        view.visibility = View.VISIBLE

        var destination: Float
        if (horizontal) {
            val ltr = resources.configuration.layoutDirection != View.LAYOUT_DIRECTION_RTL
            destination = (if (ltr) view.width else -view.width).toFloat()
        } else {
            destination = view.height.toFloat()
        }

        val executionBlock = {
            completion()
        }

        if (!animated) {
            executionBlock()
        } else {
            val showAnimator = ObjectAnimator.ofFloat(view, if (horizontal) "translationX" else "translationY", destination, 0f)

            showAnimator.duration = 200
            showAnimator.addListener(onStart = {
                interactionBlocked = true
            }, onEnd = {
                interactionBlocked = false
                executionBlock()
            }, onCancel = {
                interactionBlocked = false
                executionBlock()
            })
            showAnimator.start()
        }
    }

    private fun hideView(animated: Boolean, viewID: Int, horizontal: Boolean, completion: () -> Unit = {}) {
        val view = findViewById<View>(viewID)

        val parent = view.parent as? View
        if (parent == null) return

        var destination: Float
        if (horizontal) {
            val ltr = resources.configuration.layoutDirection != View.LAYOUT_DIRECTION_RTL
            destination = (if (ltr) (parent.width - view.left) else -(view.right)).toFloat()
        } else {
            destination = (parent.height - view.top).toFloat()
        }

        val executionBlock = {
            view.visibility = View.INVISIBLE
            completion()
        }

        if (view.visibility != View.VISIBLE || !animated) {
            executionBlock()
        } else {
            val hideAnimator = ObjectAnimator.ofFloat(view, if (horizontal) "translationX" else "translationY", 0f, destination)
            hideAnimator.duration = 200
            hideAnimator.addListener(onStart = {
                interactionBlocked = true
            }, onEnd = {
                interactionBlocked = false
                executionBlock()
            }, onCancel = {
                interactionBlocked = false
                executionBlock()
            })
            hideAnimator.start()
        }
    }

    private fun createInfo(selection: CelestiaSelection, callback: (InfoDescriptionItem) -> Unit) {
        CelestiaView.callOnRenderThread {
            // Fetch info at the Celestia thread to avoid race condition
            val overview = core.getOverviewForSelection(selection)
            val name = core.simulation.universe.getNameForSelection(selection)
            val hasWebInfo = selection.webInfoURL != null
            val hasAltSurface = (selection.body?.alternateSurfaceNames?.size ?: 0) > 0
            lifecycleScope.launch {
                callback(InfoDescriptionItem(name, overview, hasWebInfo, hasAltSurface))
            }
        }
    }

    private fun showInfo(selection: CelestiaSelection) {
        showBottomSheetFragment(InfoFragment.newInstance(selection))
    }

    private fun showSearch() {
        currentGoToData = null
        showBottomSheetFragment(SearchContainerFragment.newInstance())
    }

    private fun showBrowser() {
        showBottomSheetFragment(BrowserFragment.newInstance())
    }

    private fun showTimeControl() {
        val actions: List<CelestiaAction>
        if (resources.configuration.layoutDirection == LayoutDirection.RTL) {
            actions = listOf(
                CelestiaAction.Faster,
                CelestiaAction.PlayPause,
                CelestiaAction.Slower,
                CelestiaAction.Reverse
            )
        } else {
            actions = listOf(
                CelestiaAction.Slower,
                CelestiaAction.PlayPause,
                CelestiaAction.Faster,
                CelestiaAction.Reverse
            )
        }
        showToolbarFragment(BottomControlFragment.newInstance(actions))
    }

    private fun showScriptControl() {
        showToolbarFragment(
            BottomControlFragment.newInstance(
                listOf(
                    CelestiaAction.PlayPause,
                    CelestiaAction.CancelScript
                )))
    }

    private fun showCameraControl() {
        showBottomSheetFragment(CameraControlContainerFragment.newInstance())
    }

    private fun showHelp() {
        showBottomSheetFragment(HelpFragment.newInstance())
    }

    private fun showFavorite() {
        readFavorites()
        val scripts = CelestiaScript.getScriptsInDirectory("scripts", true)
        extraScriptPath?.let { path ->
            scripts.addAll(CelestiaScript.getScriptsInDirectory(path, true))
        }
        updateCurrentScripts(scripts)
        updateCurrentDestinations(core.destinations)
        showBottomSheetFragment(FavoriteFragment.newInstance(FavoriteRoot()))
    }

    private fun showDestinations() {
        updateCurrentDestinations(core.destinations)
        showBottomSheetFragment(FavoriteFragment.newInstance(FavoriteTypeItem(FavoriteType.Destination)))
    }

    private fun showSettings() {
        showBottomSheetFragment(SettingsFragment.newInstance())
    }

    private fun showShare() {
        showOptions("", arrayOf(CelestiaString("Image", ""), CelestiaString("URL", ""))) { which ->
            when (which) {
                1 -> {
                    shareURL()
                }
                0 -> {
                    shareImage()
                }
                else -> {
                    Log.e(TAG, "Unknown selection in share $which")
                }
            }
        }
    }

    private fun shareURL() {
        CelestiaView.callOnRenderThread {
            val orig = core.currentURL
            val sel = core.simulation.selection
            val name = core.simulation.universe.getNameForSelection(sel)
            lifecycleScope.launch {
                shareURL(orig, name)
            }
        }
    }

    private fun shareURL(url: String, name: String) {
        val encodedURL = android.util.Base64.encodeToString(url.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
        showTextInput(CelestiaString("Share", ""), name) { title ->
            Toast.makeText(this, CelestiaString("Generating sharing link…", ""), Toast.LENGTH_SHORT).show()
            val service = ShareAPI.shared.create(ShareAPIService::class.java)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val result = service.create(title, encodedURL, versionCode.toString()).commonHandler(URLCreationResponse::class.java)
                    withContext(Dispatchers.Main) {
                        val intent = ShareCompat.IntentBuilder(this@MainActivity)
                            .setType("text/plain")
                            .setChooserTitle(name)
                            .setText(result.publicURL)
                            .intent
                        if (intent.resolveActivity(packageManager) != null)
                            startActivity(intent)
                        else
                            showUnsupportedAction()
                    }
                } catch (ignored: Throwable) {
                    withContext(Dispatchers.Main) {
                        showShareError()
                    }
                }
            }
        }
    }

    private fun shareImage() {
        val directory = File(cacheDir, "screenshots")
        if (!directory.exists())
            directory.mkdir()

        val file = File(directory, "${UUID.randomUUID().toString()}.png")
        CelestiaView.callOnRenderThread {
            core.draw()
            if (core.saveScreenshot(file.absolutePath, CelestiaAppCore.IMAGE_TYPE_PNG)) {
                lifecycleScope.launch {
                    shareFile(file, "image/png")
                }
            } else {
                lifecycleScope.launch {
                    Toast.makeText(this@MainActivity, CelestiaString("Unable to generate image.", ""), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(this, "space.celestia.mobilecelestia.fileprovider", file)
        val intent = ShareCompat.IntentBuilder(this)
            .setType(mimeType)
            .setStream(uri)
            .intent
        if (intent.resolveActivity(packageManager) != null)
            startActivity(intent)
        else
            showUnsupportedAction()
    }

    private fun showEventFinder() {
        showBottomSheetFragment(EventFinderContainerFragment.newInstance())
    }

    private fun showShareError() {
        showAlert(CelestiaString("Cannot share URL", ""))
    }

    // Resource
    private fun showOnlineResource() {
        showBottomSheetFragment(ResourceFragment.newInstance())
    }

    override fun onAsyncListItemSelected(item: Any) {
        val frag = supportFragmentManager.findFragmentById(R.id.bottom_sheet)
        if (frag is ResourceFragment) {
            if (item is ResourceCategory) {
                frag.pushItem(item)
            } else if (item is ResourceItem) {
                frag.pushItem(item)
            }
        }
    }

    private fun showGoTo(data: GoToInputFragment.GoToData? = null) {
        val inputData = data ?: GoToInputFragment.GoToData(
            CelestiaAppCore.getLocalizedString("Earth", "celestia"),
            0.0f,
            0.0f,
            8.0,
            CelestiaGoToLocation.DistanceUnit.radii
        )
        showBottomSheetFragment(GoToContainerFragment.newInstance(inputData))
    }

    override fun onGoToObject(goToData: GoToInputFragment.GoToData) {
        val selection = core.simulation.findObject(goToData.objectName)
        if (selection.isEmpty) {
            showAlert(CelestiaString("Object not found", ""))
            return
        }

        val location = CelestiaGoToLocation(selection, goToData.longitude, goToData.latitude, goToData.distance, goToData.distanceUnit)
        CelestiaView.callOnRenderThread {
            core.simulation.goToLocation(location)
        }
    }

    // Utilities
    private fun showMenuFragment(fragment: Fragment) {
        val ref = WeakReference(fragment)
        hideOverlay(true) {
            ref.get()?.let {
                showMenuFragmentDirect(it)
            }
        }
    }

    private fun showMenuFragmentDirect(fragment: Fragment) {
        findViewById<View>(R.id.menu_overlay).visibility = View.VISIBLE
        supportFragmentManager
            .beginTransaction()
            .add(R.id.menu_container, fragment)
            .commitAllowingStateLoss()
        showView(true, R.id.menu_container, true)
    }

    private fun showBottomSheetFragment(fragment: Fragment) {
        val ref = WeakReference(fragment)
        hideOverlay(true) {
            ref.get()?.let {
                showBottomSheetFragmentDirect(it)
            }
        }
    }

    private fun showBottomSheetFragmentDirect(fragment: Fragment) {
        findViewById<View>(R.id.bottom_sheet_overlay).visibility = View.VISIBLE
        supportFragmentManager
            .beginTransaction()
            .add(R.id.bottom_sheet, fragment)
            .commitAllowingStateLoss()
        showView(true, R.id.bottom_sheet_card, false)
    }

    private fun showToolbarFragment(fragment: Fragment) {
        val ref = WeakReference(fragment)
        hideOverlay(true) {
            ref.get()?.let {
                showToolbarFragmentDirect(it)
            }
        }
    }

    private fun showToolbarFragmentDirect(fragment: Fragment) {
        findViewById<View>(R.id.toolbar_overlay).visibility = View.VISIBLE
        supportFragmentManager
            .beginTransaction()
            .add(R.id.toolbar_container, fragment)
            .commitAllowingStateLoss()
        showView(true, R.id.toolbar_container, false)
    }

    companion object {
        private const val CURRENT_DATA_VERSION = "23"
        // 23: 1.3.3 Localization update
        // 22: 1.3.0 Localization update
        // 21: 1.2.10 Update content to commit 2a80a7695f1dea73de20d3411bfdf8eff94155e5
        // 20: 1.2.7 Privacy Policy and Service Agreement
        // 19: 1.2.2 Shader updates
        // 18: 1.2.1 ru.po/bg.po updates
        // 17: 1.2 Beta, shader updates
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

        private const val TOOLBAR_VISIBLE_TAG = "toolbar_visible"
        private const val MENU_VISIBLE_TAG = "menu_visible"
        private const val BOTTOM_SHEET_VISIBLE_TAG = "bottom_sheet_visible"
        private const val GO_TO_DATA_TAG = "go_to"

        private const val TAG = "MainActivity"

        var customDataDirPath: String? = null
        var customConfigFilePath: String? = null
        private var language: String = "en"
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
            System.loadLibrary("celestia")
            CelestiaAppCore.setUpLocale()
        }
    }
}
