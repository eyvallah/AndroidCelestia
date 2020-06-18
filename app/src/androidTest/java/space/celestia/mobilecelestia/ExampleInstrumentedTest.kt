/*
 * ExampleInstrumentedTest.kt
 *
 * Copyright (C) 2001-2020, Celestia Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package space.celestia.mobilecelestia

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.screenshot.Screenshot
import com.microsoft.appcenter.utils.InstrumentationRegistryHelper
import org.hamcrest.CoreMatchers.endsWith
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import java.io.File
import java.lang.Exception


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    val urlsToTest: List<String>
        get() = listOf(
            "cel://Follow/Sol:Earth/2021-05-06T18:40:40.68139?x=oGpZe9TTZf7//////////w&y=SOKEeQjPoQ&z=wBS53hQ+2f3//////////w&ow=0.394195&ox=0.283234&oy=0.874075&oz=0.0195564&fov=37.4191&ts=1000&ltd=0&p=0&rf=17047014295&lm=2048&tsrc=0&ver=4",
            "cel://Follow/Sol/2020-07-19T07:29:31.17147?x=AACQWD7LD6A6&y=AACAt0m3jGDIAw&z=AABgwnq62JhG&ow=0.708013&ox=0.699494&oy=-0.0742514&oz=-0.0625495&select=HIP%2026266&fov=12.0826&ts=100000&ltd=0&p=0&rf=17047014327&lm=0&tsrc=0&ver=4",
            "cel://Follow/Sol:Earth:Skylab/1990-09-16T15:16:58.76543?x=WuJRNQ0&y=5poxWB8&z=w63iiAE&ow=0.681278&ox=0.689827&oy=-0.0400184&oz=0.241654&select=Sol:Earth&fov=13.5796&ts=1&ltd=0&p=0&rf=17049116599&lm=0&tsrc=0&ver=4",
            "cel://Follow/Milky%20Way/2020-05-22T15:42:17.94729?x=AAAAAAAAugHuPt/NOw&y=AAAAAAAAPFilR+aMjP///w&z=AAAAAAAAdkq+zSB7Fg&ow=0.723188&ox=-0.450126&oy=-0.455226&oz=0.259144&select=Milky%20Way&fov=13.5796&ts=1&ltd=0&p=0&rf=17047014295&lm=0&tsrc=0&ver=4"
        )

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java, false, false)

    @Test
    fun testAll() {
        testInfoView()
        testURLs()
    }

    fun testURLs() {
        for (i in 0 until urlsToTest.size) {
            screenshotURL(urlsToTest[i], "URL$i")
        }
    }

    fun testInfoView() {
        startActivity(null)

        onView(withContentDescription("Info")).perform(click())

        Thread.sleep(5000)

        val capture = Screenshot.capture()
        capture.setFormat(Bitmap.CompressFormat.PNG)
        capture.setName("Info")
        capture.process()

        Thread.sleep(1000)

        onView(withContentDescription("Close")).perform(click())

        Thread.sleep(1000)
    }

    fun startActivity(intent: Intent?) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val activity = activityRule.activity
        if (activity != null) {
            context.startActivity(intent)
            Thread.sleep(1000)
        } else {
            activityRule.launchActivity(intent)
            Thread.sleep(40000)
        }
    }

    fun screenshotURL(url: String, name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse( url)).setPackage(context.packageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        startActivity(intent)

        onView(withText("Open URL?")).check(matches(isDisplayed()))
        onView(withId(android.R.id.button1)).perform(click())
        onView(withContentDescription("Hide")).perform(click())

        Thread.sleep(5000)

        val capture = Screenshot.capture()
        capture.setFormat(Bitmap.CompressFormat.PNG)
        capture.setName(name)
        capture.process()

        Thread.sleep(1000)
    }
}
