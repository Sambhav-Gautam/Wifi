package com.example.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val defaultLocation = NamedLocation("Location A", 37.7749, -122.4194) // Fallback
    private val TAG = "WifiScanner"

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        requestPermissions()

        val scanResults = mutableStateListOf<ScanResultData>()
        val currentLocation = mutableStateOf(defaultLocation)
        val locations = derivedStateOf {
            listOf(
                currentLocation.value,
                NamedLocation(
                    "Location B",
                    currentLocation.value.latitude + 0.001, // ~110 meters north
                    currentLocation.value.longitude
                ),
                NamedLocation(
                    "Location C",
                    currentLocation.value.latitude,
                    currentLocation.value.longitude + 0.001 // ~110 meters east
                )
            )
        }
        val selectedLocation = mutableStateOf(locations.value.first())
        var isScanning by mutableStateOf(false)

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.scanDao()

        // Update Location A with current device location
        updateCurrentLocation(currentLocation, selectedLocation)

        // Check WiFi status
        if (!wifiManager.isWifiEnabled) {
            Log.w(TAG, "WiFi is disabled, attempting to enable")
            wifiManager.isWifiEnabled = true
        }

        fun startAutoScan(scope: CoroutineScope, dao: ScanResultDao, location: NamedLocation, onStop: () -> Unit) {
            scope.launch {
                while (isScanning) {
                    if (hasPermissions()) {
                        Log.d(TAG, "Initiating WiFi scan")
                        wifiManager.startScan()
                    }
                    delay(6000) // Android scan throttle: ~4-6 seconds
                }
                onStop()
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!hasPermissions()) {
                    Log.w(TAG, "Permissions missing, skipping scan")
                    return
                }

                val results = try {
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_WIFI_STATE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        wifiManager.scanResults.also {
                            Log.d(TAG, "Scan results: ${it.size} networks found")
                            it.forEach { result ->
                                Log.d(TAG, "Network: SSID=${result.SSID}, BSSID=${result.BSSID}, RSSI=${result.level}")
                            }
                        }
                    } else {
                        emptyList()
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException during scan", e)
                    return
                }

                val location = selectedLocation.value
                val scanNum = scanResults.size + 1
                val rssiValues = results.map { it.level }
                val timestamp = System.currentTimeMillis()

                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    if (location.name == "Location A") {
                        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                            val latitude = loc?.latitude ?: currentLocation.value.latitude
                            val longitude = loc?.longitude ?: currentLocation.value.longitude
                            Log.d(TAG, "Location A: Using lat=$latitude, long=$longitude")
                            processScanResults(
                                dao,
                                scanResults,
                                scanNum,
                                location.name,
                                results,
                                rssiValues,
                                timestamp,
                                latitude,
                                longitude
                            )
                        }.addOnFailureListener { e ->
                            Log.w(TAG, "Failed to get current location, using fallback", e)
                            processScanResults(
                                dao,
                                scanResults,
                                scanNum,
                                location.name,
                                results,
                                rssiValues,
                                timestamp,
                                currentLocation.value.latitude,
                                currentLocation.value.longitude
                            )
                        }
                    } else {
                        Log.d(TAG, "${location.name}: Using lat=${location.latitude}, long=${location.longitude}")
                        processScanResults(
                            dao,
                            scanResults,
                            scanNum,
                            location.name,
                            results,
                            rssiValues,
                            timestamp,
                            location.latitude,
                            location.longitude
                        )
                    }
                }
            }
        }

        registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "WiFi Scanner Pro",
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }
                ) { padding ->
                    WifiScannerContent(
                        modifier = Modifier.padding(padding),
                        locations = locations.value,
                        selectedLocation = selectedLocation,
                        scanResults = scanResults,
                        dao = dao,
                        onScan = {
                            if (hasPermissions()) {
                                isScanning = !isScanning
                                if (isScanning) {
                                    startAutoScan(CoroutineScope(Dispatchers.IO), dao, selectedLocation.value) {
                                        isScanning = false
                                    }
                                }
                            } else {
                                requestPermissions()
                            }
                        },
                        isScanning = isScanning,
                        onRefreshLocation = { updateCurrentLocation(currentLocation, selectedLocation) }
                    )
                }
            }
        }
    }

    private fun hasPermissions(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE)
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}.launch(perms)
    }

    private fun updateCurrentLocation(
        currentLocation: MutableState<NamedLocation>,
        selectedLocation: MutableState<NamedLocation>
    ) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            currentLocation.value = NamedLocation(
                                "Location A",
                                location.latitude,
                                location.longitude
                            )
                            if (selectedLocation.value.name == "Location A") {
                                selectedLocation.value = currentLocation.value
                            }
                            Log.d(TAG, "Updated Location A: lat=${location.latitude}, long=${location.longitude}")
                        } else {
                            Log.w(TAG, "Current location unavailable, using fallback")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to update current location", e)
                    }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException while accessing location", e)
                currentLocation.value = defaultLocation
            }
        } else {
            Log.w(TAG, "ACCESS_FINE_LOCATION permission not granted")
            currentLocation.value = defaultLocation
        }
    }

    private fun processScanResults(
        dao: ScanResultDao,
        scanResults: MutableList<ScanResultData>,
        scanNum: Int,
        locationName: String,
        results: List<ScanResult>,
        rssiValues: List<Int>,
        timestamp: Long,
        latitude: Double,
        longitude: Double
    ) {
        val address = getAddressFromLocation(latitude, longitude)
        val scanData = ScanResultData(
            scanNumber = scanNum,
            location = locationName,
            totalAPs = results.size,
            avgRSSI = if (rssiValues.isNotEmpty()) rssiValues.average().toInt() else -100,
            minRSSI = rssiValues.minOrNull() ?: -100,
            maxRSSI = rssiValues.maxOrNull() ?: -30,
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude,
            address = address,
            networks = results.map { WiFiNetwork(it.SSID, it.level, it.BSSID) }
        )

        CoroutineScope(Dispatchers.IO).launch {
            val scanId = dao.insertScan(
                ScanResultEntity(
                    scanNumber = scanNum,
                    location = locationName,
                    totalAPs = results.size,
                    avgRSSI = if (rssiValues.isNotEmpty()) rssiValues.average().toInt() else -100,
                    minRSSI = rssiValues.minOrNull() ?: -100,
                    maxRSSI = rssiValues.maxOrNull() ?: -30,
                    timestamp = timestamp,
                    latitude = latitude,
                    longitude = longitude,
                    address = address
                )
            ).toInt()
            dao.insertNetworks(results.map {
                WiFiNetworkEntity(
                    scanId = scanId,
                    ssid = it.SSID,
                    rssi = it.level,
                    bssid = it.BSSID
                )
            })
            scanResults.add(0, scanData)
            Log.d(TAG, "Saved scan $scanNum for $locationName: lat=$latitude, long=$longitude, networks=${results.size}")
        }
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown location"
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder failed", e)
            "Unknown location"
        }
    }

    data class NamedLocation(val name: String, val latitude: Double, val longitude: Double)

    @Entity(tableName = "scan_results")
    data class ScanResultEntity(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val scanNumber: Int,
        val location: String,
        val totalAPs: Int,
        val avgRSSI: Int,
        val minRSSI: Int,
        val maxRSSI: Int,
        val timestamp: Long,
        val latitude: Double,
        val longitude: Double,
        val address: String
    )

    @Entity(tableName = "wifi_networks")
    data class WiFiNetworkEntity(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val scanId: Int,
        val ssid: String,
        val rssi: Int,
        val bssid: String
    )

    @Dao
    interface ScanResultDao {
        @Insert
        suspend fun insertScan(scan: ScanResultEntity): Long

        @Insert
        suspend fun insertNetworks(networks: List<WiFiNetworkEntity>)

        @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
        suspend fun getAllScans(): List<ScanResultEntity>

        @Query("SELECT * FROM wifi_networks WHERE scanId = :scanId")
        suspend fun getNetworksForScan(scanId: Int): List<WiFiNetworkEntity>

        @Transaction
        @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
        suspend fun getAllScansWithNetworks(): List<ScanWithNetworks>
    }

    data class ScanWithNetworks(
        @Embedded val scan: ScanResultEntity,
        @Relation(
            parentColumn = "id",
            entityColumn = "scanId"
        )
        val networks: List<WiFiNetworkEntity>
    )

    @Database(entities = [ScanResultEntity::class, WiFiNetworkEntity::class], version = 2)
    abstract class AppDatabase : RoomDatabase() {
        abstract fun scanDao(): ScanResultDao

        companion object {
            @Volatile
            private var INSTANCE: AppDatabase? = null

            private val MIGRATION_1_2 = object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS `wifi_networks` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `scanId` INTEGER NOT NULL,
                            `ssid` TEXT NOT NULL,
                            `rssi` INTEGER NOT NULL,
                            `bssid` TEXT NOT NULL
                        )
                    """.trimIndent())

                    database.execSQL("""
                        ALTER TABLE scan_results 
                        ADD COLUMN latitude REAL NOT NULL DEFAULT 0.0
                    """.trimIndent())
                    database.execSQL("""
                        ALTER TABLE scan_results 
                        ADD COLUMN longitude REAL NOT NULL DEFAULT 0.0
                    """.trimIndent())
                    database.execSQL("""
                        ALTER TABLE scan_results 
                        ADD COLUMN address TEXT NOT NULL DEFAULT 'Unknown location'
                    """.trimIndent())
                }
            }

            fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "wifi_scanner_db")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    data class WiFiNetwork(val ssid: String, val rssi: Int, val bssid: String)

    data class ScanResultData(
        val scanNumber: Int,
        val location: String,
        val totalAPs: Int,
        val avgRSSI: Int,
        val minRSSI: Int,
        val maxRSSI: Int,
        val timestamp: Long,
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val networks: List<WiFiNetwork>
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun WifiScannerContent(
        modifier: Modifier = Modifier,
        locations: List<NamedLocation>,
        selectedLocation: MutableState<NamedLocation>,
        scanResults: List<ScanResultData>,
        dao: ScanResultDao,
        onScan: () -> Unit,
        isScanning: Boolean,
        onRefreshLocation: () -> Unit
    ) {
        var showMatrix by remember { mutableStateOf(false) }
        var showLogsDialog by remember { mutableStateOf(false) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Location Dropdown
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selectedLocation.value.name,
                    onValueChange = {},
                    label = { Text("Select Location") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    locations.forEach { loc ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${loc.name} (Lat: ${"%.4f".format(loc.latitude)}, Long: ${"%.4f".format(loc.longitude)})",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                selectedLocation.value = loc
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Coordinates Display
            Text(
                text = "Latitude: ${"%.4f".format(selectedLocation.value.latitude)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp)
            )
            Text(
                text = "Longitude: ${"%.4f".format(selectedLocation.value.longitude)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp)
            )

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onScan,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isScanning) "Scanning..." else "Scan",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Button(
                    onClick = { showMatrix = !showMatrix },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        if (showMatrix) "List View" else "Matrix View",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Button(
                    onClick = onRefreshLocation,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        "Refresh Loc A",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Button(
                    onClick = { showLogsDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        "View Logs",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Logs Dialog
            if (showLogsDialog) {
                ViewLogsDialog(
                    dao = dao,
                    onDismiss = { showLogsDialog = false }
                )
            }

            // Divider
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.Gray.copy(alpha = 0.2f)
            )

            // Scan Results
            if (showMatrix) {
                RssiMatrixView(
                    scanResults = scanResults,
                    location = selectedLocation.value.name
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scanResults, key = { it.scanNumber }) { scan ->
                        ScanCard(scan)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ViewLogsDialog(
        dao: ScanResultDao,
        onDismiss: () -> Unit
    ) {
        val scansWithNetworks = remember { mutableStateOf<List<ScanWithNetworks>>(emptyList()) }
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                scansWithNetworks.value = dao.getAllScansWithNetworks()
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    "Scan Logs",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scansWithNetworks.value, key = { it.scan.id }) { scanWithNetworks ->
                        LogCard(scanWithNetworks)
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LogCard(scanWithNetworks: ScanWithNetworks) {
        val scan = scanWithNetworks.scan
        val networks = scanWithNetworks.networks
        var expanded by remember { mutableStateOf(false) }
        val formattedTime = remember(scan.timestamp) {
            SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(scan.timestamp))
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Scan ${scan.scanNumber} @ ${scan.location}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Location: ${scan.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "Lat: ${"%.4f".format(scan.latitude)}, Long: ${"%.4f".format(scan.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "APs: ${scan.totalAPs}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Avg: ${scan.avgRSSI} dBm",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Range: ${scan.minRSSI}–${scan.maxRSSI} dBm",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(2f)
                    )
                }
                if (expanded) {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color.Gray.copy(alpha = 0.2f)
                    )
                    networks.forEach { net ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                net.ssid.ifBlank { "<Hidden SSID>" },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                "${net.rssi} dBm",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                net.bssid,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun RssiMatrixView(scanResults: List<ScanResultData>, location: String) {
        val filtered = scanResults.filter { it.location == location }
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "RSSI Matrix for $location",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn {
                items(filtered.chunked(10)) { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach {
                            Text(
                                "${it.avgRSSI}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ScanCard(scan: ScanResultData) {
        var expanded by remember { mutableStateOf(false) }
        val formattedTime = remember(scan.timestamp) {
            SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(scan.timestamp))
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Scan ${scan.scanNumber} @ ${scan.location}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Location: ${scan.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "Lat: ${"%.4f".format(scan.latitude)}, Long: ${"%.4f".format(scan.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "APs: ${scan.totalAPs}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Avg: ${scan.avgRSSI} dBm",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Range: ${scan.minRSSI}–${scan.maxRSSI} dBm",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(2f)
                    )
                }
                if (expanded) {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color.Gray.copy(alpha = 0.2f)
                    )
                    scan.networks.forEach { net ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                net.ssid.ifBlank { "<Hidden SSID>" },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                "${net.rssi} dBm",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                net.bssid,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
            }
        }
    }
}