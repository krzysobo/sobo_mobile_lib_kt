package com.krzysobo.sobomobilelib.service

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader


class AndroidFileHelper {
    // TODO TODO TODO - TESTING REQUIRED!!!
    fun copyResourceFileToCache(context: Context, rawResourceId: Int, tempFileName: String): File {
        // Create a temporary file in the app's cache directory
        val tempFile = File(context.cacheDir, tempFileName)

        // Read the raw resource and write to the temporary file
        context.resources.openRawResource(rawResourceId).use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    fun copyAssetFileToCache(context: Context, assetPath: String, tempFileName: String): File {
        val tempFile = File(context.cacheDir, tempFileName)
        context.assets.open(assetPath).use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }


    fun copyUriFileToCache(context: Context, uri: Uri, tempFileName: String): File {
        val tempFile = File(context.cacheDir, tempFileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }


    fun readFileFromContextFiles(context: Context, fileName: String) {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            FileReader(file).use { reader ->
//                println("content " + reader.read())
            }
        }
    }
}