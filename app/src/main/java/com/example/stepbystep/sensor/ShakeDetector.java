package com.example.stepbystep.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Person 2 - Accelerometer shake detector.
 *
 * Public API deliberately matches the existing MainActivity:
 * new ShakeDetector(context), setListener(...), start(), stop().
 * Counts and session duration belong to SessionManager, not this class.
 */
public class ShakeDetector implements SensorEventListener {

    @Nullable private final SensorManager sensorManager;
    @Nullable private final Sensor accelerometer;
    @NonNull private final ShakeSignalProcessor processor = new ShakeSignalProcessor();
    @Nullable private ShakeListener listener;
    private boolean registered;

    public ShakeDetector(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        sensorManager = (SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager == null
                ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void setListener(@Nullable ShakeListener listener) {
        this.listener = listener;
    }

    /** A device without an accelerometer can still run the step module. */
    public boolean isSensorAvailable() {
        return accelerometer != null;
    }

    public boolean isRunning() {
        return registered;
    }

    /** Start listening once. Check isRunning() to detect registration failure. */
    public void start() {
        if (registered || sensorManager == null || accelerometer == null) return;

        // On a new start (including resume) re-learn gravity and avoid
        // using a previous cooldown timestamp from another sensor stream.
        processor.reset();
        registered = sensorManager.registerListener(
                this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    /** Stop listening so the sensor does not waste battery in the background. */
    public void stop() {
        if (registered && sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        registered = false;
        processor.reset();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!registered || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER
                || event.values.length < 3) {
            return;
        }

        if (processor.addSample(event.values[0], event.values[1],
                event.values[2], event.timestamp)) {
            if (listener != null) {
                listener.onShakeDetected();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action needed for shake counting.
    }
}
