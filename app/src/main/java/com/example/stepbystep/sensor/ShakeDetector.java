package com.example.stepbystep.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * TEMPORARY placeholder so MainActivity compiles before Person 2 delivers.
 * Replace this file with Person 2's ShakeDetector (same public API:
 * constructor(Context), setListener, start, stop).
 */
public class ShakeDetector implements SensorEventListener {

    private static final float THRESHOLD_G = 2.5f;
    private static final long COOLDOWN_MS = 500;

    private final SensorManager sensorManager;
    @Nullable private final Sensor accelerometer;
    @Nullable private ShakeListener listener;
    private boolean registered = false;
    private long lastShakeMs = 0;

    public ShakeDetector(@NonNull Context context) {
        sensorManager = (SensorManager) context.getApplicationContext()
                .getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager == null ? null
                : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void setListener(@Nullable ShakeListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (registered || sensorManager == null || accelerometer == null) return;
        registered = sensorManager.registerListener(
                this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        if (!registered || sensorManager == null) return;
        sensorManager.unregisterListener(this);
        registered = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
        float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
        float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;
        double gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce < THRESHOLD_G) return;

        long now = SystemClock.elapsedRealtime();
        if (now - lastShakeMs < COOLDOWN_MS) return;
        lastShakeMs = now;

        if (listener != null) listener.onShakeDetected();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
