# Namma-Raste Reporter

Android App Development using GenAI - Project 82.

This Android Studio project implements a rapid infrastructure reporting app for potholes and broken streetlights.

## Features

- Simple citizen login to reduce anonymous spam.
- CameraX preview and photo capture.
- Issue type and severity selection.
- Automatic timestamp and best available GPS location logging.
- Unique ticket ID generation for every saved report.
- Room local database storage.
- Status tracker by ticket ID.

## Build

Open the project folder in Android Studio and run the `app` configuration.

From PowerShell, you can also build the debug APK with:

```powershell
.\build-debug.ps1
```

The generated APK is saved at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Open In Android Studio

1. Open Android Studio.
2. Select **Open**.
3. Choose this folder: `NammaRasteReporter`.
4. Let Gradle sync download the Android, Kotlin, CameraX, Room, and Material dependencies.
5. Run the app on a physical Android phone for best CameraX and GPS testing.

If Android Studio asks for a Gradle JDK, choose the bundled JDK.
