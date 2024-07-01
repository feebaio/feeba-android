/**
 * Modified MIT License
 *
 * Copyright 2017 OneSignal
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * 1. The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * 2. All copies of substantial portions of the Software may only be used in connection
 * with services provided by OneSignal.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.feeba

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.Keep
import androidx.core.app.NotificationManagerCompat
import androidx.legacy.content.WakefulBroadcastReceiver
import io.feeba.data.RuleSet
import io.feeba.data.RuleType
import io.feeba.lifecycle.LogLevel
import io.feeba.lifecycle.Logger
import java.util.regex.Pattern


object Utils {
    enum class SchemaType(private val text: String) {
        DATA("data"), HTTPS("https"), HTTP("http");

        companion object {
            fun fromString(text: String?): SchemaType? {
                for (type in values()) {
                    if (type.text.equals(text, ignoreCase = true)) {
                        return type
                    }
                }
                return null
            }
        }
    }


//    val netType: Int?
//        get() {
//            val cm = FeebaFacade.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//            val netInfo = cm.activeNetworkInfo
//            if (netInfo != null) {
//                val networkType = netInfo.type
//                return if (networkType == ConnectivityManager.TYPE_WIFI || networkType == ConnectivityManager.TYPE_ETHERNET) 0 else 1
//            }
//            return null
//        }
//    val carrierName: String?
//        get() = try {
//            val manager = FeebaFacade.appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//            // May throw even though it's not in noted in the Android docs.
//            // Issue #427
//            val carrierName = manager.networkOperatorName
//            if ("" == carrierName) null else carrierName
//        } catch (t: Throwable) {
//            t.printStackTrace()
//            null
//        }


    // Interim method that works around Proguard's overly aggressive assumenosideeffects which
    // ignores keep rules.
    // This is specifically designed to address Proguard removing catches for NoClassDefFoundError
    // when the config has "-assumenosideeffects" with
    // java.lang.Class.getName() & java.lang.Object.getClass().
    // This @Keep annotation is key so this method does not get removed / inlined.
    // Addresses issue https://github.com/OneSignal/OneSignal-Android-SDK/issues/1423
    @Keep
    private fun opaqueHasClass(_class: Class<*>): Boolean {
        return true
    }

    private fun hasWakefulBroadcastReceiver(): Boolean {
        return try {
            // noinspection ConstantConditions
            WakefulBroadcastReceiver::class.java != null
        } catch (e: Throwable) {
            false
        }
    }

    private fun hasNotificationManagerCompat(): Boolean {
        return try {
            // noinspection ConstantConditions
            NotificationManagerCompat::class.java != null
        } catch (e: Throwable) {
            false
        }
    }

//    private fun hasJobIntentService(): Boolean {
//        return try {
//            // noinspection ConstantConditions
//            JobIntentService::class.java != null
//        } catch (e: Throwable) {
//            false
//        }
//    }
//
//    private fun packageInstalledAndEnabled(packageName: String): Boolean {
//        return try {
//            val pm: PackageManager = FeebaFacade.appContext.getPackageManager()
//            val info = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA)
//            info.applicationInfo.enabled
//        } catch (e: PackageManager.NameNotFoundException) {
//            false
//        }
//    }

    fun getManifestMetaBundle(context: Context): Bundle? {
        val ai: ApplicationInfo
        try {
            ai = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            return ai.metaData
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.log(LogLevel.ERROR, "Manifest application info not found", e)
        }
        return null
    }

    fun getManifestMetaBoolean(context: Context, metaName: String?): Boolean {
        val bundle = getManifestMetaBundle(context)
        return bundle?.getBoolean(metaName) ?: false
    }

    fun getManifestMeta(context: Context, metaName: String?): String? {
        val bundle = getManifestMetaBundle(context)
        return bundle?.getString(metaName)
    }

    fun getResourceString(context: Context, key: String?, defaultStr: String): String {
        val resources = context.resources
        val bodyResId = resources.getIdentifier(key, "string", context.packageName)
        return if (bodyResId != 0) resources.getString(bodyResId) else defaultStr
    }

    fun isValidEmail(email: String?): Boolean {
        if (email == null) return false
        val emRegex = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$"
        val pattern = Pattern.compile(emRegex)
        return pattern.matcher(email).matches()
    }

    fun isStringNotEmpty(body: String?): Boolean {
        return !TextUtils.isEmpty(body)
    }

    // Get the app's permission which will be false if the user disabled notifications for the app
    //   from Settings > Apps or by long pressing the notifications and selecting block.
    //   - Detection works on Android 4.4+, requires Android Support v4 Library 24.0.0+
//    fun areNotificationsEnabled(context: Context?): Boolean {
//        try {
//            return NotificationManagerCompat.from(FeebaFacade.appContext).areNotificationsEnabled()
//        } catch (t: Throwable) {
//        }
//        return true
//    }
    fun runOnMainUIThread(runnable: Runnable) {
        if (Looper.getMainLooper().thread === Thread.currentThread()) runnable.run() else {
            val handler = Handler(Looper.getMainLooper())
            handler.post(runnable)
        }
    }


    fun isValidResourceName(name: String?): Boolean {
        return name != null && !name.matches("^[0-9]".toRegex())
    }
}

val Context.navigationBarHeight: Int
    get() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return if (Build.VERSION.SDK_INT >= 30) {
            windowManager
                .currentWindowMetrics
                .windowInsets
                .getInsets(WindowInsets.Type.navigationBars())
                .bottom

        } else {
            val currentDisplay = try {
                display
            } catch (e: NoSuchMethodError) {
                windowManager.defaultDisplay
            }

            val appUsableSize = Point()
            val realScreenSize = Point()
            currentDisplay?.apply {
                getSize(appUsableSize)
                getRealSize(realScreenSize)
            }

            // navigation bar on the side
            if (appUsableSize.x < realScreenSize.x) {
                return realScreenSize.x - appUsableSize.x
            }

            // navigation bar at the bottom
            return if (appUsableSize.y < realScreenSize.y) {
                realScreenSize.y - appUsableSize.y
            } else 0
        }
    }

val Activity.statusBarHeight: Int
    get() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= 30) {
            windowManager
                .currentWindowMetrics
                .windowInsets
                .getInsets(WindowInsets.Type.statusBars())
                .top
        } else {
            val rectangle = Rect()
            window.decorView.getWindowVisibleDisplayFrame(rectangle)
            val statusBarHeight = rectangle.top
            val contentViewTop = window.findViewById<View>(Window.ID_ANDROID_CONTENT).top
            val titleBarHeight = contentViewTop - statusBarHeight
            statusBarHeight
        }
    }

fun RuleSet.getSurveyDelaySec(): Long {
    return triggers.filter { it.type == RuleType.SESSION_DURATION }.getOrNull(0)?.value?.toLongOrNull() ?: 0
}

public fun appendQueryParameter(url: String, keyValues: Array<Pair<String, String>>): String {
    return try {
        val uri = Uri.parse(url)
        val builder = uri.buildUpon()
        keyValues.forEach { (key, value) -> builder.appendQueryParameter(key, value) }
        builder.build().toString()
    } catch (e: Exception) {
        Logger.log(LogLevel.ERROR, "Failed to append query parameter to url: $url, ${Log.getStackTraceString(e)}")
        url
    }
}

fun getSanitizedWidthPercent(width: Int): Int {
    return if (width in 1..100) width else 90
}

fun getSanitizedHeightPercent(height: Int): Int {
    return if (height in 1..100) height else 70
}