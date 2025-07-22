package com.tencent.mm.plugin

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage


class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.tencent.mm") {
            XposedHelpers.findAndHookMethod(
                "android.app.Application",
                lpparam.classLoader,
                "attach",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.args[0] as Context
                        val processName = getProcessName(context)

                        // 只在主进程 Hook
                        if (processName == context.packageName) {
                            wechatHook(context.classLoader)
                        }
                    }
                }
            )
        }
    }

    fun getProcessName(context: Context): String {
        return try {
            val pid = android.os.Process.myPid()
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun wechatHook(classLoader: ClassLoader) {
        val bitmapUtil = classLoader.loadClass("com.tencent.mm.sdk.platformtools.x")
        XposedHelpers.findAndHookMethod(
            bitmapUtil, "o0",
            Bitmap::class.java,
            Boolean::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            IntArray::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val bitmap = param.result as Bitmap
                    param.result = createRoundedBitmap(bitmap, 0.2f)
                }
            })
    }

    fun createRoundedBitmap(bitmap: Bitmap, cornerRatio: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val safeCornerRatio = cornerRatio.coerceIn(0f, 1f)
        val cornerRadius = width.coerceAtMost(height) * safeCornerRatio

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        val rectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val roundedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(roundedBitmap)

        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)

        return roundedBitmap
    }

}
