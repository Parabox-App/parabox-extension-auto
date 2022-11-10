package com.ojhdtapp.parabox.extension.auto.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.ojhdtapp.parabox.extension.auto.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


object FileUtil {

    fun getAppIcon(context: Context, packageName: String): Bitmap? {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            } else if (drawable is AdaptiveIconDrawable) {
                val drr = arrayOfNulls<Drawable>(2)
                drr[0] = drawable.background
                drr[1] = drawable.foreground
                val layerDrawable = LayerDrawable(drr)
                val width = layerDrawable.intrinsicWidth
                val height = layerDrawable.intrinsicHeight
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                layerDrawable.setBounds(0, 0, canvas.width, canvas.height)
                layerDrawable.draw(canvas)
                return bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getSmallIcon(context: Context, pkgName: String?, id: Int): Bitmap? {
        var smallIcon: Bitmap? = null
        val remotePkgContext: Context
        try {
            remotePkgContext = context.createPackageContext(pkgName, 0)
            val drawable = remotePkgContext.resources.getDrawable(id)
            if (drawable != null) {
                smallIcon = (drawable as BitmapDrawable).bitmap
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return smallIcon
    }

    fun getUriFromBitmap(context: Context, bm: Bitmap, name: String): Uri? {
        val targetDir = File(context.externalCacheDir, "bm")
        if (!targetDir.exists()) targetDir.mkdirs()
        val tempFile =
            File(targetDir, "temp_${name}.png")
        val bytes = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val bitmapData = bytes.toByteArray()

        val fileOutPut = FileOutputStream(tempFile)
        fileOutPut.write(bitmapData)
        fileOutPut.flush()
        fileOutPut.close()
        return getUriOfFile(context, tempFile)
    }

    fun getUriOfFile(context: Context, file: File): Uri? {
        return try {
            FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider", file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun Long.toFormattedDate(): String {
        return SimpleDateFormat("M'月'd'日'", Locale.getDefault()).format(Date(this))
    }

    fun Long.toDateAndTimeString(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-",
            Locale.getDefault()
        ).format(Date(this)) + this.toString().substring(11)
    }

    fun Long.toMSString(): String{
        val df = DecimalFormat("#").apply {
            roundingMode = RoundingMode.DOWN
        }
        val totalSecond = (this / 1000).toFloat().roundToInt()
        val minute = df.format(totalSecond / 60)
        val second = totalSecond % 60
        return "${if(totalSecond > 60) minute.plus("′") else ""}${second}“"
    }
}