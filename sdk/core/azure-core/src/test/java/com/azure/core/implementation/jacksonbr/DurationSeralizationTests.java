package com.azure.core.implementation.jacksonbr;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DurationSeralizationTests {
    @Test
    public void testDuration() throws IOException {
        JSON.registerSerializer(Duration.class, new DurationBeanWriter());
        assertEquals("", JSON.writeVal(null));
        assertEquals("\"PT0S\"", JSON.writeVal(Duration.ofMillis(0)));
        assertEquals("\"PT0.001S\"", JSON.writeVal(Duration.ofMillis(1)));
        assertEquals("\"PT0.009S\"", JSON.writeVal(Duration.ofMillis(9)));
        assertEquals("\"PT0.01S\"", JSON.writeVal(Duration.ofMillis(10)));
        assertEquals("\"PT0.999S\"", JSON.writeVal(Duration.ofMillis(999)));
        assertEquals("\"PT0.011S\"", JSON.writeVal(Duration.ofMillis(11)));
        assertEquals("\"PT0.099S\"", JSON.writeVal(Duration.ofMillis(99)));
        assertEquals("\"PT0.1S\"", JSON.writeVal(Duration.ofMillis(100)));
        assertEquals("\"PT0.101S\"", JSON.writeVal(Duration.ofMillis(101)));
        assertEquals("\"PT0.999S\"", JSON.writeVal(Duration.ofMillis(999)));
        assertEquals("\"PT1S\"", JSON.writeVal(Duration.ofMillis(1000)));
        assertEquals("\"PT1S\"", JSON.writeVal(Duration.ofSeconds(1)));
        assertEquals("\"PT9S\"", JSON.writeVal(Duration.ofSeconds(9)));
        assertEquals("\"PT10S\"", JSON.writeVal(Duration.ofSeconds(10)));
        assertEquals("\"PT11S\"", JSON.writeVal(Duration.ofSeconds(11)));
        assertEquals("\"PT59S\"", JSON.writeVal(Duration.ofSeconds(59)));
        assertEquals("\"PT1M\"", JSON.writeVal(Duration.ofSeconds(60)));
        assertEquals("\"PT1M1S\"", JSON.writeVal(Duration.ofSeconds(61)));
        assertEquals("\"PT1M\"", JSON.writeVal(Duration.ofMinutes(1)));
        assertEquals("\"PT9M\"", JSON.writeVal(Duration.ofMinutes(9)));
        assertEquals("\"PT10M\"", JSON.writeVal(Duration.ofMinutes(10)));
        assertEquals("\"PT11M\"", JSON.writeVal(Duration.ofMinutes(11)));
        assertEquals("\"PT59M\"", JSON.writeVal(Duration.ofMinutes(59)));
        assertEquals("\"PT1H\"", JSON.writeVal(Duration.ofMinutes(60)));
        assertEquals("\"PT1H1M\"", JSON.writeVal(Duration.ofMinutes(61)));
        assertEquals("\"PT1H\"", JSON.writeVal(Duration.ofHours(1)));
        assertEquals("\"PT9H\"", JSON.writeVal(Duration.ofHours(9)));
        assertEquals("\"PT10H\"", JSON.writeVal(Duration.ofHours(10)));
        assertEquals("\"PT11H\"", JSON.writeVal(Duration.ofHours(11)));
        assertEquals("\"PT23H\"", JSON.writeVal(Duration.ofHours(23)));
        assertEquals("\"P1D\"", JSON.writeVal(Duration.ofHours(24)));
        assertEquals("\"P1DT1H\"", JSON.writeVal(Duration.ofHours(25)));
        assertEquals("\"P1D\"", JSON.writeVal(Duration.ofDays(1)));
        assertEquals("\"P9D\"", JSON.writeVal(Duration.ofDays(9)));
        assertEquals("\"P10D\"", JSON.writeVal(Duration.ofDays(10)));
        assertEquals("\"P11D\"", JSON.writeVal(Duration.ofDays(11)));
        assertEquals("\"P99D\"", JSON.writeVal(Duration.ofDays(99)));
        assertEquals("\"P100D\"", JSON.writeVal(Duration.ofDays(100)));
        assertEquals("\"P101D\"", JSON.writeVal(Duration.ofDays(101)));
    }
}
