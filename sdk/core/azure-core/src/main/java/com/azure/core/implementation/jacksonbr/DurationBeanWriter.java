// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.Duration;

/**
 * Custom serializer for serializing {@link Duration} object into ISO8601 formats.
 */
public final class DurationBeanWriter implements BeanWriter<Duration> {

    @Override
    public void writeValue(Duration value, JsonGenerator g, JSONWriter context) throws IOException {
        g.writeString(DurationBeanWriter.toString(value));
    }

    /**
     * Convert to provided Duration to an ISO 8601 String with a days component.
     * @param duration The Duration to convert.
     * @return The String representation of the provided Duration.
     */
    public static String toString(Duration duration) {
        String result = null;
        if (duration != null) {
            if (duration.isZero()) {
                result = "PT0S";
            } else {
                final StringBuilder builder = new StringBuilder();

                builder.append('P');

                final long days = duration.toDays();
                if (days > 0) {
                    builder.append(days);
                    builder.append('D');
                    duration = duration.minusDays(days);
                }

                final long hours = duration.toHours();
                if (hours > 0) {
                    builder.append('T');
                    builder.append(hours);
                    builder.append('H');
                    duration = duration.minusHours(hours);
                }

                final long minutes = duration.toMinutes();
                if (minutes > 0) {
                    if (hours == 0) {
                        builder.append('T');
                    }

                    builder.append(minutes);
                    builder.append('M');
                    duration = duration.minusMinutes(minutes);
                }

                final long seconds = duration.getSeconds();
                if (seconds > 0) {
                    if (hours == 0 && minutes == 0) {
                        builder.append('T');
                    }

                    builder.append(seconds);
                    duration = duration.minusSeconds(seconds);
                }

                long milliseconds = duration.toMillis();
                if (milliseconds > 0) {
                    if (hours == 0 && minutes == 0 && seconds == 0) {
                        builder.append("T");
                    }

                    if (seconds == 0) {
                        builder.append("0");
                    }

                    builder.append('.');

                    if (milliseconds <= 99) {
                        builder.append('0');

                        if (milliseconds <= 9) {
                            builder.append('0');
                        }
                    }

                    // Remove trailing zeros.
                    while (milliseconds % 10 == 0) {
                        milliseconds /= 10;
                    }
                    builder.append(milliseconds);
                }

                if (seconds > 0 || milliseconds > 0) {
                    builder.append('S');
                }

                result = builder.toString();
            }
        }
        return result;
    }

}
