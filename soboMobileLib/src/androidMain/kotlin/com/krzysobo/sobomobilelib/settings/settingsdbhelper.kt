package com.krzysobo.sobomobilelib.settings

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import com.krzysobo.sobomobilelib.db.DBHelper

class AppSettingsDBHelper(
    context: Context,
    factory: SQLiteDatabase.CursorFactory?,
    dbName: String,
    dbVersion: Int,
    dbDirType: String = Environment.DIRECTORY_DOCUMENTS,
) :
    DBHelper(
        context = context,
        factory = factory,
        dbName = dbName,
        dbVersion = dbVersion,
        dbDirType = dbDirType
    ) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $ID_COL VARCHAR(255) PRIMARY KEY,
                $STRING_VALUE_COL TEXT
            )
        """.trimIndent()

        db.execSQL(createTableQuery)
    }

    // Called when the database needs to be upgraded
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun getBoolean(handle: String, defValue: Boolean): Boolean {
        val defStrValue = if (defValue) "1" else "0"
        val dataStr = getString(handle, defStrValue)
        return dataStr == "1"
    }

    fun putBoolean(handle: String, value: Boolean) {
        val targetStrValue = if (value) "1" else "0"
        putString(handle, targetStrValue)
    }

    fun putString(handle: String, stringValue: String) {
        if (!settingHandleExists(handle)) {
//            println("DBWORKER:: putString:: handle:: $handle: DOES NOT EXIST - INSERTING!!! ")
            insertString(handle, stringValue)
        } else {
//            println("DBWORKER:: putString:: handle:: $handle: EXISTS - UPDATING!!! ")
            updateString(handle, stringValue)
        }
    }

    // Retrieves all records from the database
    fun getString(handle: String, defValue: String): String {
        val c = getCursorForSettingHandle(handle)
//        println("DBWORKER:: getString SIZE:: ${c?.count}")
        if (c == null) {
            return defValue
        } else if (c.count < 1) {
//            println("DBROWKER:: String with handle $handle not found - returing default value: $defValue")
            c.close()
            return defValue
        }

        var res = ""
//        println("DBWORKER:: 1111111111")
        try {
            c.use {
//                println("DBWORKER:: 2222222222222222222")
                if (c.moveToFirst()) {
//                    println("DBWORKER:: 33333333333333333333 MOVED TO FIRST!!!")
                    val colInd = c.getColumnIndex(STRING_VALUE_COL)
//                    println("DBWORKER:: getString:: COLINDEX:: $colInd")
                    val colName = c.getColumnName(colInd)
//                    println("DBWORKER:: getString:: COLINDEX:: $colInd COLNAME:: $colName")

                    res = c.getString(colInd)
//                    println("DBWORKER:: getString:: COLINDEX:: $colInd VALUE: $res")
                }
            }
        } catch (_: IllegalArgumentException) {
            c.close()
            return defValue
        }

        c.close()
        return res
    }


    // ====================== PRIVATE AND PROTECTED METHODS =================
    private fun updateString(handle: String, stringValue: String) {
        val values = ContentValues().apply {
            put(ID_COL, handle)
            put(STRING_VALUE_COL, stringValue)
        }

        writableDatabase.use { db ->
            db.update(TABLE_NAME, values, "handle = ?", arrayOf(handle))
        }
    }

    // Inserts a new record into the database
    private fun insertString(handle: String, stringValue: String) {
        val values = ContentValues().apply {
            put(ID_COL, handle)
            put(STRING_VALUE_COL, stringValue)
        }

        writableDatabase.use { db ->
            db.insert(TABLE_NAME, null, values)
        }
    }

    /**
     * checks if the setting handle $handle exists in the settings table
     */
    private fun settingHandleExists(handle: String): Boolean {
        val c = getCursorForSettingHandle(handle) ?: return false

        c.close()
        return true
    }

    /**
     * opens and returns the cursor for handle $handle, if found in the settings table
     */
    private fun getCursorForSettingHandle(handle: String): Cursor? {
        val c = readableDatabase.rawQuery(
            "SELECT $ID_COL, $STRING_VALUE_COL FROM $TABLE_NAME WHERE handle = ?",
            arrayOf(handle)
        )
//        println("DBWORKER:: getCursorForSettingHandle:: $handle count: ${c.count}")
        return if (c.count > 0) {
            c
        } else {
            c.close(); null
        }
    }

    // companion object for this particular class, not its parent, so it can be private
    private companion object {
        const val TABLE_NAME = "APP_SETTINGS_TABLE"
        const val ID_COL = "HANDLE"
        const val STRING_VALUE_COL = "STRING_VALUE"
    }
}