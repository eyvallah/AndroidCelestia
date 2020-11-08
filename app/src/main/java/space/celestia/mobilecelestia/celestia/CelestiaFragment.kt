/*
 * CelestiaFragment.kt
 *
 * Copyright (C) 2001-2020, Celestia Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package space.celestia.mobilecelestia.celestia

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import space.celestia.mobilecelestia.MainActivity
import space.celestia.mobilecelestia.R
import space.celestia.mobilecelestia.browser.createAllBrowserItems
import space.celestia.mobilecelestia.core.CelestiaAppCore
import space.celestia.mobilecelestia.core.CelestiaRenderer
import space.celestia.mobilecelestia.utils.AppStatusReporter
import space.celestia.mobilecelestia.utils.CelestiaString
import space.celestia.mobilecelestia.utils.FontHelper
import java.util.*
import java.util.concurrent.TimeUnit

class CelestiaFragment: Fragment(), SurfaceHolder.Callback, CelestiaControlView.Listener {
    private var activity: Activity? = null

    // MARK: GL View
    private var glViewContainer: FrameLayout? = null
    private var glView: CelestiaView? = null
    private var glViewSize: Size? = null
    private var viewInteraction: CelestiaInteraction? = null

    private var currentControlViewID = R.id.active_control_view_container

    // MARK: Celestia
    private var pathToLoad: String? = null
    private var cfgToLoad: String? = null
    private var addonToLoad: String? = null
    private var enableMultisample = false
    private var enableFullResolution = false
    private var languageOverride: String? = null
    private val core by lazy { CelestiaAppCore.shared() }
    private val renderer by lazy { CelestiaRenderer.shared() }

    private val scaleFactor: Float
        get() = if (enableFullResolution) 1.0f else (1.0f / resources.displayMetrics.density)

    private val controlMargin
        get() = (4 * resources.displayMetrics.density).toInt()
    private val controlContainerTrailingMargin
        get() = (8 * resources.displayMetrics.density).toInt()

    private var zoomTimer: Disposable? = null

    private var loadSuccess = false

    interface Listener {
        fun celestiaFragmentDidRequestActionMenu()
        fun celestiaFragmentDidRequestObjectInfo()
    }

    var listener: Listener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            pathToLoad = it.getString(ARG_DATA_DIR)
            cfgToLoad = it.getString(ARG_CFG_FILE)
            addonToLoad = it.getString(ARG_ADDON_DIR)
            enableMultisample = it.getBoolean(ARG_MULTI_SAMPLE)
            enableFullResolution = it.getBoolean(ARG_FULL_RESOLUTION)
            languageOverride = it.getString(ARG_LANG_OVERRIDE)
        }

        core.setRenderer(renderer)
        renderer.setEngineStartedListener {
            this.load()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_celestia, container, false)
        glViewContainer = view.findViewById(R.id.celestia_gl_view)
        setupGLView()

        val activeControlView = CelestiaControlView(inflater.context, listOf(
            CelestiaToggleButton(R.drawable.control_mode_combined, CelestiaControlAction.ToggleModeToObject, CelestiaControlAction.ToggleModeToCamera),
            CelestiaPressButton(R.drawable.control_zoom_in, CelestiaControlAction.ZoomIn),
            CelestiaPressButton(R.drawable.control_zoom_out, CelestiaControlAction.ZoomOut),
            CelestiaTapButton(R.drawable.control_info, CelestiaControlAction.Info),
            CelestiaTapButton(R.drawable.control_action_menu, CelestiaControlAction.ShowMenu),
            CelestiaTapButton(R.drawable.control_hide, CelestiaControlAction.Hide)
        ))
        val inactiveControlView = CelestiaControlView(inflater.context, listOf(
            CelestiaTapButton(R.drawable.control_show, CelestiaControlAction.Show)
        ))
        val activeControlContainer = view.findViewById<FrameLayout>(R.id.active_control_view_container)
        val inactiveControlContainer = view.findViewById<FrameLayout>(R.id.inactive_control_view_container)

        activeControlContainer.addView(activeControlView)
        inactiveControlContainer.addView(inactiveControlView)

        val layoutParamsForControls = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        layoutParamsForControls.setMargins(controlMargin, controlMargin, controlMargin, controlMargin)
        activeControlView.layoutParams = layoutParamsForControls
        inactiveControlView.layoutParams = layoutParamsForControls

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setOnApplyWindowInsetsListener { _, insets ->
                insets.displayCutout?.let {
                    applyCutout(it)
                }
                return@setOnApplyWindowInsetsListener insets
            }
        }

        activeControlView.listener = this
        inactiveControlView.listener = this
        return view
    }

    override fun onPause() {
        super.onPause()

        renderer.pause()
    }

    override fun onResume() {
        super.onResume()

        renderer.resume()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is Listener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement CelestiaFragment.Listener")
        }
        activity = context as? Activity
    }

    override fun onDetach() {
        super.onDetach()

        listener = null
        activity = null
        zoomTimer?.dispose()
        zoomTimer = null
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun applyCutout(cutout: DisplayCutout) {
        if (!loadSuccess) { return }

        CelestiaView.callOnRenderThread {
            core.setSafeAreaInsets(cutout.safeInsets().scaleBy(scaleFactor))
        }

        val ltr = resources.configuration.layoutDirection != View.LAYOUT_DIRECTION_RTL
        val safeInsetEnd = if (ltr) cutout.safeInsetRight else cutout.safeInsetLeft

        val controlView = view?.findViewById<FrameLayout>(currentControlViewID) ?: return
        val params = controlView.layoutParams as? ConstraintLayout.LayoutParams
        if (params != null) {
            params.marginEnd = controlContainerTrailingMargin + safeInsetEnd
            controlView.layoutParams = params
        }
    }

    private fun setupGLView() {
        val activity = this.activity ?: return
        val container = glViewContainer ?: return
        val view = CelestiaView(activity, scaleFactor)

        val interaction = CelestiaInteraction(activity)
        glView = view
        viewInteraction = interaction

        interaction.scaleFactor = scaleFactor
        interaction.density = resources.displayMetrics.density
        view.isFocusable = true
        renderer.start(activity, enableMultisample)
        container.addView(view, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        view.holder?.addCallback(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun loadCelestia(path: String, cfg: String, addon: String?) {
        CelestiaAppCore.chdir(path)

        // Set up locale
        CelestiaAppCore.setLocaleDirectoryPath("$path/locale", languageOverride ?: Locale.getDefault().toString())

        val extraDirs = if (addon != null) arrayOf(addon) else null

        // Reading config, data
        if (!core.startSimulation(cfg, extraDirs, AppStatusReporter.shared())) {
            AppStatusReporter.shared().celestiaLoadResult(false)
            return
        }

        // Prepare renderer
        if (!core.startRenderer()) {
            AppStatusReporter.shared().celestiaLoadResult(false)
            return
        }

        // Prepare for browser items
        core.simulation.createAllBrowserItems()

        glViewSize?.let {
            core.resize(it.width, it.height)
            glViewSize = null
        }

        val density = resources.displayMetrics.density
        core.setDPI((96 * density * scaleFactor).toInt())
        core.setPickTolerance(10f * density * scaleFactor)

        val locale = CelestiaAppCore.getLocalizedString("LANGUAGE", "celestia")

        val font: FontHelper.FontCompat?
        val boldFont: FontHelper.FontCompat?
        // Use installed font
        val preferredInstalledFont = MainActivity.availableInstalledFonts[locale] ?: MainActivity.defaultInstalledFont
        if (preferredInstalledFont != null) {
            font = preferredInstalledFont.first
            boldFont = preferredInstalledFont.second
        } else {
            font = FontHelper.getFontForLocale(locale, 400)
            boldFont = FontHelper.getFontForLocale(locale, 700)
        }
        if (font != null && boldFont != null) {
            core.setFont(font.filePath, font.collectionIndex, 9)
            core.setTitleFont(boldFont.filePath, boldFont.collectionIndex, 15)
            core.setRendererFont(font.filePath, font.collectionIndex, 9, CelestiaAppCore.RENDER_FONT_STYLE_NORMAL)
            core.setRendererFont(boldFont.filePath, boldFont.collectionIndex, 15, CelestiaAppCore.RENDER_FONT_STYLE_LARGE)
        }

        // Display
        core.tick()
        core.start()

        glView?.isReady = true
        viewInteraction?.isReady = true
        glView?.setOnTouchListener(viewInteraction)
        glView?.setOnKeyListener(viewInteraction)
        loadSuccess = true

        Log.d(TAG, "Ready to display")
        AppStatusReporter.shared().celestiaLoadResult(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity?.runOnUiThread {
                view?.rootWindowInsets?.displayCutout?.let { applyCutout(it) }
            }
        }

        celestiaLoaded = true
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        renderer.setSurface(holder?.surface)
    }

    override fun onDestroy() {
        if (celestiaLoaded)
            renderer.stop()

        super.onDestroy()
    }

    private fun load() {
        val data = pathToLoad
        val cfg = cfgToLoad
        val addon = addonToLoad

        pathToLoad = null
        cfgToLoad = null
        addonToLoad = null

        if (data == null || cfg == null) { return }

        CelestiaAppCore.initGL()

        loadCelestia(data, cfg, addon)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        glViewSize = Size(width, height)
        Log.d(TAG, "Resize to $width x $height")
        renderer.setSurfaceSize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        renderer.setSurface(null)
    }

    // Actions
    override fun didTapAction(action: CelestiaControlAction) {
        when (action) {
            CelestiaControlAction.ShowMenu -> {
                listener?.celestiaFragmentDidRequestActionMenu()
            }
            CelestiaControlAction.Info -> {
                listener?.celestiaFragmentDidRequestObjectInfo()
            }
            CelestiaControlAction.Hide -> {
                hideCurrentControlViewToShow(R.id.inactive_control_view_container)
            }
            CelestiaControlAction.Show -> {
                hideCurrentControlViewToShow(R.id.active_control_view_container)
            }
            else -> {}
        }
    }

    private fun hideCurrentControlViewToShow(anotherView: Int) {
        val current = view?.findViewById<FrameLayout>(currentControlViewID) ?: return
        val new = view?.findViewById<FrameLayout>(anotherView) ?: return

        val ltr = resources.configuration.layoutDirection != View.LAYOUT_DIRECTION_RTL

        val density = resources.displayMetrics.density

        if (current == new) { return }

        var maxValue = current.width + controlContainerTrailingMargin
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            maxValue += if (ltr)
                current.rootWindowInsets.displayCutout?.safeInsetRight ?: 0
            else
                current.rootWindowInsets.displayCutout?.safeInsetLeft ?: 0
        }

        // Reserve 1 dp to ensure it does not completely fall off the screen
        val hideAnimator = ObjectAnimator.ofFloat(current, "translationX", 0f, (if (ltr) 1 else -1) * (maxValue - density))
        hideAnimator.duration = 200
        hideAnimator.start()

        val finishBlock: (Animator) -> Unit = {
            val currentLayoutParams = current.layoutParams as? ConstraintLayout.LayoutParams
            val newLayoutParams = new.layoutParams as? ConstraintLayout.LayoutParams
            if (currentLayoutParams != null && newLayoutParams != null) {
                currentLayoutParams.startToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                currentLayoutParams.endToEnd = ConstraintLayout.LayoutParams.UNSET
                currentLayoutParams.marginEnd = 0
                current.layoutParams = currentLayoutParams
                current.translationX = -1 * density

                newLayoutParams.startToEnd = ConstraintLayout.LayoutParams.UNSET
                newLayoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID

                var insetEnd = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    insetEnd = if (ltr)
                        new.rootWindowInsets.displayCutout?.safeInsetRight ?: 0
                    else
                        new.rootWindowInsets.displayCutout?.safeInsetLeft ?: 0
                }

                newLayoutParams.marginEnd = insetEnd + controlContainerTrailingMargin
                new.layoutParams = newLayoutParams
                new.translationX = 0f
            }
            currentControlViewID = anotherView
        }

        val showAnimator = ObjectAnimator.ofFloat(new, "translationX", 0f, -maxValue.toFloat())

        hideAnimator.addListener(onEnd = {
            showAnimator.duration = 200
            showAnimator.start()
        }, onCancel = finishBlock)

        showAnimator.addListener(onEnd = finishBlock, onCancel = finishBlock)
    }

    override fun didToggleToMode(action: CelestiaControlAction) {
        val viewInteraction = this.viewInteraction ?: return

        when (action) {
            CelestiaControlAction.ToggleModeToCamera -> {
                viewInteraction.setInteractionMode(CelestiaInteraction.InteractionMode.Camera)
                activity?.let {
                    Toast.makeText(it, CelestiaString("Switched to camera mode", ""), Toast.LENGTH_SHORT).show()
                }
            }
            CelestiaControlAction.ToggleModeToObject -> {
                viewInteraction.setInteractionMode(CelestiaInteraction.InteractionMode.Object)
                activity?.let {
                    Toast.makeText(it, CelestiaString("Switched to object mode", ""), Toast.LENGTH_SHORT).show()
                }
            }
            else -> {}
        }
    }

    override fun didStartPressingAction(action: CelestiaControlAction) {
        val viewInteraction = this.viewInteraction ?: return

        when (action) {
            CelestiaControlAction.ZoomIn -> { viewInteraction.zoomMode = CelestiaInteraction.ZoomMode.In; viewInteraction.callZoom() }
            CelestiaControlAction.ZoomOut -> { viewInteraction.zoomMode = CelestiaInteraction.ZoomMode.Out; viewInteraction.callZoom() }
            else -> {}
        }

        zoomTimer?.dispose()
        zoomTimer = Observable.interval(100, TimeUnit.MILLISECONDS).subscribe {
            viewInteraction.callZoom()
        }
    }

    override fun didEndPressingAction(action: CelestiaControlAction) {
        zoomTimer?.dispose()
        zoomTimer = null
        viewInteraction?.zoomMode = null
    }

    companion object {
        private const val ARG_DATA_DIR = "data"
        private const val ARG_CFG_FILE = "cfg"
        private const val ARG_ADDON_DIR = "addon"
        private const val ARG_MULTI_SAMPLE = "multisample"
        private const val ARG_FULL_RESOLUTION = "fullresolution"
        private const val ARG_LANG_OVERRIDE = "lang"

        private const val TAG = "CelestiaFragment"

        var celestiaLoaded = false

        fun newInstance(data: String, cfg: String, addon: String?, enableMultisample: Boolean, enableFullResolution: Boolean, languageOverride: String?) =
            CelestiaFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DATA_DIR, data)
                    putString(ARG_CFG_FILE, cfg)
                    putBoolean(ARG_MULTI_SAMPLE, enableMultisample)
                    putBoolean(ARG_FULL_RESOLUTION, enableFullResolution)
                    putString(ARG_LANG_OVERRIDE, languageOverride)
                    if (addon != null) {
                        putString(ARG_ADDON_DIR, addon)
                    }
                }
            }
    }
}

class Insets(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun scaleBy(factor: Float): Insets {
        return Insets(
            (left * factor).toInt(),
            (top * factor).toInt(),
            (right * factor).toInt(),
            (bottom * factor).toInt()
         )
    }
}

@RequiresApi(Build.VERSION_CODES.P)
fun DisplayCutout.safeInsets(): Insets {
    return Insets(safeInsetLeft, safeInsetTop, safeInsetRight, safeInsetBottom)
}

fun CelestiaAppCore.setSafeAreaInsets(insets: Insets) {
    setSafeAreaInsets(insets.left, insets.top, insets.right, insets.bottom)
}