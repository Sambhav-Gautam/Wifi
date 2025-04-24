WiFi Scanner Pro
WiFi Scanner Pro is an Android application built with Jetpack Compose and Room that scans for nearby WiFi networks, collects signal strength (RSSI) data, and associates it with geolocation information. The app allows users to select predefined locations, view scan results in list or matrix format, and review detailed logs of past scans.
Features

WiFi Scanning: Automatically scans for nearby WiFi networks and records SSID, BSSID, and RSSI values.
Location Integration: Associates scans with geolocation data (latitude, longitude, and address) using the device's GPS or predefined coordinates.
Location Selection: Choose from three locations (Location A: current device location, Location B and C: offset by ~110 meters).
Data Persistence: Stores scan results and network details in a Room database with support for migrations.
UI Views:
List View: Displays scan results as expandable cards with network details.
Matrix View: Shows RSSI values for scans at the selected location in a grid format.
Logs Dialog: View all past scans with associated network details.


Auto-Scanning: Continuous scanning with a 6-second interval (respects Android's scan throttling).
Permissions Handling: Requests and manages ACCESS_FINE_LOCATION and ACCESS_WIFI_STATE permissions.

Requirements

Android API Level 21 (Lollipop) or higher
Permissions:
ACCESS_FINE_LOCATION: For GPS location and WiFi scanning
ACCESS_WIFI_STATE: For accessing WiFi scan results


WiFi must be enabled on the device

Dependencies

Jetpack Compose: For building the UI
Room: For local database storage
Google Play Services Location: For accessing device location
Kotlin Coroutines: For asynchronous operations
AndroidX Libraries: For core functionality and permissions

Add the following to your build.gradle:
dependencies {
    implementation "androidx.activity:activity-compose:1.9.3"
    implementation "androidx.compose.material3:material3:1.3.0"
    implementation "androidx.room:room-runtime:2.6.1"
    annotationProcessor "androidx.room:room-compiler:2.6.1"
    implementation "androidx.room:room-ktx:2.6.1"
    implementation "com.google.android.gms:play-services-location:21.3.0"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1"
}

Setup

Clone the Repository:
git clone <repository-url>


Add Permissions to AndroidManifest.xml:
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />


Sync Project: Open the project in Android Studio and sync with Gradle.

Run the App: Deploy to an Android device or emulator with WiFi and location services enabled.


Usage

Launch the App: The app starts with Location A set to the device's current location (fallback: San Francisco coordinates).
Select Location: Use the dropdown to choose between Location A, B, or C.
Start Scanning: Press the "Scan" button to begin auto-scanning. The button shows a progress indicator during scanning.
View Results:
List View: Expand cards to see network details (SSID, RSSI, BSSID).
Matrix View: Toggle to see RSSI values for the selected location.
Logs: Open the logs dialog to review all scans.


Refresh Location A: Update Location A with the current device location.
Stop Scanning: Press the "Scan" button again to stop.

Database Schema

scan_results:

id: Auto-generated primary key
scanNumber: Sequential scan number
location: Location name (e.g., "Location A")
totalAPs: Number of access points detected
avgRSSI, minRSSI, maxRSSI: RSSI statistics
timestamp: Scan time
latitude, longitude: Geolocation coordinates
address: Geocoded address


wifi_networks:

id: Auto-generated primary key
scanId: Foreign key linking to scan_results
ssid: Network name
rssi: Signal strength
bssid: MAC address



Notes

Scan Throttling: Android limits WiFi scans to approximately every 4-6 seconds, enforced by the app's 6-second delay.
Geocoder: Address lookup may fail if the Geocoder service is unavailable; falls back to "Unknown location".
Permissions: The app prompts for required permissions on startup. Scanning is disabled without them.
Database Migrations: Includes a migration from version 1 to 2 to add wifi_networks table and location fields.

License
This project is licensed under the MIT License. See the LICENSE file for details.
