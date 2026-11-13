package com.example.stepbystep.sensor;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Unit tests for the Person 2 algorithm; requires no emulator. */
public class ShakeSignalProcessorTest {

    private static final float G = 9.80665f;
    private ShakeSignalProcessor processor;

    @Before
    public void setUp() {
        processor = new ShakeSignalProcessor();
    }

    @Test
    public void stillPhoneAndSmallMovementDoNotCount() {
        assertFalse(processor.addSample(0, 0, G, 0));
        for (int i = 1; i <= 50; i++) {
            assertFalse(processor.addSample(0.25f * (i % 2), 0, G, i * 20_000_000L));
        }
    }

    @Test
    public void strongImpulseCountsOnceAndRequiresRelease() {
        assertFalse(processor.addSample(0, 0, G, 0));
        assertTrue(processor.addSample(30, 0, G, 20_000_000L));
        assertFalse(processor.addSample(-30, 0, G, 40_000_000L));
        assertFalse(processor.addSample(30, 0, G, 60_000_000L));
    }

    @Test
    public void releaseWithinCooldownDoesNotDoubleCount() {
        assertFalse(processor.addSample(0, 0, G, 0));
        assertTrue(processor.addSample(30, 0, G, 20_000_000L));
        assertFalse(processor.addSample(0, 0, G, 100_000_000L));
        assertFalse(processor.addSample(30, 0, G, 150_000_000L));
    }

    @Test
    public void anotherShakeAfterCooldownCounts() {
        assertFalse(processor.addSample(0, 0, G, 0));
        assertTrue(processor.addSample(30, 0, G, 20_000_000L));
        for (int i = 1; i <= 25; i++) {
            assertFalse(processor.addSample(0, 0, G, 20_000_000L + i * 20_000_000L));
        }
        assertTrue(processor.addSample(30, 0, G, 600_000_000L));
    }

    @Test
    public void resetAllowsNewSessionAndOutOfOrderSamplesAreIgnored() {
        assertFalse(processor.addSample(0, 0, G, 0));
        assertTrue(processor.addSample(30, 0, G, 20_000_000L));
        assertFalse(processor.addSample(30, 0, G, 10_000_000L));
        processor.reset();
        assertFalse(processor.addSample(0, 0, G, 30_000_000L));
        assertTrue(processor.addSample(30, 0, G, 50_000_000L));
    }

    @Test
    public void invalidSamplesAreIgnored() {
        assertFalse(processor.addSample(Float.NaN, 0, G, 0));
        assertFalse(processor.addSample(Float.POSITIVE_INFINITY, 0, G, 10_000_000L));
        assertFalse(processor.addSample(0, 0, G, 20_000_000L));
        assertTrue(processor.addSample(30, 0, G, 40_000_000L));
    }
}
