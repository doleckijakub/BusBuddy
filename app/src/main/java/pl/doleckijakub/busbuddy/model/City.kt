package pl.doleckijakub.busbuddy.model

enum class City(
    val lat: Double,
    val lng: Double,
    val scheduleServiceURL: String,
    val code: String,
) {
    LUBLIN(51.2181536, 22.2347195, "http://sip.ztm.lublin.eu/AndroidService/SchedulesService.svc", "LUBLI"),
    ;
}
