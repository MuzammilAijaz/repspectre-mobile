# RepSpectre Mobile

RepSpectre Mobile is the Android companion to a barbell-mounted workout tracker, called [RepSpectre](https://github.com/MuzammilAijaz/repspectre).

The larger project aims to turn movement at the barbell into a useful record of training and provide objective information alongside a lifter's own subjective assessment. Rate of perceived exertion (RPE) and repetitions in reserve (RIR) are separate measures that can provide this context.

Currently, this app receives IMU telemetry over BLE, labels recordings with workout context, stores them in Room, and supplies the resulting sessions to [RepSpectre ML](https://github.com/MuzammilAijaz/repspectre-ml).

## Project context

RepSpectre Mobile connects the embedded and host-side projects:

```text

   repspectre: Firmware captures barbell motion
        |
        v
-> repspectre-mobile: This app receives, labels, and stores sessions (currently)
        |
        v
   repspectre-ml: Prepares the sessions for analysis and training
```

> **Project status:** The app is still being reworked into a more complete Android architecture. The raylib layer builds and has a desktop path. Workout coaching features are planned work.

For model preparation and sensor data analysis, see [RepSpectre ML](https://github.com/MuzammilAijaz/repspectre-ml). For the portable embedded firmware, see [RepSpectre](https://github.com/MuzammilAijaz/repspectre).

## Overarching goal

The long-term goal of this project is for RepSpectre Mobile to become the main interface for the [RepSpectre](https://github.com/MuzammilAijaz/repspectre) embedded system and the training data it collects.

The aim is to track individual repetitions and build a history of what actually happened at the barbell, including measurements that can provide an objective counterpart to a lifter's own assessment of effort such as RPE or RIR.

With enough history, that data could be used to look at how performance changes over time and help answer questions such as whether training volume or intensity should change and whether changes in performance are worth investigating.

The system could also use warm-up repetitions and previous performance to estimate what weight might be appropriate for a session. Longer-term trends could highlight downward changes in strength or performance for the lifter to review alongside recovery, training load, and other factors. The system would surface those trends; it would not establish their cause on its own.

These are the longer-term goals of the project. The current application is primarily focused on collecting and storing the data needed to eventually support them.

## Data flow

Here is what happens to sensor data collected via BLE.

```text
BLE GATT notification
        |
        v
AppService and GATT callback
        |
        v
SharedFlow of parsed sensor data
        |
        +--> Compose and ViewModel state
        |
        +--> Room-backed session records
```

The `FullIMURaw` format includes acceleration, gyroscope values, quaternion orientation, and a microsecond timestamp. It is the main format used by the collection and ML workflow.

## Repository layout

### Jetpack Compose and application code

```text
app/src/main/java/
  BLE/                  GATT scanning, connection, and notification parsing
  data/                 Repository, BLE command, and application wiring
  data/database/        Room database, entities, DAOs, and repositories
  domain/model/         Motion-state, lift-context, and format enums
  service/              Bound BLE service and high-frequency sensor flow
  ui/                   Jetpack Compose screens and view models
  raylib/               Kotlin JNI bridge and native activity entry point
```

## Android build

Open the project in Android Studio or build from the command line with an installed Android SDK, NDK, and a compatible JDK:

```sh
./gradlew :app:assembleDebug
```

The app targets Android API 24 or newer. BLE collection requires a physical Android device with the relevant Bluetooth permissions and a compatible peripheral advertising the service UUID expected by `AppService`.

The project configures the Android NDK through Gradle and uses CMake to build the native library.

## Raylib Native Screens

```text
app/src/main/cpp/
  src/                  raylib screens and desktop/Android native entry points
  deps/                 raylib and raymob dependencies
  resources/            models and shaders for the visualizer
```

The repository also contains an experimental native visualizer built with raylib and raymob. The aim is a stylized, PS2-era and Y2K-inspired way to present motion and orientation.

Compose remains responsible for collection setup, forms, and database-oriented workflows, where conventional Android controls are more appropriate. Raylib screens are intended for cases where a spatial display adds useful context; this separation acknowledges that a more distinctive interface can also make ordinary workflows harder to use.

The native project lives in `app/src/main/cpp`. It uses raylib and raymob to render an orientation visualizer, including a fixed virtual resolution so the same layout can be scaled onto different device sizes.

### Cross-platform compilation for running UI on desktop

There is also a desktop build path for working on the native UI without installing an APK:

```sh
cd app/src/main/cpp
make
```

Platform-specific compile-time paths allow the native UI to run on a desktop for quick visual iteration. Android lifecycle, BLE flow, and JNI-boundary testing still require a real device.
