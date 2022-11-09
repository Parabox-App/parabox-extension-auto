package com.ojhdtapp.parabox.extension.auto.core.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
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
    fun getUriFromBitmap(context: Context, bm: Bitmap): Uri? {
        val targetDir = File(context.externalCacheDir, "bm")
        if (!targetDir.exists()) targetDir.mkdirs()
        val tempFile =
            File(targetDir, "temp_${System.currentTimeMillis().toDateAndTimeString()}.png")
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