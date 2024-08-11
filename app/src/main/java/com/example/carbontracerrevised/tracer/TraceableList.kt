package com.example.carbontracerrevised.tracer

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TraceableList private constructor(context: Context) {
    private val dbHelper: TracerDbHelper = TracerDbHelper(context)

    companion object {
        @Volatile
        private var INSTANCE: TraceableList? = null

        fun getInstance(context: Context): TraceableList {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TraceableList(context).also { INSTANCE = it }
            }
        }
    }

    suspend fun insertTraceable(traceable: Traceable) {
        withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(TracerContract.Traceable.COLUMN_NAME_OBJECT_NAME, traceable.name)
                put(TracerContract.Traceable.COLUMN_NAME_MATERIAL, traceable.material)
                put(TracerContract.Traceable.COLUMN_NAME_AMOUNT, traceable.amount)
                put(TracerContract.Traceable.COLUMN_NAME_OCCURRENCE, traceable.occurrence)
                put(TracerContract.Traceable.COLUMN_NAME_CATEGORY, traceable.category)
                put(TracerContract.Traceable.COLUMN_NAME_CO2E, traceable.co2e)
            }
            db.insert(TracerContract.Traceable.TABLE_NAME, null, values)
        }
    }

    suspend fun deleteTraceable(traceables: List<Traceable>) {
        withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                for (t in traceables) {
                    db.delete(TracerContract.Traceable.TABLE_NAME, "id=?", arrayOf(t.id.toString()))
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    suspend fun readTracer(): MutableList<Traceable> {
        return withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val projection = arrayOf(
                TracerContract.Traceable.COLUMN_NAME_ID,
                TracerContract.Traceable.COLUMN_NAME_OBJECT_NAME,
                TracerContract.Traceable.COLUMN_NAME_MATERIAL,
                TracerContract.Traceable.COLUMN_NAME_AMOUNT,
                TracerContract.Traceable.COLUMN_NAME_OCCURRENCE,
                TracerContract.Traceable.COLUMN_NAME_CATEGORY,
                TracerContract.Traceable.COLUMN_NAME_CO2E
            )

            val cursor = db.query(
                TracerContract.Traceable.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
            )

            val traceables = mutableListOf<Traceable>()
            with(cursor) {
                while (moveToNext()) {
                    val id = getInt(getColumnIndexOrThrow(TracerContract.Traceable.COLUMN_NAME_ID))
                    val objectName = getString(getColumnIndexOrThrow(TracerContract.Traceable.COLUMN_NAME_OBJECT_NAME))
                    val material = getString(getColumnIndexOrThrow(TracerContract.Traceable.COLUMN_NAME_MATERIAL))
                    val amount = getString(getColumnIndexOrThrow(TracerContract.Traceable.COLUMN_NAME_AMOUNT))
                    val occurrence = getString(getColumnIndexOrThrow(TracerContract.Traceable.COLUMN_NAME_OCCURRENCE))
                    val category = getInt(getColumnIndexOrThrow(TracerContract.Traceable.COLUMN_NAME_CATEGORY))
                    val co2e = getString(getColumnIndexOrThrow(TracerContract.Traceable.COLUMN_NAME_CO2E))
                    traceables.add(Traceable(id, objectName, material, amount, occurrence, category, co2e))
                }
            }
            cursor.close()
            traceables
        }
    }

    suspend fun updateTraceable(traceable: Traceable) {
        withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(TracerContract.Traceable.COLUMN_NAME_OBJECT_NAME, traceable.name)
                put(TracerContract.Traceable.COLUMN_NAME_MATERIAL, traceable.material)
                put(TracerContract.Traceable.COLUMN_NAME_AMOUNT, traceable.amount)
                put(TracerContract.Traceable.COLUMN_NAME_OCCURRENCE, traceable.occurrence)
                put(TracerContract.Traceable.COLUMN_NAME_CATEGORY, traceable.category)
                put(TracerContract.Traceable.COLUMN_NAME_CO2E, traceable.co2e)
            }
            db.update(TracerContract.Traceable.TABLE_NAME, values, "id = ?", arrayOf(traceable.id.toString()))
        }
    }
}


