/*
 * CelestiaRenderer.cpp
 *
 * Copyright (C) 2001-2020, Celestia Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

#include <jni.h>
#include <pthread.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <epoxy/egl.h>
#include <celestia/celestiacore.h>

#include <android/log.h>

#define LOG_INFO(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOG_ERROR(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define LOG_TAG "Renderer"

class CelestiaRenderer
{
public:
    CelestiaRenderer() = default;
    ~CelestiaRenderer() = default;

    bool initialize();
    void destroy();
    inline void resizeIfNeeded();
    inline void tickAndDraw() const;
    void start();
    void stop();
    inline void lock();
    inline void unlock();
    void pause();
    void resume();
    inline void wait();
    void setSurface(JNIEnv *env, jobject surface);
    void setSize(int width, int height);
    void setCorePointer(CelestiaCore *core);

    jobject javaObject = nullptr;

    CelestiaCore *core = nullptr;

    enum RenderThreadMessage {
        MSG_NONE = 0,
        MSG_WINDOW_SET,
        MSG_RENDER_LOOP_EXIT
    };

    enum RenderThreadMessage msg = MSG_NONE;

    bool enableMultisample = false;
    bool engineStartedCalled = false;

    EGLDisplay display = EGL_NO_DISPLAY;
    EGLSurface surface = EGL_NO_SURFACE;
    EGLContext context = EGL_NO_CONTEXT;
    EGLConfig config {};
    EGLint format {};

    static JavaVM *jvm;
    static jmethodID flushTasksMethod;
    static jmethodID engineStartedMethod;

private:
    bool suspendedFlag = false;
    pthread_t threadId {};
    pthread_mutex_t msgMutex {};
    pthread_cond_t resumeCond {};

    int windowWidth = 0;
    int windowHeight = 0;
    int currentWindowWidth = 0;
    int currentWindowHeight = 0;

    ANativeWindow *window = nullptr;

    static void *threadCallback(void *self);
};

JavaVM *CelestiaRenderer::jvm = nullptr;
jmethodID CelestiaRenderer::flushTasksMethod = nullptr;
jmethodID CelestiaRenderer::engineStartedMethod = nullptr;

bool CelestiaRenderer::initialize()
{
    if (context == EGL_NO_CONTEXT)
    {
        const EGLint multisampleAttribs[] = {
                EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL_BLUE_SIZE, 8,
                EGL_GREEN_SIZE, 8,
                EGL_RED_SIZE, 8,
                EGL_DEPTH_SIZE, 16,
                EGL_SAMPLES, 4,
                EGL_SAMPLE_BUFFERS, 1,
                EGL_NONE
        };
        const EGLint attribs[] = {
                EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL_BLUE_SIZE, 8,
                EGL_GREEN_SIZE, 8,
                EGL_RED_SIZE, 8,
                EGL_DEPTH_SIZE, 16,
                EGL_NONE
        };

        EGLint numConfigs;
        LOG_INFO("Initializing context");

        if ((display = eglGetDisplay(EGL_DEFAULT_DISPLAY)) == EGL_NO_DISPLAY) {
            LOG_ERROR("eglGetDisplay() returned error %d", eglGetError());
            return false;
        }

        if (!eglInitialize(display, nullptr, nullptr)) {
            LOG_ERROR("eglInitialize() returned error %d", eglGetError());
            destroy();
            return false;
        }

        if (enableMultisample) {
            // Try to enable multisample but fallback if not available
            if (!eglChooseConfig(display, multisampleAttribs, &config, 1, &numConfigs) && !eglChooseConfig(display, attribs, &config, 1, &numConfigs)) {
                LOG_ERROR("eglChooseConfig() returned error %d", eglGetError());
                destroy();
                return false;
            }
        } else {
            if (!eglChooseConfig(display, attribs, &config, 1, &numConfigs)) {
                LOG_ERROR("eglChooseConfig() returned error %d", eglGetError());
                destroy();
                return false;
            }
        }

        if (!eglGetConfigAttrib(display, config, EGL_NATIVE_VISUAL_ID, &format)) {
            LOG_ERROR("eglGetConfigAttrib() returned error %d", eglGetError());
            destroy();
            return false;
        }

        const EGLint contextAttributes[] = {
                EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL_NONE
        };

        if (!(context = eglCreateContext(display, config, nullptr, contextAttributes))) {
            LOG_ERROR("eglCreateContext() returned error %d", eglGetError());
            destroy();
            return false;
        }
    }

    if (window) {
        ANativeWindow_setBuffersGeometry(window, 0, 0, format);
        if (!(surface = eglCreateWindowSurface(display, config, window, nullptr))) {
            LOG_ERROR("eglCreateWindowSurface() returned error %d", eglGetError());
            destroy();
            return false;
        }

        if (!eglMakeCurrent(display, surface, surface, context)) {
            LOG_ERROR("eglMakeCurrent() returned error %d", eglGetError());
            destroy();
            return false;
        }
    } else {
        eglDestroySurface(display, surface);
        surface = EGL_NO_SURFACE;
    }
    return true;
}

void CelestiaRenderer::destroy()
{
    LOG_INFO("Destroying context");

    if (context != EGL_NO_CONTEXT)
    {
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDestroyContext(display, context);
        if (surface != EGL_NO_SURFACE)
            eglDestroySurface(display, surface);
        eglTerminate(display);
    }
    display = EGL_NO_DISPLAY;
    surface = EGL_NO_SURFACE;
    context = EGL_NO_CONTEXT;
    currentWindowWidth = 0;
    currentWindowHeight = 0;
}

void CelestiaRenderer::resizeIfNeeded()
{
    if (currentWindowHeight != windowHeight || currentWindowWidth != windowWidth)
    {
        core->resize(windowWidth, windowHeight);
        currentWindowWidth = windowWidth;
        currentWindowHeight = windowHeight;
    }
}

void CelestiaRenderer::tickAndDraw() const
{
    core->tick();
    core->draw();
    if (!eglSwapBuffers(display, surface))
        LOG_ERROR("eglSwapBuffers() returned error %d", eglGetError());
}

void CelestiaRenderer::start()
{
    pthread_mutex_init(&msgMutex, nullptr);
    pthread_cond_init(&resumeCond, nullptr);
    pthread_create(&threadId, nullptr, threadCallback, this);
}

void CelestiaRenderer::stop()
{
    lock();
    msg = CelestiaRenderer::MSG_RENDER_LOOP_EXIT;
    unlock();

    pthread_join(threadId, nullptr);

    pthread_mutex_destroy(&msgMutex);
    pthread_cond_destroy(&resumeCond);
}

void CelestiaRenderer::lock()
{
    pthread_mutex_lock(&msgMutex);
}

void CelestiaRenderer::unlock()
{
    pthread_mutex_unlock(&msgMutex);
}

void CelestiaRenderer::pause()
{
    lock();
    suspendedFlag = true;
    unlock();
}

void CelestiaRenderer::resume()
{
    lock();
    suspendedFlag = false;
    pthread_cond_broadcast(&resumeCond);
    unlock();
}

void CelestiaRenderer::wait()
{
    while (suspendedFlag)
        pthread_cond_wait(&resumeCond, &msgMutex);
}

void CelestiaRenderer::setSurface(JNIEnv *env, jobject m_surface)
{
    lock();
    msg = CelestiaRenderer::MSG_WINDOW_SET;

    // Release current window
    if (window)
        ANativeWindow_release(window);

    if (m_surface)
        window = ANativeWindow_fromSurface(env, m_surface);
    else
        window = nullptr;
    unlock();
}

void CelestiaRenderer::setSize(int width, int height)
{
    lock();
    windowWidth = width;
    windowHeight = height;
    unlock();
}

void CelestiaRenderer::setCorePointer(CelestiaCore *m_core)
{
    lock();
    core = m_core;
    unlock();
}

void *CelestiaRenderer::threadCallback(void *self)
{
    auto renderer = (CelestiaRenderer *)self;

    // Create ENV for the native thread
    JNIEnv *newEnv;
    JavaVMAttachArgs args;
    args.version = JNI_VERSION_1_6;
    args.name = nullptr;
    args.group = nullptr;
    CelestiaRenderer::jvm->AttachCurrentThread(&newEnv, &args);

    bool renderingEnabled = true;

    while (renderingEnabled) {
        if (renderer->display != EGL_NO_DISPLAY && !renderer->engineStartedCalled)
        {
            newEnv->CallVoidMethod(renderer->javaObject, CelestiaRenderer::engineStartedMethod);
            renderer->engineStartedCalled = true;
        }
        if (renderer->engineStartedCalled)
            newEnv->CallVoidMethod(renderer->javaObject, CelestiaRenderer::flushTasksMethod);

        renderer->lock();
        renderer->wait();

        switch (renderer->msg)
        {
            case CelestiaRenderer::MSG_WINDOW_SET:
                renderer->initialize();
                break;
            case CelestiaRenderer::MSG_RENDER_LOOP_EXIT:
                renderingEnabled = false;
                renderer->destroy();
                break;
            default:
                break;
        }
        renderer->msg = CelestiaRenderer::MSG_NONE;

        if (renderer->engineStartedCalled && renderer->core) {
            renderer->resizeIfNeeded();
            renderer->tickAndDraw();
        }
        renderer->unlock();
    }

    // Detach
    CelestiaRenderer::jvm->DetachCurrentThread();
    pthread_exit(nullptr);
}

extern "C"
JNIEXPORT void JNICALL
Java_space_celestia_mobilecelestia_core_CelestiaRenderer_c_1initialize(JNIEnv *env, jobject thiz, jlong ptr) {
    auto renderer = (CelestiaRenderer *)ptr;
    renderer->javaObject = env->NewGlobalRef(thiz);
    jclass clazz = env->GetObjectClass(thiz);
    CelestiaRenderer::engineStartedMethod = env->GetMethodID(clazz, "engineStarted", "()V");
    CelestiaRenderer::flushTasksMethod = env->GetMethodID(clazz, "flushTasks", "()V");

    env->GetJavaVM(&CelestiaRenderer::jvm);
}

extern "C"
JNIEXPORT void JNICALL
Java_space_celestia_mobilecelestia_core_CelestiaRenderer_c_1deinitialize(JNIEnv *env, jobject thiz, jlong ptr) {

    auto renderer = (CelestiaRenderer *)ptr;
    env->DeleteGlobalRef(renderer->javaObject);
    delete renderer;
}

extern "C"
JNIEXPORT void JNICALL
Java_space_celestia_mobilecelestia_core_CelestiaRenderer_c_1start(JNIEnv *env, jobject thiz,
                                                                  jlong ptr,
                                                                  jobject activity,
                                                                  jboolean enable_multisample) {
    LOG_INFO("Creating renderer thread");

    auto renderer = (CelestiaRenderer *)ptr;
    renderer->enableMultisample = enable_multisample == JNI_TRUE;

    renderer->start();
}

extern "C"
JNIEXPORT void JNICALL
Java_space_celestia_mobilecelestia_core_CelestiaRenderer_c_1stop(JNIEnv *env, jobject thiz, jlong ptr) {
    auto renderer = (CelestiaRenderer *)ptr;

    LOG_INFO("Stopping renderer thread");
    renderer->stop();
    LOG_INFO("Renderer thread stopped");
}

extern "C"
JNIEXPORT void JNICALL
Java_space_celestia_mobilecelestia_core_CelestiaRenderer_c_1pause(JNIEnv *env, jobject thiz, jlong ptr) {
    auto renderer = (CelestiaRenderer *)ptr;

    renderer->pause();
}

extern "C"
JNIEXPORT void JNICALL
Java_space_celestia_mobilecelestia_core_CelestiaRenderer_c_1resume(JNIEnv *env, jobject thiz, jlong ptr) {
    auto renderer = (CelestiaRenderer *)ptr;

    renderer->resume();
}

extern "C"
JNIEXPORT void JNICALL
Java_space_celestia_mobilecelestia_core_CelestiaRenderer_c_1setSurface(JNIEnv *env, jobject thiz,
                                                                       jlong ptr,
                                                                       jobject surface) {
    auto renderer = (CelestiaRenderer *)ptr;

    renderer->setSurface(env, surface);
}

extern "C"
JNIEXPORT void JNICALL
Java_space_celestia_mobilecelestia_core_CelestiaRenderer_c_1setCorePointer(JNIEnv *env,
                                                                           jobject thiz,
                                                                           jlong ptr,
                                                                           jlong core_ptr) {
    auto renderer = (CelestiaRenderer *)ptr;

    renderer->setCorePointer((CelestiaCore *)core_ptr);
}

extern "C"
JNIEXPORT void JNICALL
Java_space_celestia_mobilecelestia_core_CelestiaRenderer_c_1setSurfaceSize(JNIEnv *env,
                                                                           jobject thiz,
                                                                           jlong ptr,
                                                                           jint width,
                                                                           jint height) {
    auto renderer = (CelestiaRenderer *)ptr;

    renderer->setSize(width, height);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_space_celestia_mobilecelestia_core_CelestiaRenderer_c_1createNativeRenderObject(JNIEnv *env,
                                                                                     jclass clazz) {
    return (jlong)new CelestiaRenderer;
}