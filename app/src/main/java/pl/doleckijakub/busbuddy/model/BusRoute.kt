package pl.doleckijakub.busbuddy.model

data class BusRoute(
    val id: Int,
    val busName: String,
    val direction: String,
    val stops: List<BusStop>
)
