# 📡 WiFi Scanner Pro

**WiFi Scanner Pro** is an Android application built using **Jetpack Compose** and **Room**. It scans for nearby WiFi networks, records signal strength (RSSI), and associates the data with geolocation information. Users can select predefined locations, view scan results in both list and matrix formats, and review logs of past scans.

---

## 🚀 Features

- **WiFi Scanning**  
  Automatically scans for nearby WiFi networks and records:
  - SSID
  - BSSID
  - RSSI (signal strength)

- **Location Integration**  
  Associates each scan with:
  - Latitude and longitude
  - Geocoded address (using GPS or predefined coordinates)

- **Location Selection**  
  Choose between:
  - **Location A:** Current device location  
  - **Location B & C:** Offset by ~110 meters

- **Data Persistence**  
  Stores scan results and network details in a **Room** database with migration support.

- **UI Views**
  - **List View:** Expandable cards with network details
  - **Matrix View:** Grid showing RSSI values for each scan
  - **Logs Dialog:** Review all past scan data

- **Auto-Scanning**  
  Scans every 6 seconds (respects Android's scan throttling limitations).

- **Permissions Handling**  
  Manages runtime permissions for location and WiFi access.

---

## 📱 Requirements

- Android API Level **21 (Lollipop)** or higher
- Required Permissions:
  - `ACCESS_FINE_LOCATION`: For GPS and scanning
  - `ACCESS_WIFI_STATE`: For WiFi scan results
- WiFi must be enabled on the device

---

## 📦 Dependencies

- [Jetpack Compose](https://developer.android.com/jetpack/compose) – UI Toolkit  
- [Room](https://developer.android.com/training/data-storage/room) – Local database  
- [Google Play Services Location](https://developers.google.com/location-context/fused-location-provider) – Location services  
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) – Async operations  
- AndroidX Libraries – Core functionality and permissions

### Add the following to `build.gradle`:

```groovy
dependencies {
    implementation "androidx.activity:activity-compose:1.9.3"
    implementation "androidx.compose.material3:material3:1.3.0"
    implementation "androidx.room:room-runtime:2.6.1"
    annotationProcessor "androidx.room:room-compiler:2.6.1"
    implementation "androidx.room:room-ktx:2.6.1"
    implementation "com.google.android.gms:play-services-location:21.3.0"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1"
}
```

---

## 🛠 Setup

1. **Clone the Repository:**

   ```bash
   git clone <repository-url>
   ```

2. **Add Permissions in `AndroidManifest.xml`:**

   ```xml
   <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
   <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
   ```

3. **Sync the Project:**  
   Open in Android Studio and sync with Gradle.

4. **Run the App:**  
   Deploy to a physical device or emulator with WiFi and location enabled.

---

## 📋 Usage

- **Launch the App:** Starts with Location A set to device location (fallback: San Francisco).
- **Select Location:** Choose from A, B, or C using the dropdown.
- **Start Scanning:** Tap the **Scan** button to begin auto-scanning (every 6 seconds).
- **View Results:**
  - **List View:** Expand for SSID, RSSI, and BSSID
  - **Matrix View:** View signal strength across scans
  - **Logs:** Open past scan history
- **Refresh Location A:** Update to the device’s current location
- **Stop Scanning:** Tap the **Scan** button again

---

## 🗃 Database Schema

### `scan_results`

| Field         | Description                    |
|---------------|--------------------------------|
| `id`          | Auto-generated primary key     |
| `scanNumber`  | Sequential scan number         |
| `location`    | Location name (e.g., "Location A") |
| `totalAPs`    | Number of access points        |
| `avgRSSI`     | Average signal strength        |
| `minRSSI`     | Minimum signal strength        |
| `maxRSSI`     | Maximum signal strength        |
| `timestamp`   | Timestamp of scan              |
| `latitude`    | Geolocation latitude           |
| `longitude`   | Geolocation longitude          |
| `address`     | Geocoded address               |

### `wifi_networks`

| Field     | Description                          |
|-----------|--------------------------------------|
| `id`      | Auto-generated primary key           |
| `scanId`  | Foreign key (linked to scan_results) |
| `ssid`    | WiFi network name                    |
| `rssi`    | Signal strength                      |
| `bssid`   | MAC address                          |

---

## 📝 Notes

- **Scan Throttling:** Android enforces a ~4-6 second delay between scans.
- **Geocoder Fallback:** If geocoding fails, address is marked as `"Unknown location"`.
- **Permission Prompts:** Scanning is disabled unless all required permissions are granted.
- **Migrations:** Supports Room migration from version 1 to 2, adding `wifi_networks` table and location fields.

---

## 📄 License

This project is licensed under the **MIT License**.  
See the [LICENSE](./LICENSE) file for details.

---
