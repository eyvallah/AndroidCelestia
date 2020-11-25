/*
 * WebRequest.kt
 *
 * Copyright (C) 2001-2020, Celestia Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package space.celestia.mobilecelestia.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.lang.reflect.Type

class BaseResult(val status: Int, val info: Info) {
    class Info(val detail: String?, val reason: String?)
}

class ResultException internal constructor(val code: Int) : java.lang.Exception()

fun <T> BaseResult.commonHandler(t: Type, gson: Gson = GsonBuilder().create()): T {
    if (status != 0) throw ResultException(status)
    val detail = info.detail ?: throw ResultException(status)
    return gson.fromJson(detail, t)
}

fun <T> BaseResult.commonHandler(cls: Class<T>, gson: Gson = GsonBuilder().create()): T {
    if (status != 0) throw ResultException(status)
    val detail = info.detail ?: throw ResultException(status)
    @Suppress("UNCHECKED_CAST")
    if (cls == String::class.java) return cls.cast(detail) as T
    return gson.fromJson(detail, cls)
}