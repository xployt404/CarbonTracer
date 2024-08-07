package com.example.carbontracerrevised.chat

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatHistory (context: Context){
    private val dbHelper = ChatHistoryDbHelper(context)


    suspend fun insertChatMessage(isGemini: Boolean, message: String, timestamp: String) {
        return withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(ChatHistoryContract.ChatEntry.COLUMN_NAME_ISGEMINI, isGemini)
                put(ChatHistoryContract.ChatEntry.COLUMN_NAME_MESSAGE, message)
                put(ChatHistoryContract.ChatEntry.COLUMN_NAME_TIMESTAMP, timestamp)
            }

            db.insert(ChatHistoryContract.ChatEntry.TABLE_NAME, null, values)
        }
    }
    suspend fun readChatHistory(): MutableList<ChatMessage> {
        return withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase

            val projection = arrayOf(
                ChatHistoryContract.ChatEntry.COLUMN_NAME_ISGEMINI,
                ChatHistoryContract.ChatEntry.COLUMN_NAME_MESSAGE,
                ChatHistoryContract.ChatEntry.COLUMN_NAME_TIMESTAMP
            )

            val cursor = db.query(
                ChatHistoryContract.ChatEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
            )

            val chatHistory = mutableListOf<ChatMessage>()
            with(cursor) {
                while (moveToNext()) {
                    val isGemini =
                        getInt(getColumnIndexOrThrow(ChatHistoryContract.ChatEntry.COLUMN_NAME_ISGEMINI))
                    val message =
                        getString(getColumnIndexOrThrow(ChatHistoryContract.ChatEntry.COLUMN_NAME_MESSAGE))
                    if (isGemini == 0) {
                        chatHistory.add(ChatMessage(message, isSent = true, fromGemini = false))
                    } else {
                        chatHistory.add(ChatMessage(message, isSent = true, fromGemini = true))
                    }
                }
            }
            cursor.close()

            chatHistory
        }
    }

    suspend fun clearChartHistory() {
        return withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(ChatHistoryContract.ChatEntry.TABLE_NAME, null, null)
        db.close()
        }
    }



}