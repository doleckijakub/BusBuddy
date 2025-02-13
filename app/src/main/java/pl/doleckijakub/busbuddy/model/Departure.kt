package pl.doleckijakub.busbuddy.model

data class Departure(
    val id: Int,
    val name: String,
    val time: String,
    val direction: String,
    val directionSymbol: String,
    val plannedTime: String,
    val isGreen: String,
    val type: String,
    val timeType: String,
    val features: String,
    val __unused: String,
)
