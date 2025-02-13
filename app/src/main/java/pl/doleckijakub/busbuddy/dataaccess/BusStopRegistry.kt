package pl.doleckijakub.busbuddy.dataaccess

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import pl.doleckijakub.busbuddy.R
import pl.doleckijakub.busbuddy.model.BusStop
import pl.doleckijakub.busbuddy.model.City

class BusStopRegistry {

    companion object {
        private lateinit var database: SQLiteDatabase

        private fun init() {
            database = DatabaseManager.DB
        }

        fun getById(id: Int): BusStop {
            init()

            val query = "SELECT * FROM bus_stops WHERE id = ?"
            val args = arrayOf("$id")

            val cursor: Cursor = database.rawQuery(query, args)

            cursor.use {
                while (cursor.moveToNext()) {
//                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    val city = City.LUBLIN; // TODO: fix
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    val lat = cursor.getFloat(cursor.getColumnIndexOrThrow("lat"))
                    val lng = cursor.getFloat(cursor.getColumnIndexOrThrow("lng"))
                    val address = cursor.getString(cursor.getColumnIndexOrThrow("address"))

                    return BusStop(id, city, name, lat, lng, address)
                }
            }

            throw NoSuchElementException("BusStop #$id")
        }

        fun getByName(q: String): List<BusStop> {
            init()

            val result = mutableListOf<BusStop>()

            val query = "SELECT * FROM bus_stops WHERE name LIKE ?"
            // val args = arrayOf("%$q%")
            val args = arrayOf("$q%")

            val cursor: Cursor = database.rawQuery(query, args)

            cursor.use {
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    val city = City.LUBLIN; // TODO: fix
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    val lat = cursor.getFloat(cursor.getColumnIndexOrThrow("lat"))
                    val lng = cursor.getFloat(cursor.getColumnIndexOrThrow("lng"))
                    val address = cursor.getString(cursor.getColumnIndexOrThrow("address"))

                    result.add(BusStop(id, city, name, lat, lng, address))
                }
            }

            return result
        }
    }

}
