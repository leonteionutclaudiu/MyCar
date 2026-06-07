package com.example.mycar.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LocalNoteDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TEXT TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
        onCreate(db)
    }

    fun insertNote(text: String): Long {
        val values = ContentValues().apply {
            put(COLUMN_TEXT, text)
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
        }
        return writableDatabase.insert(TABLE_NOTES, null, values)
    }

    fun getNotes(): List<LocalNote> {
        val notes = mutableListOf<LocalNote>()
        readableDatabase.rawQuery(
            "SELECT $COLUMN_ID, $COLUMN_TEXT, $COLUMN_CREATED_AT FROM $TABLE_NOTES ORDER BY $COLUMN_ID DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                notes.add(
                    LocalNote(
                        id = cursor.getLong(0),
                        text = cursor.getString(1),
                        createdAt = cursor.getLong(2)
                    )
                )
            }
        }
        return notes
    }

    fun deleteNote(id: Long) {
        writableDatabase.delete(TABLE_NOTES, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    companion object {
        private const val DATABASE_NAME = "mycar_local.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NOTES = "local_notes"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TEXT = "text"
        private const val COLUMN_CREATED_AT = "created_at"
    }
}
