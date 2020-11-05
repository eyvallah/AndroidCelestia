/*
 * CelestiaScript.cpp
 *
 * Copyright (C) 2001-2020, Celestia Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

#include "CelestiaJNI.h"
#import <celestia/scriptmenu.h>

extern "C"
JNIEXPORT jobject JNICALL
Java_space_celestia_mobilecelestia_core_CelestiaScript_c_1getScriptsInDirectory(JNIEnv *env,
                                                                                jclass clazz,
                                                                                jstring path,
                                                                                jboolean deep_scan) {
    const char *str = env->GetStringUTFChars(path, nullptr);
    std::vector<ScriptMenuItem> *results = ScanScriptsDirectory(str, deep_scan != JNI_FALSE);
    env->ReleaseStringUTFChars(path, str);

    jobject arrayObj = env->NewObject(alClz, aliMethodID, (int)results->size());

    for (const auto& result : *results) {
        jobject jscript = env->NewObject(cscriptClz, cscriptiMethodID,
                                         env->NewStringUTF(result.filename.c_str()),
                                         env->NewStringUTF(result.title.c_str())
        );
        env->CallBooleanMethod(arrayObj, alaMethodID, jscript);
    }

    delete results;
    return arrayObj;
}