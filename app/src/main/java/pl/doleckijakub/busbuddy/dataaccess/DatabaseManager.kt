package pl.doleckijakub.busbuddy.dataaccess

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.collection.arraySetOf
import pl.doleckijakub.busbuddy.R
import java.io.File
import java.io.FileOutputStream

class DatabaseManager {
    companion object {
        lateinit var DB: SQLiteDatabase

        fun init(context: Context) {
            DB = create(context, R.raw.appdata, "appdata.db")
        }

        private fun create(context: Context, rawResourceId: Int, dbName: String): SQLiteDatabase {
            val outputFile = File(context.filesDir, dbName)

            if (outputFile.exists())
//                 outputFile.delete()
                ;
            else {
                val inputStream = context.resources.openRawResource(rawResourceId)
                FileOutputStream(outputFile).use { output ->
                    inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
            }

            val result = SQLiteDatabase.openDatabase(outputFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)

            when (rawResourceId) {
                R.raw.appdata -> {
                    result.execSQL(
                        """
                    CREATE TABLE IF NOT EXISTS settings (
                        id INTEGER PRIMARY KEY, 
                        showTutorialNotificationOnStartup INTEGER NOT NULL DEFAULT 1
                    )
                    """
                    )

                    result.execSQL("INSERT OR IGNORE INTO settings (id, showTutorialNotificationOnStartup) VALUES (1, 1)")
                }
            }

            return result
        }
    }
}