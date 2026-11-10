package com.example.stepbystep.sensor;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class StepCounterManager implements SensorEventListener {

    public enum Status {
        READY,
        NO_SENSOR,
        NO_PERMISSION
    }

    public static final String PERMISSION = Manifest.permission.ACTIVITY_RECOGNITION;

    private final Context context;
    private final SensorManager sensorManager;
    @Nullable private final Sensor stepCounterSensor;
    @Nullable private final Sensor stepDetectorSensor;

    @Nullable private StepCounterListener listener;

    private boolean sessionActive = false;
    private boolean registered = false;

    private long baseline = -1;
    private long lastRawValue = -1;
    private int carriedSteps = 0;
    private int sessionSteps = 0;

    private long startTimeMs = 0;
    private long endTimeMs = 0;

    public StepCounterManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        } else {
            stepCounterSensor = null;
            stepDetectorSensor = null;
        }
    }

    public void setListener(@Nullable StepCounterListener listener) {
        this.listener = listener;
    }

    public void start() {
        resetSessionSteps();
        sessionActive = true;
        startTimeMs = SystemClock.elapsedRealtime();
        endTimeMs = 0;
        registerSensor();
    }

    public void stop() {
        if (sessionActive) {
            endTimeMs = SystemClock.elapsedRealtime();
        }
        sessionActive = false;
        unregisterSensor();
    }

    public void resetSessionSteps() {
        baseline = -1;
        lastRawValue = -1;
        carriedSteps = 0;
        sessionSteps = 0;
        notifyListener();
    }

    public int getCurrentSteps() {
        return sessionSteps;
    }

    public void pause() {
        unregisterSensor();
    }

    public void resume() {
        if (sessionActive) {
            registerSensor();
        }
    }

    public boolean isSessionActive() {
        return sessionActive;
    }

    public boolean isSensorAvailable() {
        return stepCounterSensor != null || stepDetectorSensor != null;
    }

    public boolean isUsingStepCounter() {
        return stepCounterSensor != null;
    }

    public Status getStatus() {
        if (!isSensorAvailable()) return Status.NO_SENSOR;
        if (!hasPermission(context)) return Status.NO_PERMISSION;
        return Status.READY;
    }

    public long getElapsedMillis() {
        if (startTimeMs == 0) return 0;
        long end = sessionActive ? SystemClock.elapsedRealtime() : endTimeMs;
        return Math.max(0, end - startTimeMs);
    }

    public double getStepsPerMinute() {
        return calculateStepsPerMinute(sessionSteps, getElapsedMillis());
    }

    public static double calculateStepsPerMinute(int steps, long durationMillis) {
        if (durationMillis <= 0) return 0.0;
        return steps / (durationMillis / 60000.0);
    }

    public static boolean hasPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true;
        return ContextCompat.checkSelfPermission(context, PERMISSION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermission(@NonNull Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        ActivityCompat.requestPermissions(activity, new String[]{PERMISSION}, requestCode);
    }

    public void simulateSteps(int count) {
        if (!sessionActive || count <= 0) return;
        sessionSteps += count;
        notifyListener();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!sessionActive) return;

        int type = event.sensor.getType();
        if (type == Sensor.TYPE_STEP_COUNTER) {
            handleStepCounter((long) event.values[0]);
        } else if (type == Sensor.TYPE_STEP_DETECTOR) {
            sessionSteps++;
            notifyListener();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void handleStepCounter(long rawValue) {
        if (baseline < 0) {
            baseline = rawValue;
            lastRawValue = rawValue;
            notifyListener();
            return;
        }

        if (rawValue < lastRawValue) {
            carriedSteps = sessionSteps;
            baseline = rawValue;
        }
        lastRawValue = rawValue;

        int newSteps = carriedSteps + (int) (rawValue - baseline);
        if (newSteps != sessionSteps) {
            sessionSteps = newSteps;
            notifyListener();
        }
    }

    private void registerSensor() {
        if (registered || sensorManager == null) return;
        if (getStatus() != Status.READY) return;

        Sensor sensor = (stepCounterSensor != null) ? stepCounterSensor : stepDetectorSensor;
        registered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
    }

    private void unregisterSensor() {
        if (!registered || sensorManager == null) return;
        sensorManager.unregisterListener(this);
        registered = false;
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onStepCountChanged(sessionSteps);
        }
    }
}
