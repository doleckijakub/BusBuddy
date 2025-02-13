package pl.doleckijakub.busbuddy

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pl.doleckijakub.busbuddy.dataaccess.BusStopRegistry
import pl.doleckijakub.busbuddy.dataaccess.DatabaseManager
import pl.doleckijakub.busbuddy.model.*
import pl.doleckijakub.busbuddy.service.RouteService
import pl.doleckijakub.busbuddy.ui.theme.BusBuddyTheme

data class AppState(
    var location: MutableState<AppLocation?>,
)

enum class AppLocation {
    BUS_STOP_INFO,
    ROUTE_FINDER,
    SETTINGS,
    ;

    @Composable
    fun GetComposable(state: AppState) {
        when (this) {
            BUS_STOP_INFO -> StopSelectorForm(state)
            ROUTE_FINDER  -> RouteFinderUI(state)
            SETTINGS -> SettingsTab(state)
        }
    }

    private @Composable
    fun SettingsTab(state: AppState) {
        val db = DatabaseManager.DB

        val showTutorialNotificationOnStartup = remember {
            val cursor = db.rawQuery(
                "SELECT showTutorialNotificationOnStartup FROM settings",
                emptyArray<String?>()
            )
            cursor.moveToNext()
            mutableIntStateOf(cursor.getInt(cursor.getColumnIndexOrThrow("showTutorialNotificationOnStartup")))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.size(24.dp))

            HomeButton(state)

            Row(
                horizontalArrangement = Arrangement.Absolute.Right,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show notification on startup")

                Spacer(modifier = Modifier.size(16.dp))

                Switch(
                    checked = showTutorialNotificationOnStartup.intValue != 0,
                    onCheckedChange = { checked ->
                        showTutorialNotificationOnStartup.intValue = if (checked) 1 else 0
                        DatabaseManager.DB.execSQL(
                            "UPDATE settings SET showTutorialNotificationOnStartup = ?",
                            arrayOf(showTutorialNotificationOnStartup.intValue)
                        )
                    },
                )
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build()) // TODO: Remove

        DatabaseManager.init(this)

        enableEdgeToEdge()

        setContent {
            BusBuddyTheme {
                val state = AppState(remember { mutableStateOf(null) })

                if (state.location.value == null) {
                    BusBuddyStartPage(state)
                } else {
                    state.location.value!!.GetComposable(state)
                }
            }
        }
    }
}

@Composable
fun BusBuddyStartPage(state: AppState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.size(24.dp))

        Text("Welcome to BusBuddy, Find out the most accurate, live bus schedule (Bus stop info) or find a route between two bus stops (Find route).", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            state.location.value = AppLocation.BUS_STOP_INFO
        }) {
            Text("Bus stop info")
        }

        Button(onClick = {
            state.location.value = AppLocation.ROUTE_FINDER
        }) {
            Text("Find route")
        }

        Button(onClick = {
            state.location.value = AppLocation.SETTINGS
        }, colors = ButtonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = Color.DarkGray,
            disabledContainerColor = Color.LightGray
        )) {
            Image(
                painter = painterResource(R.drawable.settings),
                contentDescription = "",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(35.dp)
            )
        }
    }
}

@Composable
fun DepartureList(departures: List<Departure>) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        departures.forEach { departure ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = "${departure.name} → ${departure.direction}", fontSize = 16.sp)
                    Text(text = "Departure at/in: ${departure.time}", fontSize = 14.sp)
                    Text(text = "Type: ${departure.type}, Time type: ${departure.timeType}", fontSize = 12.sp, color = if (departure.isGreen == "true") Color.Green else Color.Red)
                }
            }
        }
    }
}

