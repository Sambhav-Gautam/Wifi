```
   ____ ___ ______   ____       _            
  / ___(___|  ___ \ / __/______| |__   ___   
 | |      _|  ___  | |__ | ___||  _ \ / __|  
 | |__   | | |___| |  __|| |___| | | | | 
  \____|  |_|_|   |_|_|  \____||_| |_|___|

```

# WiFi Scanner Pro

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.3.0-green.svg)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room-2.6.1-blue.svg)](https://developer.android.com/training/data-storage/room)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)](https://github.com/Sambhav-Gautam/Wifi/actions)

WiFi Scanner Pro is a sophisticated Android application built with **Jetpack Compose** and **Room**. It enables users to scan nearby WiFi networks, capture signal strength (RSSI), and associate data with geolocation information. The app supports predefined location selection, displays scan results in list and matrix formats, and maintains a detailed log of past scans for analysis.

## Features

- **WiFi Scanning**  
  Automatically detects nearby WiFi networks and records:  
  - SSID (Network Name)  
  - BSSID (MAC Address)  
  - RSSI (Signal Strength)  

- **Geolocation Integration**  
  Associates scan data with:  
  - Latitude and longitude coordinates  
  - Geocoded address derived from GPS or predefined coordinates  

- **Location Selection**  
  Choose from predefined locations:  
  - **Location A**: Current device location (via GPS)  
  - **Location B & C**: Locations offset by approximately 110 meters from Location A  

- **Data Persistence**  
  Stores scan results and network details in a **Room** database with robust schema migration support.  

- **User Interface Views**  
  - **List View**: Expandable cards displaying detailed network information (SSID, RSSI, BSSID)  
  - **Matrix View**: Grid-based visualization of RSSI values across multiple scans  
  - **Logs Dialog**: Historical view of all past scan data  

- **Automated Scanning**  
  Performs scans every 6 seconds, adhering to Android's scan throttling restrictions.  

- **Permission Management**  
  Seamlessly handles runtime permissions for location and WiFi access to ensure compliance and functionality.

## Demo Video

https://github.com/user-attachments/assets/0f0d15fd-347c-411a-9e3b-0ee85c1d3d00


## Requirements

- **Minimum Android API**: Level 21 (Lollipop) or higher  
- **Permissions**:  
  - `ACCESS_FINE_LOCATION`: Required for GPS and WiFi scanning  
  - `ACCESS_WIFI_STATE`: Required to retrieve WiFi scan results  
- **Hardware**: Device with WiFi and location services enabled  

## Dependencies

The project relies on the following Android and third-party libraries:  
- Jetpack Compose – Modern UI toolkit  
- Room – Persistent local database  
- Google Play Services Location – Geolocation services  
- Kotlin Coroutines – Asynchronous operations  
- AndroidX Libraries – Core functionality and permission handling  

### Gradle Dependencies

Add the following to your `app/build.gradle`:

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

## Setup Instructions

1. **Clone the Repository**  
   ```bash
   git clone https://github.com/Sambhav-Gautam/Wifi.git
   ```

2. **Configure Permissions**  
   Update `AndroidManifest.xml` to include:  
   ```xml
   <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
   <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
   ```

3. **Sync the Project**  
   Open the project in Android Studio and sync with Gradle to resolve dependencies.

4. **Run the Application**  
   Deploy to a physical Android device or emulator with WiFi and location services enabled.

## Usage Guide

1. **Launch the Application**  
   The app initializes with **Location A** set to the device's current location. If location services are unavailable, it defaults to a predefined location (San Francisco).

2. **Select a Location**  
   Use the dropdown menu to choose between **Location A**, **Location B**, or **Location C**.

3. **Initiate Scanning**  
   Press the **Scan** button to start automatic scanning every 6 seconds.

4. **View Scan Results**  
   - **List View**: Expand cards to view detailed network information (SSID, RSSI, BSSID).  
   - **Matrix View**: Visualize RSSI values in a grid format.  
   - **Logs Dialog**: Access a comprehensive history of all scans.

5. **Refresh Location A**  
   Update Location A to the device's current GPS coordinates.

6. **Stop Scanning**  
   Press the **Scan** button again to pause automatic scanning.

## Database Schema

### `scan_results`

| Field         | Type   | Description                          |
|---------------|--------|--------------------------------------|
| `id`          | Integer | Auto-generated primary key           |
| `scanNumber`  | Integer | Sequential scan identifier           |
| `location`    | String  | Location name (e.g., "Location A")   |
| `totalAPs`    | Integer | Number of detected access points     |
| `avgRSSI`     | Float   | Average signal strength              |
| `minRSSI`     | Integer | Minimum signal strength              |
| `maxRSSI`     | Integer | Maximum signal strength              |
| `timestamp`   | Long    | Scan timestamp (epoch)               |
| `latitude`    | Double  | Geolocation latitude                 |
| `longitude`   | Double  | Geolocation longitude                |
| `address`     | String  | Geocoded address                     |

### `wifi_networks`

| Field     | Type   | Description                          |
|-----------|--------|--------------------------------------|
| `id`      | Integer | Auto-generated primary key           |
| `scanId`  | Integer | Foreign key (links to `scan_results`)|
| `ssid`    | String  | WiFi network name                    |
| `rssi`    | Integer | Signal strength                      |
| `bssid`   | String  | MAC address                          |

## Technical Notes

- **Scan Throttling**: Android imposes a 4-6 second delay between WiFi scans to optimize battery usage.  
- **Geocoder Fallback**: If geocoding fails, the address field defaults to "Unknown location".  
- **Permission Handling**: Scanning is disabled until all required permissions are granted.  
- **Database Migrations**: Supports Room migrations from version 1 to 2, adding the `wifi_networks` table and location fields.

## Contributing

Contributions are encouraged! To contribute:  
1. Fork the repository.  
2. Create a feature branch (`git checkout -b feature/your-feature`).  
3. Commit your changes (`git commit -m 'Add your feature'`).  
4. Push to the branch (`git push origin feature/your-feature`).  
5. Open a Pull Request.  

Ensure code adheres to the project's coding standards and includes relevant tests.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
