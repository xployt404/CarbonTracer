package com.example.carbontracerrevised.tracer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

class TracerDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "Tracer.db"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(TracerContract.SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle schema changes here instead of dropping the table
        // For example, you could add ALTER TABLE statements to migrate data
        db.execSQL(TracerContract.SQL_DELETE_ENTRIES)
        onCreate(db)
    }
}

object TracerContract {
    object Traceable : BaseColumns {
        const val COLUMN_NAME_ID = "id"
        const val TABLE_NAME = "tracer"
        const val COLUMN_NAME_OBJECT_NAME = "objectName"
        const val COLUMN_NAME_MATERIAL = "material"
        const val COLUMN_NAME_AMOUNT = "amount" // Consider changing to INTEGER or REAL if applicable
        const val COLUMN_NAME_OCCURRENCE = "occurrence" // Consider changing to INTEGER or REAL if applicable
        const val COLUMN_NAME_CATEGORY = "category" // This should be INTEGER
        const val COLUMN_NAME_CO2E = "co2e" // Consider changing to REAL if applicable
    }

    const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${Traceable.TABLE_NAME} (" +
                "${Traceable.COLUMN_NAME_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                "${Traceable.COLUMN_NAME_OBJECT_NAME} TEXT," +
                "${Traceable.COLUMN_NAME_MATERIAL} TEXT," +
                "${Traceable.COLUMN_NAME_AMOUNT} REAL," + // Changed to REAL
                "${Traceable.COLUMN_NAME_OCCURRENCE} REAL," + // Changed to REAL
                "${Traceable.COLUMN_NAME_CATEGORY} INTEGER," +
                "${Traceable.COLUMN_NAME_CO2E} REAL)" // Changed to REAL

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${Traceable.TABLE_NAME}"
}