@Composable
fun HomeButton(state: AppState) {
    Button(onClick = {
        state.location.value = null
    }) {
        Image(
            painter = painterResource(R.drawable.home),
            contentDescription = "",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun StopSelectorForm(state: AppState) {
    val selectedBusStop = remember { mutableStateOf<BusStop?>(null) }
    val departures = remember { mutableStateOf<List<Departure>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(24.dp)) {
        HomeButton(state)

        BusStopQueryExact("Select Bus Stop", selectedBusStop)

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                selectedBusStop.value?.let { stop ->
                    coroutineScope.launch {
                        departures.value = stop.getPlannedDepartures()
                    }
                }
            },
            enabled = selectedBusStop.value != null
        ) {
            Text("Search")
        }

        departures.value?.let { list ->
            DepartureList(list)
        }
    }
}

@Composable
fun RouteFinderUI(state: AppState) {
    val context = LocalContext.current
    val serviceConnection = remember { mutableStateOf<RouteService?>(null) }
    val isServiceBound = remember { mutableStateOf(false) }

    val startBusStop = remember { mutableStateOf<String?>(null) }
    val endBusStop = remember { mutableStateOf<String?>(null) }
    val maxPaths = remember { mutableIntStateOf(1) }

    val routes = remember { mutableStateOf<List<List<BusStop>>>(emptyList()) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as RouteService.LocalBinder
                serviceConnection.value = binder.getService()
                isServiceBound.value = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                serviceConnection.value = null
                isServiceBound.value = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val intent = Intent(context, RouteService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        HomeButton(state)

        Text("Find a Route", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        BusStopQuery("Start Stop", startBusStop)
        Spacer(modifier = Modifier.height(16.dp))
        BusStopQuery("End Stop", endBusStop)

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Number of Routes: ", fontSize = 16.sp)
            Slider(
                value = maxPaths.intValue.toFloat(),
                onValueChange = { maxPaths.intValue = it.toInt() },
                valueRange = 1f..10f,
                steps = 9
            )
            Text(maxPaths.intValue.toString(), fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (startBusStop.value != null && endBusStop.value != null && isServiceBound.value) {
                    routes.value = serviceConnection.value?.findRoutes(
                        startBusStop.value!!,
                        endBusStop.value!!,
                        maxPaths.intValue
                    ) ?: emptyList()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Find Routes")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (routes.value.isNotEmpty()) {
            Text("Available Routes:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(routes.value) { route ->
                    Text(
                        text = route.joinToString(" → ") { it.name },
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        } else {
            Text("No routes found.", fontSize = 16.sp, fontStyle = FontStyle.Italic)
        }
    }
}


@Composable
fun BusStopQuery(
    label: String,
    selectedBusStop: MutableState<String?>
) {
    val query = remember { mutableStateOf(TextFieldValue()) }
    val results = remember { mutableStateOf(listOf<String>()) }

    Column {
        Text(label, fontSize = 16.sp)

        TextField(
            value = query.value,
            onValueChange = { textFieldValue ->
                query.value = textFieldValue
                if (textFieldValue.text.isNotBlank()) {
                    results.value = BusStopRegistry.getByName(textFieldValue.text)
                        .map { busStop -> busStop.name.replace(" 0[0-9]$".toRegex(), "").trim() }
                        .distinct()
                        .filter { name -> name.contains(query.value.text, ignoreCase = true) }
                        .sortedWith(compareBy(
                            { !it.contains(query.value.text, ignoreCase = true) },
                            { it.length }
                        ))
                } else {
                    results.value = emptyList()
                }
            },
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .border(1.dp, Color.Gray)
                .padding(8.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(start = 8.dp)
        ) {
            results.value.forEach { busStopName ->
                Text(
                    text = busStopName,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable {
                            selectedBusStop.value = busStopName
                            query.value = TextFieldValue(busStopName)
                            results.value = emptyList()
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun BusStopQueryExact(
    label: String,
    selectedBusStop: MutableState<BusStop?>
) {
    val query = remember { mutableStateOf(TextFieldValue()) }
    val results = remember { mutableStateOf(listOf<BusStop>()) }

    Column {
        Text(label, fontSize = 16.sp)

        TextField(
            value = query.value,
            onValueChange = { textFieldValue ->
                query.value = textFieldValue
                if (textFieldValue.text.isNotBlank()) {
                    results.value = BusStopRegistry.getByName(textFieldValue.text)
                        .filter { stop -> stop.name.contains(query.value.text, ignoreCase = true) }
                        .sortedWith(compareBy(
                            { !it.name.contains(query.value.text, ignoreCase = true) },
                            { it.name.length }
                        ))
                } else {
                    results.value = emptyList()
                }
            },
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .border(1.dp, Color.Gray)
                .padding(8.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(start = 8.dp)
        ) {
            results.value.forEach { stop ->
                Text(
                    text = stop.name,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable {
                            selectedBusStop.value = stop
                            query.value = TextFieldValue(stop.name)
                            results.value = emptyList()
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}