//class TraceableList {
//    companion object{
//        fun insertTraceable(context: Context, traceable: Traceable) {
//            val dbHelper = TracerDbHelper(context)
//            val db = dbHelper.writableDatabase
//
//            val values = ContentValues().apply {
//                put(COLUMN_NAME_OBJECT_NAME, traceable.objectName)
//                put(COLUMN_NAME_MATERIAL, traceable.material)
//                put(COLUMN_NAME_AMOUNT, traceable.amount)
//                put(COLUMN_NAME_OCCURRENCE, traceable.occurrence)
//                put(COLUMN_NAME_CATEGORY, traceable.category)
//                put(COLUMN_NAME_CO2E, traceable.co2e)
//            }
//
//            db.insert(TABLE_NAME, null, values)
//            dbHelper.close()
//            db.close()
//        }
//
//        fun deleteTraceable(context: Context, traceables: List<Traceable>) {
//            val dbHelper = TracerDbHelper(context)
//            val db = dbHelper.writableDatabase
//
//            for (t in traceables){
//                db.delete(TABLE_NAME, "id=?", arrayOf(t.id.toString()))
//            }
//
//            dbHelper.close()
//            db.close()
//        }
//
//        fun readTracer(context : Context): MutableList<Traceable> {
//            val dbHelper = TracerDbHelper(context)
//            val db = dbHelper.readableDatabase
//
//            val projection = arrayOf(
//                COLUMN_NAME_ID,
//                COLUMN_NAME_OBJECT_NAME,
//                COLUMN_NAME_MATERIAL,
//                COLUMN_NAME_AMOUNT,
//                COLUMN_NAME_OCCURRENCE,
//                COLUMN_NAME_CATEGORY,
//                COLUMN_NAME_CO2E
//            )
//
//            val cursor = db.query(
//                TABLE_NAME,
//                projection,
//                null,
//                null,
//                null,
//                null,
//                null
//            )
//
//            val traceables = mutableListOf<Traceable>()
//            with(cursor) {
//                while (moveToNext()) {
//                    val id = getInt(getColumnIndexOrThrow(COLUMN_NAME_ID))
//                    val objectName = getString(getColumnIndexOrThrow(COLUMN_NAME_OBJECT_NAME))
//                    val material = getString(getColumnIndexOrThrow(COLUMN_NAME_MATERIAL))
//                    val amount = getString(getColumnIndexOrThrow(COLUMN_NAME_AMOUNT))
//                    val occurence = getString(getColumnIndexOrThrow(COLUMN_NAME_OCCURRENCE))
//                    val category = getInt(getColumnIndexOrThrow(COLUMN_NAME_CATEGORY))
//                    val co2e = getString(getColumnIndexOrThrow(COLUMN_NAME_CO2E))
//                    traceables.add(Traceable(id, objectName, material, amount, occurence, category, co2e))
//                }
//            }
//            cursor.close()
//            dbHelper.close()
//            db.close()
//
//            return traceables
//        }
//
//        fun updateTraceable(context: Context, traceable: Traceable) {
//            val dbHelper = TracerDbHelper(context)
//            val db = dbHelper.writableDatabase
//            val values = ContentValues().apply {
//                put(COLUMN_NAME_OBJECT_NAME, traceable.objectName)
//                put(COLUMN_NAME_MATERIAL, traceable.material)
//                put(COLUMN_NAME_AMOUNT, traceable.amount)
//                put(COLUMN_NAME_OCCURRENCE, traceable.occurrence)
//                put(COLUMN_NAME_CATEGORY, traceable.category)
//                put(COLUMN_NAME_CO2E, traceable.co2e)
//            }
//            db.update(TABLE_NAME, values, "id = ?", arrayOf(traceable.id.toString()))
//            dbHelper.close()
//            db.close()
//        }
//
//    }
//
//}