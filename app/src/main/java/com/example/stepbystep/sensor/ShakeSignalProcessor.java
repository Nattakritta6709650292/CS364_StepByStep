package com.example.stepbystep.sensor;

/**
 * Pure Java shake classifier. Receives accelerometer data (m/s^2) and the
 * SensorEvent timestamp (nanoseconds, monotonic), so it can be unit tested
 * without Android hardware.
 *
 * 1) Low-pass the acceleration to estimate gravity.
 * 2) Subtract gravity and measure linear acceleration in g.
 * 3) Trigger only on a strong peak, require a release before the next peak,
 *    and enforce a 500 ms cooldown to avoid double counting.
 *
 * The constants are practical defaults; tune on real devices if necessary.
 */
final class ShakeSignalProcessor {

    private static final float EARTH_GRAVITY = 9.80665f;
    private static final float GRAVITY_ALPHA = 0.90f;
    private static final float TRIGGER_G = 1.60f;
    private static final float RELEASE_G = 0.65f;
    private static final long COOLDOWN_NS = 500_000_000L;

    private float gravityX;
    private float gravityY;
    private float gravityZ;

    private boolean initialized;
    private boolean armed;
    private long lastSampleTimestampNs;
    private long lastShakeTimestampNs;

    ShakeSignalProcessor() {
        reset();
    }

    void reset() {
        gravityX = gravityY = gravityZ = 0f;
        initialized = false;
        armed = true;
        lastSampleTimestampNs = Long.MIN_VALUE;
        lastShakeTimestampNs = Long.MIN_VALUE;
    }

    /** Returns true exactly when a new shake event should be counted. */
    boolean addSample(float x, float y, float z, long timestampNs) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            return false;
        }
        if (lastSampleTimestampNs != Long.MIN_VALUE
                && timestampNs <= lastSampleTimestampNs) {
            return false;
        }
        lastSampleTimestampNs = timestampNs;

        // Calibrate the initial gravity vector. Do not report a shake on the
        // first sample (the phone may already be held at an angle).
        if (!initialized) {
            gravityX = x;
            gravityY = y;
            gravityZ = z;
            initialized = true;
            return false;
        }

        gravityX = GRAVITY_ALPHA * gravityX + (1f - GRAVITY_ALPHA) * x;
        gravityY = GRAVITY_ALPHA * gravityY + (1f - GRAVITY_ALPHA) * y;
        gravityZ = GRAVITY_ALPHA * gravityZ + (1f - GRAVITY_ALPHA) * z;

        float linearX = x - gravityX;
        float linearY = y - gravityY;
        float linearZ = z - gravityZ;
        double linearG = Math.sqrt(linearX * linearX
                + linearY * linearY + linearZ * linearZ) / EARTH_GRAVITY;

        // Hysteresis: wait for acceleration to fall before rearming.
        if (linearG <= RELEASE_G) {
            armed = true;
            return false;
        }
        if (!armed || linearG < TRIGGER_G) {
            return false;
        }

        if (lastShakeTimestampNs != Long.MIN_VALUE
                && timestampNs - lastShakeTimestampNs < COOLDOWN_NS) {
            return false;
        }

        armed = false;
        lastShakeTimestampNs = timestampNs;
        return true;
    }
}
