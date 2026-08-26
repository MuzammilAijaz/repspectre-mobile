package com.example.esp32_mpu6050_mobile_data_collection.domain.model

enum class MotionStates(val description: String, val requiresLiftContext: Boolean = false) {

    //===== Lift Sessions ==========================================================

    /** The default MotionState for Lift Sessions */
    REP_START(
        description = "The point at which a new repetition begins.",
        requiresLiftContext = true
    ),

    //===== Lift Specific Noise Sessions ===========================================

    /**
     * Lift Specific MotionStates require context on what lift is being performed.
     *
     * For i.e. UNRACK_TRANSIENT is different for every type of [LiftCategories],
     * like squats (unracked from a squat rack) vs barbell curls (unranked from a rack)
     */

    SETUP_MOVEMENT(
        description = "Movement performed to position the lifter or barbell before the repetition begins.",
        requiresLiftContext = true
    ),
    UNRACK_TRANSIENT(
        description = "The brief movement caused by removing the barbell from the rack before the lift begins.",
        requiresLiftContext = true
    ),
    RERACK_TRANSIENT(
        description = "The brief movement caused by returning the barbell to the rack after the lift.",
        requiresLiftContext = true
    ),

    //===== Noise Sessions =========================================================

    SENSOR_JITTER(
        description = "Small, rapid fluctuations in the sensor signal that are not meaningful physical movement."
    ),
    SENSOR_DRIFT(
        description = "A gradual change in the estimated sensor state caused by accumulated bias or integration error rather than actual movement."
    ),
    SENSOR_VIBRATION(
        description = "Rapid oscillation detected by the sensor, typically occurring at a higher frequency than the intended barbell movement."
    ),

    BARBELL_ROLLING(
        description = "The barbell rotates or rolls while remaining approximately in place, such as when resting on the floor."
    ),
    BARBELL_MICROMOTION(
        description = "Small, intentional or incidental barbell movement that does not constitute a meaningful lifting movement."
    ),
    BARBELL_IMPACT(
        description = "A sudden, high-magnitude movement caused by the barbell contacting another object or surface."
    ),

    EXTERNAL_DISTURBANCE(
        description = "Movement of the barbell caused by an external force rather than the lifter's intended movement."
    ),
    PLATFORM_VIBRATION(
        description = "Vibration transmitted through the floor, platform, rack, or other supporting structure."
    ),
    BACKGROUND_GYM_MOTION(
        description = "Sensor movement caused by activity elsewhere in the gym rather than the barbell itself."
    ),
    BARBELL_STATIONARY(
        description = "The barbell is sufficiently motionless to be considered stationary within the system's measurement tolerance."
    )
}

enum class LiftCategories {
    FLOOR_PULL,
    SQUAT,
    PRESS_HORIZONTAL,
    PRESS_VERTICAL,
    UPRIGHT_PULL,
    ROW,
    OTHER
}

enum class Tempos {
    NORMAL,
    EXPLOSIVE,
    FAST,
    CONTROLLED,
    SLOW_TEMPO,
    PAUSED,
    ECCENTRIC_EMPHASIS,
    CONCENTRIC_EMPHASIS
}

enum class SensorDataFormats {
    ACCELERATION_RAW,
    GYROSCOPE_RAW,
    MAGNETOMETER_RAW,
    QUATERNION_ORIENTATION,
    LINEAR_ACCELERATION,
    FULL_IMU_RAW,
    FULL_IMU_PROCESSED,
    ALL_FEATURES
}

enum class DeviceStates {
    BARBELL_STATIONARY,
    DEVICE_CARRYING,
    DEVICE_SETUP,
    DEVICE_LOADING,
    LIFT_SETUP,
    UNRACK,
    REP_START
}
