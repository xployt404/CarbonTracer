package com.example.carbontracerrevised

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

class ConfigFile {
    companion object{
        private const val FILE_NAME = "config.json"
        suspend fun read(context: Context): String? {
            return try {
                withContext(Dispatchers.IO){
                    val inputStream = context.openFileInput(FILE_NAME)
                    inputStream.bufferedReader().use { it.readText() }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

        suspend fun write(context: Context, jsonString: String) {
            try {
                withContext(Dispatchers.IO){
                    val outputStream = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)
                    outputStream.write(jsonString.toByteArray())
                    outputStream.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun getJsonAttribute(jsonString: String?, key: String): Any? {
            val jsonObject = JSONObject(jsonString.toString())
            return if (jsonObject.has(key)) {
                jsonObject.get(key) // Get the value of the attribute
            } else {
                null // Return null if the key does not exist
            }
        }


        suspend fun updateJsonAttribute(context: Context, key: String, newValue: Any) {
            val jsonObject = JSONObject(read(context).toString())
            jsonObject.put(key, newValue) // Update the attribute
            write(context, jsonObject.toString())
        }
    }
}