package pl.doleckijakub.busbuddy.model

import pl.doleckijakub.busbuddy.service.JScheduleServiceClient

data class BusStop(
    val id: Int,
    val city: City,
    val name: String,
    val lat: Float,
    val lng: Float,
    val address: String,
) {
    fun getPlannedDepartures(): List<Departure> {
        return JScheduleServiceClient.getDepartures(city, id)
    }

    override fun hashCode(): Int {
        return id
    }
}
