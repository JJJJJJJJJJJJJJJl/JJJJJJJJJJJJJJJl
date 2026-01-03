# Instagram Time Monitor ⏰

An Android app that monitors your Instagram usage and alerts you when you've been on the app for longer than 5 minutes.

## Features

- 📱 Monitors Instagram usage in real-time
- ⏰ Alerts after 5 minutes of continuous use
- 🔔 Shows countdown notification while monitoring
- 🎯 Simple and easy to use interface
- 🔄 Runs as a foreground service for reliability

## How It Works

The app uses Android's `UsageStatsManager` to track when Instagram is in the foreground. When you open Instagram:

1. Timer starts automatically
2. You'll see a notification showing remaining time
3. After 5 minutes, you'll receive an alert notification with sound and vibration
4. Timer resets when you close Instagram

## Requirements

- Android 8.0 (API 26) or higher
- Instagram app installed (`com.instagram.android`)

## Setup Instructions

### 1. Build the App

```bash
# Open in Android Studio or build from command line
./gradlew assembleDebug
```

### 2. Install the App

```bash
# Install the APK on your device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Grant Required Permission

**IMPORTANT:** The app requires "Usage Access" permission to monitor Instagram.

1. Open the app
2. Tap "Grant Usage Access Permission"
3. Find "Instagram Time Monitor" in the list
4. Toggle the permission ON

Alternatively, grant permission manually:
- Go to Settings → Apps → Special app access → Usage access
- Find "Instagram Time Monitor" and enable it

### 4. Start Monitoring

1. Return to the app
2. Tap "Start Monitoring"
3. The app will run in the background

You'll see a persistent notification showing the service is active.

## Usage

### Starting the Monitor
- Open the app
- Ensure Usage Access permission is granted
- Tap "Start Monitoring"

### Stopping the Monitor
- Open the app
- Tap "Stop Monitoring"

### What Happens When You Use Instagram
- Open Instagram normally
- A notification shows your remaining time (e.g., "Time remaining: 4m 32s")
- After 5 minutes, you'll get an alert: "⏰ Instagram Time Limit Reached!"
- The alert includes vibration and notification sound

## Customization

To change the time limit, edit `InstagramMonitorService.kt`:

```kotlin
private val timeLimit = 5 * 60 * 1000L // Change 5 to desired minutes
```

To change the check interval:

```kotlin
private val checkInterval = 2000L // Check every 2 seconds (2000ms)
```

## Project Structure

```
app/
├── src/main/
│   ├── AndroidManifest.xml              # App permissions and components
│   ├── java/com/instagrammonitor/
│   │   ├── MainActivity.kt              # Main UI and permission handling
│   │   └── InstagramMonitorService.kt   # Background monitoring service
│   └── res/layout/
│       └── activity_main.xml            # UI layout
├── build.gradle                          # App dependencies
└── proguard-rules.pro                    # ProGuard configuration
```

## Technical Details

### Permissions Used
- `PACKAGE_USAGE_STATS`: Track which apps are in foreground
- `FOREGROUND_SERVICE`: Run monitoring service
- `POST_NOTIFICATIONS`: Show alerts (Android 13+)
- `WAKE_LOCK`: Keep monitoring active

### How Monitoring Works

The `InstagramMonitorService` runs as a foreground service:

1. Every 2 seconds, it queries `UsageStatsManager` for recent app events
2. Detects when Instagram moves to foreground (`MOVE_TO_FOREGROUND`)
3. Tracks elapsed time since Instagram became active
4. Updates notification with countdown
5. Triggers alert when 5-minute threshold is reached
6. Resets when Instagram moves to background (`MOVE_TO_BACKGROUND`)

### Battery Impact

The app is designed to be battery-efficient:
- Uses infrequent checks (2-second intervals)
- Lightweight queries to UsageStatsManager
- No GPS or network usage

## Troubleshooting

### "Usage Access permission required" Error
- Go to Settings → Apps → Special app access → Usage access
- Enable permission for "Instagram Time Monitor"

### Notifications Not Showing
- Check that notifications are enabled for the app
- On Android 13+, grant notification permission when prompted

### Service Stops Running
- Some devices aggressively kill background services
- Check battery optimization settings
- Disable battery optimization for this app

### Alert Doesn't Trigger
- Ensure Instagram package name is `com.instagram.android`
- Some Instagram variants (Lite, Business) may have different package names
- Check Logcat for debugging information

## Development

### Building from Source

```bash
git clone <repository-url>
cd JJJJJJJJJJJJJJJl
./gradlew build
```

### Running in Debug Mode

```bash
./gradlew installDebug
adb shell am start -n com.instagrammonitor/.MainActivity
```

### Viewing Logs

```bash
adb logcat | grep InstagramMonitor
```

## License

This project is provided as-is for personal use.

## Disclaimer

This app is for personal time management and awareness. It monitors local app usage only and does not collect, store, or transmit any personal data.
