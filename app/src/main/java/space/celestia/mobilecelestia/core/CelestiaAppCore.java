/*
 * CelestiaAppCore.java
 *
 * Copyright (C) 2001-2020, Celestia Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package space.celestia.mobilecelestia.core;

import android.graphics.PointF;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.Date;

public class CelestiaAppCore {
    public static final int MOUSE_BUTTON_LEFT       = 0x01;
    public static final int MOUSE_BUTTON_MIDDLE     = 0x02;
    public static final int MOUSE_BUTTON_RIGHT      = 0x04;
    public static final int SHIFT_KEY               = 0x08;
    public static final int CONTROL_KEY             = 0x10;

    public static final int RENDER_FONT_STYLE_NORMAL    = 0;
    public static final int RENDER_FONT_STYLE_LARGE     = 1;

    public interface ProgressWatcher {
        void onCelestiaProgress(@NonNull String progress);
    }

    private final long pointer;
    private boolean initialized;
    private CelestiaSimulation simulation;

    // Singleton
    private static CelestiaAppCore shared;
    public static CelestiaAppCore shared() {
        if (shared == null)
            shared = new CelestiaAppCore();
        return shared;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public CelestiaAppCore() {
        this.pointer = c_init();
        this.initialized = false;
        this.simulation = null;
    }

    public boolean startRenderer() {
        return c_startRenderer(pointer);
    }

    public boolean startSimulation(@Nullable String configFileName, @Nullable String[] extraDirectories, @Nullable ProgressWatcher watcher) {
        initialized = c_startSimulation(pointer, configFileName, extraDirectories, watcher);
        return initialized;
    }

    public void start() {
        c_start(pointer);
    }

    public void start(@NonNull Date date) {
        c_start(pointer, (double)date.getTime() / 1000);
    }

    public void draw() {
        c_draw(pointer);
    }

    public void tick() {
        c_tick(pointer);
    }

    public void resize(int w, int h) {
        c_resize(pointer, w, h);
    }

    public void setSafeAreaInsets(int left, int top, int right, int bottom) {
        c_setSafeAreaInsets(pointer, left, top, right, bottom);
    }

    public void setDPI(int dpi) {
        c_setDPI(pointer, dpi);
    }

    // Control
    public void mouseButtonUp(int buttons, PointF point, int modifiers) {
        c_mouseButtonUp(pointer, buttons, point.x, point.y, modifiers);
    }

    public void mouseButtonDown(int buttons, PointF point, int modifiers) {
        c_mouseButtonDown(pointer, buttons, point.x, point.y, modifiers);
    }

    public void mouseMove(int buttons, PointF offset, int modifiers) {
        c_mouseMove(pointer, buttons, offset.x, offset.y, modifiers);
    }

    public void mouseWheel(float motion, int modifiers) {
        c_mouseWheel(pointer, motion, modifiers);
    }

    public void keyUp(int input) {
        c_keyUp(pointer, input);
    }

    public void keyDown(int input) {
        c_keyDown(pointer, input);
    }

    public void keyUp(int input, int key, int modifiers) {
        c_keyUpWithModifiers(pointer, input, key, modifiers);
    }

    public void keyDown(int input, int key, int modifers) {
        c_keyDownWithModifers(pointer, input, key, modifers);
    }

    public void charEnter(int input) {
        c_charEnter(pointer, input);
    }

    public void runScript(@NonNull String scriptPath) { c_runScript(pointer, scriptPath); }
    public @NonNull String getCurrentURL() { return c_getCurrentURL(pointer); }
    public void goToURL(@NonNull String url) { c_goToURL(pointer, url); }

    public void setFont(@NonNull String fontPath, int collectionIndex, int fontSize) {
        c_setFont(pointer, fontPath, collectionIndex, fontSize);
    }

    public void setTitleFont(@NonNull String fontPath, int collectionIndex, int fontSize) {
        c_setTitleFont(pointer, fontPath, collectionIndex, fontSize);
    }

    public void setRendererFont(@NonNull String fontPath, int collectionIndex, int fontSize, int fontStyle) {
        c_setRendererFont(pointer, fontPath, collectionIndex, fontSize, fontStyle);
    }

    public void setPickTolerance(float pickTolerance) {
        c_setPickTolerance(pointer, pickTolerance);
    }

    public static boolean initGL() {
        return c_initGL();
    }
    public static void chdir(String path) { c_chdir(path);}

    public void setRenderer(CelestiaRenderer renderer) {
        renderer.setCorePointer(pointer);
    }

    // Locale
    public static void setLocaleDirectoryPath(@NonNull String localeDirectoryPath, @NonNull String locale) { c_setLocaleDirectoryPath(localeDirectoryPath, locale); }
    public static @NonNull String getLocalizedString(@NonNull String string) { return getLocalizedString(string, "celestia_ui"); }
    public static @NonNull String getLocalizedString(@NonNull String string, @NonNull String domain) { return c_getLocalizedString(string, domain); }
    public static @NonNull String getLocalizedFilename(@NonNull String filename) { return c_getLocalizedFilename(filename); }

    public CelestiaSimulation getSimulation() {
        if (simulation == null)
            simulation = new CelestiaSimulation(c_getSimulation(pointer));
        return simulation;
    }

    public @NonNull String getRenderInfo() {
        return c_getRenderInfo(pointer);
    }

    // C function
    private static native long c_init();
    private static native boolean c_startRenderer(long ptr);
    private static native boolean c_startSimulation(long ptr, String configFileName, String[] extraDirectories, ProgressWatcher watcher);
    private static native void c_start(long ptr);
    private static native void c_start(long ptr, double secondsSinceEpoch);
    private static native void c_draw(long ptr);
    private static native void c_tick(long ptr);
    private static native void c_resize(long ptr, int w, int h);
    private static native void c_setSafeAreaInsets(long ptr, int left, int top, int right, int bottom);
    private static native void c_setDPI(long ptr, int dpi);
    private static native long c_getSimulation(long ptr);

    // Control
    private static native void c_mouseButtonUp(long ptr, int buttons, float x, float y, int modifiers);
    private static native void c_mouseButtonDown(long ptr, int buttons, float x, float y, int modifiers);
    private static native void c_mouseMove(long ptr, int buttons, float x, float y, int modifiers);
    private static native void c_mouseWheel(long ptr, float motion, int modifiers);
    private static native void c_keyUpWithModifiers(long ptr, int input, int key, int modifiers);
    private static native void c_keyDownWithModifers(long ptr, int input, int key, int modifiers);
    private static native void c_keyUp(long ptr, int input);
    private static native void c_keyDown(long ptr, int input);
    private static native void c_charEnter(long ptr, int input);

    private static native void c_runScript(long ptr, String path);
    private static native String c_getCurrentURL(long ptr);
    private static native void c_goToURL(long ptr, String url);

    private static native void c_setFont(long ptr, String fontPath, int collectionIndex, int fontSize);
    private static native void c_setTitleFont(long ptr, String fontPath, int collectionIndex, int fontSize);
    private static native void c_setRendererFont(long ptr, String fontPath, int collectionIndex, int fontSize, int fontStyle);

    private static native void c_setPickTolerance(long ptr, float pickTolerance);

    private static native boolean c_initGL();
    private static native void c_chdir(String path);

    // Locale
    private static native void c_setLocaleDirectoryPath(String path, String locale);
    private static native String c_getLocalizedString(String string, String domain);
    private static native String c_getLocalizedFilename(String filename);

    private static native String c_getRenderInfo(long ptr);

    private final static String TAG = "CelestiaAppCore";

    // Settings
    public boolean getBooleanValueForPield(@NonNull String field) {
        try {
            Method method = getClass().getDeclaredMethod("get" + field);
            Object value = method.invoke(this);
            return (boolean)value;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get field " + field);
        }
        return false;
    }

    public void setBooleanValueForField(@NonNull String field, boolean value) {
        try {
            Method method = getClass().getDeclaredMethod("set" + field, boolean.class);
            method.invoke(this, value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set field " + field);
        }
    }

    public int getIntValueForField(@NonNull String field) {
        try {
            Method method = getClass().getDeclaredMethod("get" + field);
            Object value = method.invoke(this);
            return (int)value;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get field " + field);
        }
        return 0;
    }

    public void setIntValueForField(@NonNull String field, int value) {
        try {
            Method method = getClass().getDeclaredMethod("set" + field, int.class);
            method.invoke(this, value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set field " + field);
        }
    }

    public void setDoubleValueForField(@NonNull String field, double value) {
        try {
            Method method = getClass().getDeclaredMethod("set" + field, double.class);
            method.invoke(this, value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set field " + field);
        }
    }

    public double getDoubleValueForField(@NonNull String field) {
        try {
            Method method = getClass().getDeclaredMethod("get" + field);
            Object value = method.invoke(this);
            return (double)value;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get field " + field);
        }
        return 0.0;
    }

    public boolean getShowStars() { return c_getShowStars(); }
    public void setShowStars(boolean showStars) { c_setShowStars(showStars); }
    private native void c_setShowStars(boolean showStars);
    private native boolean c_getShowStars();
    public boolean getShowPlanets() { return c_getShowPlanets(); }
    public void setShowPlanets(boolean showPlanets) { c_setShowPlanets(showPlanets); }
    private native void c_setShowPlanets(boolean showPlanets);
    private native boolean c_getShowPlanets();
    public boolean getShowDwarfPlanets() { return c_getShowDwarfPlanets(); }
    public void setShowDwarfPlanets(boolean showDwarfPlanets) { c_setShowDwarfPlanets(showDwarfPlanets); }
    private native void c_setShowDwarfPlanets(boolean showDwarfPlanets);
    private native boolean c_getShowDwarfPlanets();
    public boolean getShowMoons() { return c_getShowMoons(); }
    public void setShowMoons(boolean showMoons) { c_setShowMoons(showMoons); }
    private native void c_setShowMoons(boolean showMoons);
    private native boolean c_getShowMoons();
    public boolean getShowMinorMoons() { return c_getShowMinorMoons(); }
    public void setShowMinorMoons(boolean showMinorMoons) { c_setShowMinorMoons(showMinorMoons); }
    private native void c_setShowMinorMoons(boolean showMinorMoons);
    private native boolean c_getShowMinorMoons();
    public boolean getShowAsteroids() { return c_getShowAsteroids(); }
    public void setShowAsteroids(boolean showAsteroids) { c_setShowAsteroids(showAsteroids); }
    private native void c_setShowAsteroids(boolean showAsteroids);
    private native boolean c_getShowAsteroids();
    public boolean getShowComets() { return c_getShowComets(); }
    public void setShowComets(boolean showComets) { c_setShowComets(showComets); }
    private native void c_setShowComets(boolean showComets);
    private native boolean c_getShowComets();
    public boolean getShowSpacecrafts() { return c_getShowSpacecrafts(); }
    public void setShowSpacecrafts(boolean showSpacecrafts) { c_setShowSpacecrafts(showSpacecrafts); }
    private native void c_setShowSpacecrafts(boolean showSpacecrafts);
    private native boolean c_getShowSpacecrafts();
    public boolean getShowGalaxies() { return c_getShowGalaxies(); }
    public void setShowGalaxies(boolean showGalaxies) { c_setShowGalaxies(showGalaxies); }
    private native void c_setShowGalaxies(boolean showGalaxies);
    private native boolean c_getShowGalaxies();
    public boolean getShowGlobulars() { return c_getShowGlobulars(); }
    public void setShowGlobulars(boolean showGlobulars) { c_setShowGlobulars(showGlobulars); }
    private native void c_setShowGlobulars(boolean showGlobulars);
    private native boolean c_getShowGlobulars();
    public boolean getShowNebulae() { return c_getShowNebulae(); }
    public void setShowNebulae(boolean showNebulae) { c_setShowNebulae(showNebulae); }
    private native void c_setShowNebulae(boolean showNebulae);
    private native boolean c_getShowNebulae();
    public boolean getShowOpenClusters() { return c_getShowOpenClusters(); }
    public void setShowOpenClusters(boolean showOpenClusters) { c_setShowOpenClusters(showOpenClusters); }
    private native void c_setShowOpenClusters(boolean showOpenClusters);
    private native boolean c_getShowOpenClusters();
    public boolean getShowDiagrams() { return c_getShowDiagrams(); }
    public void setShowDiagrams(boolean showDiagrams) { c_setShowDiagrams(showDiagrams); }
    private native void c_setShowDiagrams(boolean showDiagrams);
    private native boolean c_getShowDiagrams();
    public boolean getShowBoundaries() { return c_getShowBoundaries(); }
    public void setShowBoundaries(boolean showBoundaries) { c_setShowBoundaries(showBoundaries); }
    private native void c_setShowBoundaries(boolean showBoundaries);
    private native boolean c_getShowBoundaries();
    public boolean getShowCloudMaps() { return c_getShowCloudMaps(); }
    public void setShowCloudMaps(boolean showCloudMaps) { c_setShowCloudMaps(showCloudMaps); }
    private native void c_setShowCloudMaps(boolean showCloudMaps);
    private native boolean c_getShowCloudMaps();
    public boolean getShowNightMaps() { return c_getShowNightMaps(); }
    public void setShowNightMaps(boolean showNightMaps) { c_setShowNightMaps(showNightMaps); }
    private native void c_setShowNightMaps(boolean showNightMaps);
    private native boolean c_getShowNightMaps();
    public boolean getShowAtmospheres() { return c_getShowAtmospheres(); }
    public void setShowAtmospheres(boolean showAtmospheres) { c_setShowAtmospheres(showAtmospheres); }
    private native void c_setShowAtmospheres(boolean showAtmospheres);
    private native boolean c_getShowAtmospheres();
    public boolean getShowCometTails() { return c_getShowCometTails(); }
    public void setShowCometTails(boolean showCometTails) { c_setShowCometTails(showCometTails); }
    private native void c_setShowCometTails(boolean showCometTails);
    private native boolean c_getShowCometTails();
    public boolean getShowPlanetRings() { return c_getShowPlanetRings(); }
    public void setShowPlanetRings(boolean showPlanetRings) { c_setShowPlanetRings(showPlanetRings); }
    private native void c_setShowPlanetRings(boolean showPlanetRings);
    private native boolean c_getShowPlanetRings();
    public boolean getShowMarkers() { return c_getShowMarkers(); }
    public void setShowMarkers(boolean showMarkers) { c_setShowMarkers(showMarkers); }
    private native void c_setShowMarkers(boolean showMarkers);
    private native boolean c_getShowMarkers();
    public boolean getShowOrbits() { return c_getShowOrbits(); }
    public void setShowOrbits(boolean showOrbits) { c_setShowOrbits(showOrbits); }
    private native void c_setShowOrbits(boolean showOrbits);
    private native boolean c_getShowOrbits();
    public boolean getShowPartialTrajectories() { return c_getShowPartialTrajectories(); }
    public void setShowPartialTrajectories(boolean showPartialTrajectories) { c_setShowPartialTrajectories(showPartialTrajectories); }
    private native void c_setShowPartialTrajectories(boolean showPartialTrajectories);
    private native boolean c_getShowPartialTrajectories();
    public boolean getShowSmoothLines() { return c_getShowSmoothLines(); }
    public void setShowSmoothLines(boolean showSmoothLines) { c_setShowSmoothLines(showSmoothLines); }
    private native void c_setShowSmoothLines(boolean showSmoothLines);
    private native boolean c_getShowSmoothLines();
    public boolean getShowEclipseShadows() { return c_getShowEclipseShadows(); }
    public void setShowEclipseShadows(boolean showEclipseShadows) { c_setShowEclipseShadows(showEclipseShadows); }
    private native void c_setShowEclipseShadows(boolean showEclipseShadows);
    private native boolean c_getShowEclipseShadows();
    public boolean getShowRingShadows() { return c_getShowRingShadows(); }
    public void setShowRingShadows(boolean showRingShadows) { c_setShowRingShadows(showRingShadows); }
    private native void c_setShowRingShadows(boolean showRingShadows);
    private native boolean c_getShowRingShadows();
    public boolean getShowCloudShadows() { return c_getShowCloudShadows(); }
    public void setShowCloudShadows(boolean showCloudShadows) { c_setShowCloudShadows(showCloudShadows); }
    private native void c_setShowCloudShadows(boolean showCloudShadows);
    private native boolean c_getShowCloudShadows();
    public boolean getShowAutoMag() { return c_getShowAutoMag(); }
    public void setShowAutoMag(boolean showAutoMag) { c_setShowAutoMag(showAutoMag); }
    private native void c_setShowAutoMag(boolean showAutoMag);
    private native boolean c_getShowAutoMag();
    public boolean getShowCelestialSphere() { return c_getShowCelestialSphere(); }
    public void setShowCelestialSphere(boolean showCelestialSphere) { c_setShowCelestialSphere(showCelestialSphere); }
    private native void c_setShowCelestialSphere(boolean showCelestialSphere);
    private native boolean c_getShowCelestialSphere();
    public boolean getShowEclipticGrid() { return c_getShowEclipticGrid(); }
    public void setShowEclipticGrid(boolean showEclipticGrid) { c_setShowEclipticGrid(showEclipticGrid); }
    private native void c_setShowEclipticGrid(boolean showEclipticGrid);
    private native boolean c_getShowEclipticGrid();
    public boolean getShowHorizonGrid() { return c_getShowHorizonGrid(); }
    public void setShowHorizonGrid(boolean showHorizonGrid) { c_setShowHorizonGrid(showHorizonGrid); }
    private native void c_setShowHorizonGrid(boolean showHorizonGrid);
    private native boolean c_getShowHorizonGrid();
    public boolean getShowGalacticGrid() { return c_getShowGalacticGrid(); }
    public void setShowGalacticGrid(boolean showGalacticGrid) { c_setShowGalacticGrid(showGalacticGrid); }
    private native void c_setShowGalacticGrid(boolean showGalacticGrid);
    private native boolean c_getShowGalacticGrid();

    public boolean getShowStarLabels() { return c_getShowStarLabels(); }
    public void setShowStarLabels(boolean showStarLabels) { c_setShowStarLabels(showStarLabels); }
    private native void c_setShowStarLabels(boolean showStarLabels);
    private native boolean c_getShowStarLabels();
    public boolean getShowPlanetLabels() { return c_getShowPlanetLabels(); }
    public void setShowPlanetLabels(boolean showPlanetLabels) { c_setShowPlanetLabels(showPlanetLabels); }
    private native void c_setShowPlanetLabels(boolean showPlanetLabels);
    private native boolean c_getShowPlanetLabels();
    public boolean getShowMoonLabels() { return c_getShowMoonLabels(); }
    public void setShowMoonLabels(boolean showMoonLabels) { c_setShowMoonLabels(showMoonLabels); }
    private native void c_setShowMoonLabels(boolean showMoonLabels);
    private native boolean c_getShowMoonLabels();
    public boolean getShowConstellationLabels() { return c_getShowConstellationLabels(); }
    public void setShowConstellationLabels(boolean showConstellationLabels) { c_setShowConstellationLabels(showConstellationLabels); }
    private native void c_setShowConstellationLabels(boolean showConstellationLabels);
    private native boolean c_getShowConstellationLabels();
    public boolean getShowGalaxyLabels() { return c_getShowGalaxyLabels(); }
    public void setShowGalaxyLabels(boolean showGalaxyLabels) { c_setShowGalaxyLabels(showGalaxyLabels); }
    private native void c_setShowGalaxyLabels(boolean showGalaxyLabels);
    private native boolean c_getShowGalaxyLabels();
    public boolean getShowGlobularLabels() { return c_getShowGlobularLabels(); }
    public void setShowGlobularLabels(boolean showGlobularLabels) { c_setShowGlobularLabels(showGlobularLabels); }
    private native void c_setShowGlobularLabels(boolean showGlobularLabels);
    private native boolean c_getShowGlobularLabels();
    public boolean getShowNebulaLabels() { return c_getShowNebulaLabels(); }
    public void setShowNebulaLabels(boolean showNebulaLabels) { c_setShowNebulaLabels(showNebulaLabels); }
    private native void c_setShowNebulaLabels(boolean showNebulaLabels);
    private native boolean c_getShowNebulaLabels();
    public boolean getShowOpenClusterLabels() { return c_getShowOpenClusterLabels(); }
    public void setShowOpenClusterLabels(boolean showOpenClusterLabels) { c_setShowOpenClusterLabels(showOpenClusterLabels); }
    private native void c_setShowOpenClusterLabels(boolean showOpenClusterLabels);
    private native boolean c_getShowOpenClusterLabels();
    public boolean getShowAsteroidLabels() { return c_getShowAsteroidLabels(); }
    public void setShowAsteroidLabels(boolean showAsteroidLabels) { c_setShowAsteroidLabels(showAsteroidLabels); }
    private native void c_setShowAsteroidLabels(boolean showAsteroidLabels);
    private native boolean c_getShowAsteroidLabels();
    public boolean getShowSpacecraftLabels() { return c_getShowSpacecraftLabels(); }
    public void setShowSpacecraftLabels(boolean showSpacecraftLabels) { c_setShowSpacecraftLabels(showSpacecraftLabels); }
    private native void c_setShowSpacecraftLabels(boolean showSpacecraftLabels);
    private native boolean c_getShowSpacecraftLabels();
    public boolean getShowLocationLabels() { return c_getShowLocationLabels(); }
    public void setShowLocationLabels(boolean showLocationLabels) { c_setShowLocationLabels(showLocationLabels); }
    private native void c_setShowLocationLabels(boolean showLocationLabels);
    private native boolean c_getShowLocationLabels();
    public boolean getShowCometLabels() { return c_getShowCometLabels(); }
    public void setShowCometLabels(boolean showCometLabels) { c_setShowCometLabels(showCometLabels); }
    private native void c_setShowCometLabels(boolean showCometLabels);
    private native boolean c_getShowCometLabels();
    public boolean getShowDwarfPlanetLabels() { return c_getShowDwarfPlanetLabels(); }
    public void setShowDwarfPlanetLabels(boolean showDwarfPlanetLabels) { c_setShowDwarfPlanetLabels(showDwarfPlanetLabels); }
    private native void c_setShowDwarfPlanetLabels(boolean showDwarfPlanetLabels);
    private native boolean c_getShowDwarfPlanetLabels();
    public boolean getShowMinorMoonLabels() { return c_getShowMinorMoonLabels(); }
    public void setShowMinorMoonLabels(boolean showMinorMoonLabels) { c_setShowMinorMoonLabels(showMinorMoonLabels); }
    private native void c_setShowMinorMoonLabels(boolean showMinorMoonLabels);
    private native boolean c_getShowMinorMoonLabels();

    // ShowLatinConstellationLabels (UI) is the opposite value of ShowI18nConstellationLabels
    public boolean getShowLatinConstellationLabels() { return !c_getShowI18nConstellationLabels(); }
    public void setShowLatinConstellationLabels(boolean showLatinConstellationLabels) { c_setShowI18nConstellationLabels(!showLatinConstellationLabels); }
    private native void c_setShowI18nConstellationLabels(boolean showI18nConstellationLabels);
    private native boolean c_getShowI18nConstellationLabels();

    public boolean getShowPlanetOrbits() { return c_getShowPlanetOrbits(); }
    public void setShowPlanetOrbits(boolean showPlanetOrbits) { c_setShowPlanetOrbits(showPlanetOrbits); }
    private native void c_setShowPlanetOrbits(boolean showPlanetOrbits);
    private native boolean c_getShowPlanetOrbits();
    public boolean getShowMoonOrbits() { return c_getShowMoonOrbits(); }
    public void setShowMoonOrbits(boolean showMoonOrbits) { c_setShowMoonOrbits(showMoonOrbits); }
    private native void c_setShowMoonOrbits(boolean showMoonOrbits);
    private native boolean c_getShowMoonOrbits();
    public boolean getShowAsteroidOrbits() { return c_getShowAsteroidOrbits(); }
    public void setShowAsteroidOrbits(boolean showAsteroidOrbits) { c_setShowAsteroidOrbits(showAsteroidOrbits); }
    private native void c_setShowAsteroidOrbits(boolean showAsteroidOrbits);
    private native boolean c_getShowAsteroidOrbits();
    public boolean getShowSpacecraftOrbits() { return c_getShowSpacecraftOrbits(); }
    public void setShowSpacecraftOrbits(boolean showSpacecraftOrbits) { c_setShowSpacecraftOrbits(showSpacecraftOrbits); }
    private native void c_setShowSpacecraftOrbits(boolean showSpacecraftOrbits);
    private native boolean c_getShowSpacecraftOrbits();
    public boolean getShowCometOrbits() { return c_getShowCometOrbits(); }
    public void setShowCometOrbits(boolean showCometOrbits) { c_setShowCometOrbits(showCometOrbits); }
    private native void c_setShowCometOrbits(boolean showCometOrbits);
    private native boolean c_getShowCometOrbits();
    public boolean getShowStellarOrbits() { return c_getShowStellarOrbits(); }
    public void setShowStellarOrbits(boolean showStellarOrbits) { c_setShowStellarOrbits(showStellarOrbits); }
    private native void c_setShowStellarOrbits(boolean showStellarOrbits);
    private native boolean c_getShowStellarOrbits();
    public boolean getShowDwarfPlanetOrbits() { return c_getShowDwarfPlanetOrbits(); }
    public void setShowDwarfPlanetOrbits(boolean showDwarfPlanetOrbits) { c_setShowDwarfPlanetOrbits(showDwarfPlanetOrbits); }
    private native void c_setShowDwarfPlanetOrbits(boolean showDwarfPlanetOrbits);
    private native boolean c_getShowDwarfPlanetOrbits();
    public boolean getShowMinorMoonOrbits() { return c_getShowMinorMoonOrbits(); }
    public void setShowMinorMoonOrbits(boolean showMinorMoonOrbits) { c_setShowMinorMoonOrbits(showMinorMoonOrbits); }
    private native void c_setShowMinorMoonOrbits(boolean showMinorMoonOrbits);
    private native boolean c_getShowMinorMoonOrbits();

    public boolean getShowCityLabels() { return c_getShowCityLabels(); }
    public void setShowCityLabels(boolean showCityLabels) { c_setShowCityLabels(showCityLabels); }
    private native void c_setShowCityLabels(boolean showCityLabels);
    private native boolean c_getShowCityLabels();
    public boolean getShowObservatoryLabels() { return c_getShowObservatoryLabels(); }
    public void setShowObservatoryLabels(boolean showObservatoryLabels) { c_setShowObservatoryLabels(showObservatoryLabels); }
    private native void c_setShowObservatoryLabels(boolean showObservatoryLabels);
    private native boolean c_getShowObservatoryLabels();
    public boolean getShowLandingSiteLabels() { return c_getShowLandingSiteLabels(); }
    public void setShowLandingSiteLabels(boolean showLandingSiteLabels) { c_setShowLandingSiteLabels(showLandingSiteLabels); }
    private native void c_setShowLandingSiteLabels(boolean showLandingSiteLabels);
    private native boolean c_getShowLandingSiteLabels();
    public boolean getShowCraterLabels() { return c_getShowCraterLabels(); }
    public void setShowCraterLabels(boolean showCraterLabels) { c_setShowCraterLabels(showCraterLabels); }
    private native void c_setShowCraterLabels(boolean showCraterLabels);
    private native boolean c_getShowCraterLabels();
    public boolean getShowVallisLabels() { return c_getShowVallisLabels(); }
    public void setShowVallisLabels(boolean showVallisLabels) { c_setShowVallisLabels(showVallisLabels); }
    private native void c_setShowVallisLabels(boolean showVallisLabels);
    private native boolean c_getShowVallisLabels();
    public boolean getShowMonsLabels() { return c_getShowMonsLabels(); }
    public void setShowMonsLabels(boolean showMonsLabels) { c_setShowMonsLabels(showMonsLabels); }
    private native void c_setShowMonsLabels(boolean showMonsLabels);
    private native boolean c_getShowMonsLabels();
    public boolean getShowPlanumLabels() { return c_getShowPlanumLabels(); }
    public void setShowPlanumLabels(boolean showPlanumLabels) { c_setShowPlanumLabels(showPlanumLabels); }
    private native void c_setShowPlanumLabels(boolean showPlanumLabels);
    private native boolean c_getShowPlanumLabels();
    public boolean getShowChasmaLabels() { return c_getShowChasmaLabels(); }
    public void setShowChasmaLabels(boolean showChasmaLabels) { c_setShowChasmaLabels(showChasmaLabels); }
    private native void c_setShowChasmaLabels(boolean showChasmaLabels);
    private native boolean c_getShowChasmaLabels();
    public boolean getShowPateraLabels() { return c_getShowPateraLabels(); }
    public void setShowPateraLabels(boolean showPateraLabels) { c_setShowPateraLabels(showPateraLabels); }
    private native void c_setShowPateraLabels(boolean showPateraLabels);
    private native boolean c_getShowPateraLabels();
    public boolean getShowMareLabels() { return c_getShowMareLabels(); }
    public void setShowMareLabels(boolean showMareLabels) { c_setShowMareLabels(showMareLabels); }
    private native void c_setShowMareLabels(boolean showMareLabels);
    private native boolean c_getShowMareLabels();
    public boolean getShowRupesLabels() { return c_getShowRupesLabels(); }
    public void setShowRupesLabels(boolean showRupesLabels) { c_setShowRupesLabels(showRupesLabels); }
    private native void c_setShowRupesLabels(boolean showRupesLabels);
    private native boolean c_getShowRupesLabels();
    public boolean getShowTesseraLabels() { return c_getShowTesseraLabels(); }
    public void setShowTesseraLabels(boolean showTesseraLabels) { c_setShowTesseraLabels(showTesseraLabels); }
    private native void c_setShowTesseraLabels(boolean showTesseraLabels);
    private native boolean c_getShowTesseraLabels();
    public boolean getShowRegioLabels() { return c_getShowRegioLabels(); }
    public void setShowRegioLabels(boolean showRegioLabels) { c_setShowRegioLabels(showRegioLabels); }
    private native void c_setShowRegioLabels(boolean showRegioLabels);
    private native boolean c_getShowRegioLabels();
    public boolean getShowChaosLabels() { return c_getShowChaosLabels(); }
    public void setShowChaosLabels(boolean showChaosLabels) { c_setShowChaosLabels(showChaosLabels); }
    private native void c_setShowChaosLabels(boolean showChaosLabels);
    private native boolean c_getShowChaosLabels();
    public boolean getShowTerraLabels() { return c_getShowTerraLabels(); }
    public void setShowTerraLabels(boolean showTerraLabels) { c_setShowTerraLabels(showTerraLabels); }
    private native void c_setShowTerraLabels(boolean showTerraLabels);
    private native boolean c_getShowTerraLabels();
    public boolean getShowAstrumLabels() { return c_getShowAstrumLabels(); }
    public void setShowAstrumLabels(boolean showAstrumLabels) { c_setShowAstrumLabels(showAstrumLabels); }
    private native void c_setShowAstrumLabels(boolean showAstrumLabels);
    private native boolean c_getShowAstrumLabels();
    public boolean getShowCoronaLabels() { return c_getShowCoronaLabels(); }
    public void setShowCoronaLabels(boolean showCoronaLabels) { c_setShowCoronaLabels(showCoronaLabels); }
    private native void c_setShowCoronaLabels(boolean showCoronaLabels);
    private native boolean c_getShowCoronaLabels();
    public boolean getShowDorsumLabels() { return c_getShowDorsumLabels(); }
    public void setShowDorsumLabels(boolean showDorsumLabels) { c_setShowDorsumLabels(showDorsumLabels); }
    private native void c_setShowDorsumLabels(boolean showDorsumLabels);
    private native boolean c_getShowDorsumLabels();
    public boolean getShowFossaLabels() { return c_getShowFossaLabels(); }
    public void setShowFossaLabels(boolean showFossaLabels) { c_setShowFossaLabels(showFossaLabels); }
    private native void c_setShowFossaLabels(boolean showFossaLabels);
    private native boolean c_getShowFossaLabels();
    public boolean getShowCatenaLabels() { return c_getShowCatenaLabels(); }
    public void setShowCatenaLabels(boolean showCatenaLabels) { c_setShowCatenaLabels(showCatenaLabels); }
    private native void c_setShowCatenaLabels(boolean showCatenaLabels);
    private native boolean c_getShowCatenaLabels();
    public boolean getShowMensaLabels() { return c_getShowMensaLabels(); }
    public void setShowMensaLabels(boolean showMensaLabels) { c_setShowMensaLabels(showMensaLabels); }
    private native void c_setShowMensaLabels(boolean showMensaLabels);
    private native boolean c_getShowMensaLabels();
    public boolean getShowRimaLabels() { return c_getShowRimaLabels(); }
    public void setShowRimaLabels(boolean showRimaLabels) { c_setShowRimaLabels(showRimaLabels); }
    private native void c_setShowRimaLabels(boolean showRimaLabels);
    private native boolean c_getShowRimaLabels();
    public boolean getShowUndaeLabels() { return c_getShowUndaeLabels(); }
    public void setShowUndaeLabels(boolean showUndaeLabels) { c_setShowUndaeLabels(showUndaeLabels); }
    private native void c_setShowUndaeLabels(boolean showUndaeLabels);
    private native boolean c_getShowUndaeLabels();
    public boolean getShowReticulumLabels() { return c_getShowReticulumLabels(); }
    public void setShowReticulumLabels(boolean showReticulumLabels) { c_setShowReticulumLabels(showReticulumLabels); }
    private native void c_setShowReticulumLabels(boolean showReticulumLabels);
    private native boolean c_getShowReticulumLabels();
    public boolean getShowPlanitiaLabels() { return c_getShowPlanitiaLabels(); }
    public void setShowPlanitiaLabels(boolean showPlanitiaLabels) { c_setShowPlanitiaLabels(showPlanitiaLabels); }
    private native void c_setShowPlanitiaLabels(boolean showPlanitiaLabels);
    private native boolean c_getShowPlanitiaLabels();
    public boolean getShowLineaLabels() { return c_getShowLineaLabels(); }
    public void setShowLineaLabels(boolean showLineaLabels) { c_setShowLineaLabels(showLineaLabels); }
    private native void c_setShowLineaLabels(boolean showLineaLabels);
    private native boolean c_getShowLineaLabels();
    public boolean getShowFluctusLabels() { return c_getShowFluctusLabels(); }
    public void setShowFluctusLabels(boolean showFluctusLabels) { c_setShowFluctusLabels(showFluctusLabels); }
    private native void c_setShowFluctusLabels(boolean showFluctusLabels);
    private native boolean c_getShowFluctusLabels();
    public boolean getShowFarrumLabels() { return c_getShowFarrumLabels(); }
    public void setShowFarrumLabels(boolean showFarrumLabels) { c_setShowFarrumLabels(showFarrumLabels); }
    private native void c_setShowFarrumLabels(boolean showFarrumLabels);
    private native boolean c_getShowFarrumLabels();
    public boolean getShowEruptiveCenterLabels() { return c_getShowEruptiveCenterLabels(); }
    public void setShowEruptiveCenterLabels(boolean showEruptiveCenterLabels) { c_setShowEruptiveCenterLabels(showEruptiveCenterLabels); }
    private native void c_setShowEruptiveCenterLabels(boolean showEruptiveCenterLabels);
    private native boolean c_getShowEruptiveCenterLabels();
    public boolean getShowOtherLabels() { return c_getShowOtherLabels(); }
    public void setShowOtherLabels(boolean showOtherLabels) { c_setShowOtherLabels(showOtherLabels); }
    private native void c_setShowOtherLabels(boolean showOtherLabels);
    private native boolean c_getShowOtherLabels();

    public void setResolution(int resolution) { c_setResolution(resolution); }
    public int getResolution() { return c_getResolution(); }
    private native void c_setResolution(int resolution);
    private native int c_getResolution();
    public void setStarStyle(int starStyle) { c_setStarStyle(starStyle);}
    public int getStarStyle() { return c_getStarStyle(); }
    private native void c_setStarStyle(int starStyle);
    private native int c_getStarStyle();
    public void setHudDetail(int hudDetail) { c_setHudDetail(hudDetail); }
    public int getHudDetail() { return c_getHudDetail(); }
    private native void c_setHudDetail(int hudDetail);
    private native int c_getHudDetail();
    public void setTimeZone(int timeZone) { c_setTimeZone(timeZone); }
    public int getTimeZone() { return c_getTimeZone(); }
    private native void c_setTimeZone(int TimeZone);
    private native int c_getTimeZone();
    public void setDateFormat(int dateFormat) { c_setDateFormat(dateFormat); }
    public int getDateFormat() { return c_getDateFormat(); }
    private native void c_setDateFormat(int DateFormat);
    private native int c_getDateFormat();

    public void setAmbientLightLevel(double ambientLightLevel) { c_setAmbientLightLevel(ambientLightLevel); }
    public double getAmbientLightLevel() { return c_getAmbientLightLevel(); }
    private native void c_setAmbientLightLevel(double ambientLightLevel);
    private native double c_getAmbientLightLevel();

    public void setFaintestVisible(double faintestVisible) { c_setFaintestVisible(faintestVisible); }
    public double getFaintestVisible() { return c_getFaintestVisible(); }
    private native void c_setFaintestVisible(double faintestVisible);
    private native double c_getFaintestVisible();

    public void setGalaxyBrightness(double galaxyBrightness) { c_setGalaxyBrightness(galaxyBrightness); }
    public double getGalaxyBrightness() { return c_getGalaxyBrightness(); }
    private native void c_setGalaxyBrightness(double galaxyBrightness);
    private native double c_getGalaxyBrightness();

    public void setMinimumFeatureSize(double minimumFeatureSize) { c_setMinimumFeatureSize(minimumFeatureSize); }
    public double getMinimumFeatureSize() { return c_geMinimumFeatureSize(); }
    private native void c_setMinimumFeatureSize(double minimumFeatureSize);
    private native double c_geMinimumFeatureSize();

    public boolean getShowBodyAxes() { return c_getReferenceMarkEnabled(pointer, "body axes"); }
    public void setShowBodyAxes(boolean value) { c_toggleReferenceMarkEnabled(pointer, "body axes"); }

    public boolean getShowFrameAxes() { return c_getReferenceMarkEnabled(pointer, "frame axes"); }
    public void setShowFrameAxes(boolean value) { c_toggleReferenceMarkEnabled(pointer, "frame axes"); }

    public boolean getShowSunDirection() { return c_getReferenceMarkEnabled(pointer, "sun direction"); }
    public void setShowSunDirection(boolean value) { c_toggleReferenceMarkEnabled(pointer, "sun direction"); }

    public boolean getShowVelocityVector() { return c_getReferenceMarkEnabled(pointer, "velocity vector"); }
    public void setShowVelocityVector(boolean value) { c_toggleReferenceMarkEnabled(pointer, "velocity vector"); }

    public boolean getShowPlanetographicGrid() { return c_getReferenceMarkEnabled(pointer, "planetographic grid"); }
    public void setShowPlanetographicGrid(boolean value) { c_toggleReferenceMarkEnabled(pointer, "planetographic grid"); }

    public boolean getShowTerminator() { return c_getReferenceMarkEnabled(pointer, "terminator"); }
    public void setShowTerminator(boolean value) { c_toggleReferenceMarkEnabled(pointer, "terminator"); }

    private static native boolean c_getReferenceMarkEnabled(long ptr, String str);
    private static native void c_toggleReferenceMarkEnabled(long ptr, String str);
}
