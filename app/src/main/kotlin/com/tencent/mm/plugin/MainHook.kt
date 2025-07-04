package com.tencent.mm.plugin

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
                        wechatHook(context.classLoader)
                    }
                }
            )
            wechatHook(lpparam.classLoader)
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

    private val sharedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sharedRectF = RectF()

    fun createRoundedBitmap(bitmap: Bitmap, cornerRatio: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val cornerRadius = width.coerceAtMost(height) * cornerRatio

        sharedPaint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        sharedRectF.set(0f, 0f, width.toFloat(), height.toFloat())

        val roundedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(roundedBitmap)

        canvas.drawRoundRect(sharedRectF, cornerRadius, cornerRadius, sharedPaint)

        return roundedBitmap
    }

}
