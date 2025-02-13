package pl.doleckijakub.busbuddy.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import pl.doleckijakub.busbuddy.dataaccess.BusStopRegistry
import pl.doleckijakub.busbuddy.dataaccess.RouteRegistry
import pl.doleckijakub.busbuddy.model.BusStop
import java.util.*
import java.util.logging.Logger

class RouteService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): RouteService = this@RouteService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun findRoutes(startName: String, endName: String, maxPaths: Int): List<List<BusStop>> {
        val startStops = BusStopRegistry.getByName(startName)
        val endStops = BusStopRegistry.getByName(endName)

        if (startStops.isEmpty() || endStops.isEmpty()) {
            return emptyList()
        }

        val graph = RouteRegistry.getConnectionGraphWithBusInfo()
        val allPaths = mutableListOf<List<BusStop>>()

        for (start in startStops) {
            for (end in endStops) {
                val paths = findShortestPaths(graph, start.id, end.id, maxPaths)
                Log.d(null, "${start.name} => ${end.name} - ${paths.joinToString(separator = ", ")}")
                allPaths.addAll(paths.map { ids -> ids.map { busName -> BusStopRegistry.getByName(busName.second)[0] } })
                allPaths.sortBy { path -> path.size }
                if (allPaths.size >= maxPaths) return allPaths.take(maxPaths)
            }
        }

        return allPaths.take(maxPaths)
    }

    private fun findShortestPaths(
        graph: Map<Int, Set<Pair<Int, Int>>>,
        start: Int,
        end: Int,
        maxPaths: Int
    ): MutableList<List<Pair<Int, String>>> {
        val queue = PriorityQueue<Pair<List<Pair<Int, Int>>, Int>>(compareBy { it.second })
        queue.add(listOf(start to -1) to 0)
        val results = mutableListOf<List<Pair<Int, String>>>()

        while (queue.isNotEmpty() && results.size < maxPaths) {
            val (path, cost) = queue.poll()!!
            val lastStop = path.last().first

            if (lastStop == end) {
                results.add(path.drop(1).map { p -> Pair(p.first, RouteRegistry.getRouteString(p.second)) })
                continue
            }

            if (cost > 30) break

            graph[lastStop]?.forEach { (nextStop, busNumber) ->
                if (nextStop !in path.map { it.first }) {
                    val newPath = path + (nextStop to busNumber)
                    queue.add(newPath to (cost + 1))
                }
            }
        }

        return results
    }

}
