package pl.doleckijakub.busbuddy.dataaccess

import android.database.sqlite.SQLiteDatabase

class RouteRegistry {
    companion object {
        private lateinit var database: SQLiteDatabase

        private fun init() {
            database = DatabaseManager.DB
        }

        private fun getStopIdsAlongRouteById(id: Int): List<Int> {
            init()

            val result = mutableListOf<Int>()

            val query = "SELECT stop_id FROM route_stops WHERE route_id = ?"
            val args = arrayOf("$id")

            database.rawQuery(query, args).use {
                while (it.moveToNext()) {
                    val stopId = it.getInt(it.getColumnIndexOrThrow("stop_id"))

                    result.add(stopId)
                }
            }

            return result
        }

        private fun getRawBusRoutes(): List<Triple<Int, String, String>> {
            init()

            val result = mutableListOf<Triple<Int, String, String>>()

            val query = "SELECT id, bus_name, direction FROM bus_routes"
            val args = emptyArray<String>()

            database.rawQuery(query, args).use {
                while (it.moveToNext()) {
                    val id = it.getInt(it.getColumnIndexOrThrow("id"))
                    val busName = it.getString(it.getColumnIndexOrThrow("bus_name"))
                    val direction = it.getString(it.getColumnIndexOrThrow("direction"))

                    result.add(Triple(id, busName, direction))
                }
            }

            return result
        }

        fun getConnectionGraph(): Map<Int, Set<Int>> {
            init()

            val graph = mutableMapOf<Int, MutableSet<Int>>()

            for (route in getRawBusRoutes()) {
                val stopIds = getStopIdsAlongRouteById(route.first)
                for (i in 1..<stopIds.size) {
                    graph.getOrPut(stopIds[i - 1]) { mutableSetOf() }.add(stopIds[i])
                }
            }

            return graph
        }

        fun getConnectionGraphWithBusInfo(): Map<Int, Set<Pair<Int, Int>>> {
            init()

            val graph = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()

            for (route in getRawBusRoutes()) {
                val stopIds = getStopIdsAlongRouteById(route.first)
                for (i in 1..<stopIds.size) {
                    graph.getOrPut(stopIds[i - 1]) { mutableSetOf() }.addAll(getBussesConnecting(stopIds[i - 1], stopIds[i]).map { Pair(stopIds[i], it) })
                }
            }

            return graph
        }

        fun getRouteString(routeId: Int): String {
            init()

            val query = "SELECT CONCAT(bus_name, ' -> ', direction) FROM bus_routes WHERE id = ?"
            val args = arrayOf(routeId.toString())

            database.rawQuery(query, args).use {
                if (it.moveToNext()) {
                    return it.getString(0)
                }
            }

            throw NoSuchElementException("getRouteString($routeId)")
        }

        private fun getBussesConnecting(a: Int, b: Int): Set<Int> {
            init()

            val result = mutableSetOf<Int>()

            val query = "SELECT id FROM bus_routes WHERE id IN (SELECT id FROM route_stops WHERE stop_id = ?) AND id IN (SELECT id FROM route_stops WHERE stop_id = ?)"
            val args = arrayOf(a.toString(), b.toString())

            database.rawQuery(query, args).use {
                while (it.moveToNext()) {
                    val id = it.getInt(it.getColumnIndexOrThrow("id"))
                    result.add(id)
                }
            }

            return result
        }
    }
}