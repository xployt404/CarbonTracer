package com.example.carbontracerrevised.chat

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

class ChatHistoryDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "Chat.db"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(ChatHistoryContract.SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(ChatHistoryContract.SQL_DELETE_ENTRIES)
        onCreate(db)
    }
}


object ChatHistoryContract {
    object ChatEntry : BaseColumns {
        const val TABLE_NAME = "chat_history"
        const val COLUMN_NAME_ISGEMINI = "isGemini"
        const val COLUMN_NAME_MESSAGE = "message"
        const val COLUMN_NAME_TIMESTAMP = "timestamp"
    }

    const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${ChatEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${ChatEntry.COLUMN_NAME_ISGEMINI} BOOL," +
                "${ChatEntry.COLUMN_NAME_MESSAGE} TEXT," +
                "${ChatEntry.COLUMN_NAME_TIMESTAMP} TEXT)"

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${ChatEntry.TABLE_NAME}"
}