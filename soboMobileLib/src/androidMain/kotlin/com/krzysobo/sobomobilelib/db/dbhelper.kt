package com.krzysobo.sobomobilelib.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import android.util.Log
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util.EnumSet


// TODO - MAKE IT KMP!!! https://developer.android.com/kotlin/multiplatform/sqlite?hl=pl
abstract class DBHelper(
    protected val context: Context,
    factory: SQLiteDatabase.CursorFactory?,
    val dbName: String,
    val dbVersion: Int,
    val dbDirType: String = Environment.DIRECTORY_DOCUMENTS,
) :
    SQLiteOpenHelper(context, dbName, factory, dbVersion) {
    // ================== OVERRIDDEN "TECHNICAL" METHODS ======================
    // Called when the database is created for the first time

    override fun getReadableDatabase(): SQLiteDatabase {
        val dbFile = getDbFile()
//        Log.d("DB", "DBDBDB -- READABLE -- absolute path ${dbFile.absolutePath}")

        return if (dbFile.exists()) {
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath, null,
                SQLiteDatabase.OPEN_READONLY
            )
        } else {
            super.getReadableDatabase()
        }
    }

    override fun getWritableDatabase(): SQLiteDatabase {
        val dbFile = getDbFile()
//        Log.d("DB", "DB - WRITABLE -- absolute path ${dbFile.absolutePath}")

        return if (dbFile.exists()) {
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath, null,
                SQLiteDatabase.OPEN_READWRITE
            )
        } else {
            val db = super.getWritableDatabase()
            db.close() // Close the default DB
            val newDb = SQLiteDatabase.openOrCreateDatabase(dbFile.absolutePath, null)
            onCreate(newDb)
            newDb
        }
    }

    protected fun getDbFile(): File {
        val extStoragePubDir =
            Environment.getExternalStoragePublicDirectory(dbDirType)
        val dbFile = File(extStoragePubDir, dbName)

//        Log.d("DB", "DBDBDB -- getDbFile -- extStoragePubDir $extStoragePubDir DB FILE PATH: ${dbFile.absolutePath}")
//                val dbFile = File(context.getExternalFilesDir(null), DATABASE_NAME)
        try {
            Files.setPosixFilePermissions(
                dbFile.toPath(),
                EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_WRITE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_WRITE
                )
            )
        } catch (e: Exception) {
            Log.d("DB", "DBDBDB -- Failed to set file permissions: ${e.message}")
        }
        return dbFile
    }

}
