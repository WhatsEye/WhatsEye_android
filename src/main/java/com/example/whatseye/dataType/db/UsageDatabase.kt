package com.example.whatseye.dataType.db

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.whatseye.dataType.data.UsageData

class UsageDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "usage.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_USAGE = "usage_data"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_HOUR = "hour"
        private const val COLUMN_USAGE_SECONDS = "usage_seconds"
        private const val COLUMN_SENT = "sent"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_USAGE (
                $COLUMN_DATE TEXT NOT NULL,
                $COLUMN_HOUR INTEGER NOT NULL,
                $COLUMN_USAGE_SECONDS INTEGER,
                $COLUMN_SENT INTEGER DEFAULT 0,
                PRIMARY KEY ($COLUMN_DATE, $COLUMN_HOUR)
            )
        """.trimIndent()
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USAGE")
        onCreate(db)
    }

    @Synchronized
    fun insertOrUpdateUsageData(date: String, hour: Int, usageSeconds: Long, sent: Int): Long {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val cursor = db.rawQuery(
                "SELECT $COLUMN_SENT FROM $TABLE_USAGE WHERE $COLUMN_DATE = ? AND $COLUMN_HOUR = ?",
                arrayOf(date, hour.toString())
            )

            val shouldInsert: Boolean = !cursor.moveToFirst()
            cursor.close()

            val result: Long

            if (shouldInsert) {
                val contentValues = ContentValues().apply {
                    put(COLUMN_DATE, date)
                    put(COLUMN_HOUR, hour)
                    put(COLUMN_USAGE_SECONDS, usageSeconds)
                    put(COLUMN_SENT, sent) // Mark new data as unsent
                }
                result = db.insert(TABLE_USAGE, null, contentValues)
                Log.d("UsageDatabase", "Inserted new usage data: date=$date, hour=$hour, usage_seconds=$usageSeconds")
            } else {
                val contentValues = ContentValues().apply {
                    put(COLUMN_USAGE_SECONDS, usageSeconds)
                    put(COLUMN_SENT, sent) // Optionally reset sent flag on update
                }
                val rowsUpdated = db.update(
                    TABLE_USAGE,
                    contentValues,
                    "$COLUMN_DATE = ? AND $COLUMN_HOUR = ?",
                    arrayOf(date, hour.toString())
                )
                result = rowsUpdated.toLong()
                Log.d("UsageDatabase", "Updated existing usage data: date=$date, hour=$hour, usage_seconds=$usageSeconds")
            }

            db.setTransactionSuccessful()
            result
        } catch (e: Exception) {
            Log.e("UsageDatabase", "Error inserting or updating usage data", e)
            -1L
        } finally {
            db.endTransaction()
        }
    }

    @SuppressLint("Range")
    fun getUnsent(): List<UsageData> {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_USAGE WHERE $COLUMN_SENT = 0"
        val cursor = db.rawQuery(query, null)
        val unsentData = mutableListOf<UsageData>()

        if (cursor.moveToFirst()) {
            do {
                val date = cursor.getString(cursor.getColumnIndex(COLUMN_DATE))
                val hour = cursor.getInt(cursor.getColumnIndex(COLUMN_HOUR))
                val usageSeconds = cursor.getLong(cursor.getColumnIndex(COLUMN_USAGE_SECONDS))
                unsentData.add(UsageData(date, hour, usageSeconds))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return unsentData
    }
    fun getUsageDataForDate( date: String): List<UsageData> {
        val db = readableDatabase
        val usageList = mutableListOf<UsageData>()
        val query = "SELECT $COLUMN_HOUR, $COLUMN_USAGE_SECONDS FROM $TABLE_USAGE WHERE $COLUMN_DATE = ? ORDER BY $COLUMN_HOUR ASC"
        val cursor = db.rawQuery(query, arrayOf(date))

        if (cursor.moveToFirst()) {
            do {
                val hour = cursor.getInt(0)
                val seconds = cursor.getLong(1)
                usageList.add(UsageData(date,hour, seconds))
                Log.d("rawQuery", hour.toString())
            } while (cursor.moveToNext())
        }
        cursor.close()
        return usageList
    }
    }
