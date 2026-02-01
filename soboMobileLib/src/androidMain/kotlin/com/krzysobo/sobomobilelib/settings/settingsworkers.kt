package com.krzysobo.sobomobilelib.settings

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import com.krzysobo.soboapptpl.settings.AppSettingsWorker


/**
 * for RAW SQLite
 */
class AppSettingsWorkerDb(
    androidContext: Context,
    factory: SQLiteDatabase.CursorFactory? = null,
    dbName: String,
    dbVersion: Int,
    dbDirType: String = Environment.DIRECTORY_DOCUMENTS,
) : AppSettingsWorker {
    private var dbHelper: AppSettingsDBHelper = AppSettingsDBHelper(
        androidContext, factory,
        dbName = dbName,
        dbVersion = dbVersion,
        dbDirType = dbDirType,
    )

    override fun getString(key: String, defValue: String): String {
//        println("DBWORKER:: getString():: KEY: $key DEFAULT: $defValue ")
        val resVal = dbHelper.getString(key, defValue)
//        println("DBWORKER:: getString():: RESULT: $resVal  ")
        return resVal
    }

    override fun putString(key: String, value: String) {
        dbHelper.putString(key, value)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return dbHelper.getBoolean(key, defValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        dbHelper.putBoolean(key, value)
    }

}
