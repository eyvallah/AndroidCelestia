/*
 * ResourceAPI.kt
 *
 * Copyright (C) 2001-2020, Celestia Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package space.celestia.mobilecelestia.resource.model

import com.google.gson.*
import io.reactivex.rxjava3.core.Observable
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import space.celestia.mobilecelestia.utils.BaseResult
import java.lang.reflect.Type
import java.util.*

object ResourceAPI {
    class DateAdapter : JsonDeserializer<Date> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): Date {
            try {
                val seconds = json?.asDouble ?: return Date()
                return Date((seconds * 1000.0).toLong())
            } catch(e: Throwable) {
                throw JsonParseException(e)
            }
        }
    }

    val shared: Retrofit = Retrofit.Builder()
        .baseUrl("https://celestia.mobi/api/resource/")
        .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val gson by lazy { GsonBuilder().registerTypeAdapter(Date::class.java, DateAdapter()).create() }
}

interface ResourceAPIService {
    @GET("categories")
    fun categories(
        @Query("lang") lang: String
    ): Observable<BaseResult>

    @GET("items")
    fun items(
        @Query("lang") lang: String,
        @Query("category") category: String
    ): Observable<BaseResult>

    @GET("item")
    fun item(
        @Query("lang") lang: String,
        @Query("item") item: String
    ): Observable<BaseResult>
}
