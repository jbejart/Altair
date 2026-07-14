package com.altair.app

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object OsiptelDb {

    private const val DATABASE_NAME = "osiptel.db"

    fun ensureDb(context: Context): File {
        val outputFile = File(context.filesDir, DATABASE_NAME)

        if (outputFile.exists() && outputFile.length() > 0) {
            return outputFile
        }

        context.assets.open(DATABASE_NAME).use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }

        return outputFile
    }
}