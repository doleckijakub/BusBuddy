package pl.doleckijakub.busbuddy.model

data class Bus(
    val id: Int,
    val vehicleNumber: Int,
    val busNumber: String, // String because of lines like Z, N1, N2, N3...
) {

}
