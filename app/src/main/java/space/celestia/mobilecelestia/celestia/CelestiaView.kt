/*
 * CelestiaView.kt
 *
 * Copyright (C) 2001-2020, Celestia Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package space.celestia.mobilecelestia.celestia

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.opengl.GLSurfaceView
import android.view.Choreographer
import android.view.SurfaceView
import space.celestia.mobilecelestia.core.CelestiaRenderer

@SuppressLint("ViewConstructor")
class CelestiaView(context: Context, private val scaleFactor: Float) : SurfaceView(context) {
    var isReady = false

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        holder.setFixedSize((width * scaleFactor).toInt(), (height * scaleFactor).toInt())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        sharedView = this
    }

    override fun onDetachedFromWindow() {
        sharedView = null

        super.onDetachedFromWindow()
    }

    companion object {
        private var sharedView: CelestiaView? = null
        private val renderer by lazy { CelestiaRenderer.shared() }

        // Call on render thread to avoid concurrency issue.
        fun callOnRenderThread(block: () -> Unit) {
            renderer.enqueueTask(block)
        }
    }
}

fun PointF.scaleBy(factor: Float): PointF {
    return PointF(x * factor, y * factor)
}