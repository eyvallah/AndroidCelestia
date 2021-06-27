/*
 * CelestiaRenderer.java
 *
 * Copyright (C) 2001-2020, Celestia Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package space.celestia.mobilecelestia.core;

import android.app.Activity;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CelestiaRenderer {
    public interface Callback {
        void call();
    }

    private List<Callback> tasks = new ArrayList<>();
    private final Object taskLock = new Object();
    private final long pointer;
    private boolean started = false;

    private static CelestiaRenderer sharedRenderer = null;
    private EngineStartedListener engineStartedListener = null;

    private CelestiaRenderer(long pointer) {
        this.pointer = pointer;
        c_initialize(pointer);
    }

    @Override
    protected void finalize() throws Throwable {
        c_deinitialize(pointer);
        super.finalize();
    }

    public static CelestiaRenderer shared() {
        if (sharedRenderer == null)
            sharedRenderer = new CelestiaRenderer(c_createNativeRenderObject());
        return sharedRenderer;
    }

    public void makeContextCurrent() {
        c_makeContextCurrent(pointer);
    }

    public void enqueueTask(@NonNull Callback task) {
        synchronized (taskLock) {
            tasks.add(task);
        }
    }

    public void startConditionally(@NonNull Activity activity, boolean enableMultisample) {
        if (started) return;
        start(activity, enableMultisample);
    }

    public void start(@NonNull Activity activity, boolean enableMultisample) {
        started = true;
        c_start(pointer, activity, enableMultisample);
    }

    public void stop() {
        c_stop(pointer);
    }

    public void pause() {
        c_pause(pointer);
    }

    public void resume() {
        c_resume(pointer);
    }

    public void setSurface(@Nullable Surface surface) {
        c_setSurface(pointer, surface);
    }

    protected void setCorePointer(long ptr) {
        c_setCorePointer(pointer, ptr);
    }

    public void setSurfaceSize(int width, int height) {
        c_setSurfaceSize(pointer, width, height);
    }

    public interface EngineStartedListener {
        boolean onEngineStarted();
    }

    public void setEngineStartedListener(EngineStartedListener engineStartedListener) {
        this.engineStartedListener = engineStartedListener;
    }

    private boolean engineStarted() {
        boolean result = false;
        if (engineStartedListener != null)
            result = engineStartedListener.onEngineStarted();
        engineStartedListener = null;
        return result;
    }

    private void flushTasks() {
        ArrayList<Callback> taskCopy = new ArrayList<>();
        synchronized (taskLock) {
            if (tasks.isEmpty()) return;
            for (Callback task : tasks) {
                taskCopy.add(task);
            }
            tasks = new ArrayList<>();
        }
        for (Callback task : taskCopy) {
            task.call();
        }
    }

    private static native long c_createNativeRenderObject();
    private native void c_initialize(long pointer);
    private native void c_deinitialize(long pointer);
    private native void c_start(long pointer, Activity activity, boolean enableMultisample);
    private native void c_stop(long pointer);
    private native void c_pause(long pointer);
    private native void c_resume(long pointer);
    private native void c_setSurface(long pointer, Surface surface);
    private native void c_setCorePointer(long pointer, long corePtr);
    private native void c_setSurfaceSize(long pointer, int width, int height);
    private native void c_makeContextCurrent(long pointer);
}
