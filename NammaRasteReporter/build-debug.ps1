$ErrorActionPreference = "Stop"

$androidStudioJdk = "C:\Program Files\Android\Android Studio\jbr"
if (-not $env:JAVA_HOME -and (Test-Path $androidStudioJdk)) {
    $env:JAVA_HOME = $androidStudioJdk
}

if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME is not set. Install a JDK or open the project in Android Studio."
}

& "$env:JAVA_HOME\bin\java.exe" -jar ".\gradle\wrapper\gradle-wrapper.jar" assembleDebug
