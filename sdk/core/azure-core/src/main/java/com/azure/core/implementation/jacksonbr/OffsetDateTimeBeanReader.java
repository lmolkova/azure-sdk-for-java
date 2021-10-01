// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.UnixTime;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.datatype.jsr310.DecimalUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Custom deserializer for deserializing epoch formats into {@link UnixTime} objects.
 */
public final class OffsetDateTimeBeanReader extends ValueReader<OffsetDateTime> {

    private static final ZoneId DEFAULT_TIMEZONE_ID = TimeZone.getTimeZone("UTC").toZoneId();
    private static final Pattern ISO8601_UTC_ZERO_OFFSET_SUFFIX_REGEX = Pattern.compile("\\+00:?(00)?$");
    private final boolean replaceZeroOffsetAsZ;
    private final DateTimeFormatter _formatter;

    public final static OffsetDateTimeBeanReader INSTANCE = new OffsetDateTimeBeanReader(DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        OffsetDateTime::from,
        a -> OffsetDateTime.ofInstant(Instant.ofEpochMilli(a.value), a.zoneId),
        a -> OffsetDateTime.ofInstant(Instant.ofEpochSecond(a.integer, a.fraction), a.zoneId),
        (d, z) -> (d.isEqual(OffsetDateTime.MIN) || d.isEqual(OffsetDateTime.MAX) ? d : d.withOffsetSameInstant(z.getRules().getOffset(d.toLocalDateTime()))),
        true // yes, replace zero offset with Z
        );

    private final Function<FromIntegerArguments, OffsetDateTime> fromMilliseconds;

    private final Function<FromDecimalArguments, OffsetDateTime> fromNanoseconds;

    private final Function<TemporalAccessor, OffsetDateTime> parsedToValue;

    @Override
    public OffsetDateTime read(JsonParser parser) throws IOException {
        //NOTE: Timestamps contain no timezone info, and are always in configured TZ. Only
        //string values have to be adjusted to the configured TZ.
        switch (parser.currentTokenId()) {
            case JsonTokenId.ID_NUMBER_FLOAT:
                return _fromDecimal(parser.getDecimalValue());
            case JsonTokenId.ID_NUMBER_INT:
                return _fromLong(parser.getLongValue());
            case JsonTokenId.ID_STRING:
                return _fromString(parser, parser.getText());
            case JsonTokenId.ID_START_OBJECT:
                return _fromString(parser,
                    _handleUnexpectedToken(parser, JsonToken.VALUE_STRING,
                        JsonToken.VALUE_NUMBER_INT, JsonToken.VALUE_NUMBER_FLOAT));
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                return (OffsetDateTime) parser.getEmbeddedObject();
            case JsonTokenId.ID_START_ARRAY:
                return _handleUnexpectedToken(parser);//stdReader.._deserializeFromArray(parser);
        }
        return _handleUnexpectedToken(parser, JsonToken.VALUE_STRING,
            JsonToken.VALUE_NUMBER_INT, JsonToken.VALUE_NUMBER_FLOAT);
    }

    private OffsetDateTimeBeanReader(DateTimeFormatter formatter,
                                     Function<TemporalAccessor, OffsetDateTime> parsedToValue,
                                     Function<FromIntegerArguments, OffsetDateTime> fromMilliseconds,
                                     Function<FromDecimalArguments, OffsetDateTime> fromNanoseconds,
                                     BiFunction<OffsetDateTime, ZoneId, OffsetDateTime> adjust,
                                     boolean replaceZeroOffsetAsZ)
    {
        super(OffsetDateTime.class);
        _formatter = formatter;
        //super(supportedType, formatter);
        this.parsedToValue = parsedToValue;
        this.fromMilliseconds = fromMilliseconds;
        this.fromNanoseconds = fromNanoseconds;
        this.replaceZeroOffsetAsZ = replaceZeroOffsetAsZ;
    }

    // Helper method to find Strings of form "all digits" and "digits-comma-digits"
    private int _countPeriods(String str)
    {
        int commas = 0;
        for (int i = 0, end = str.length(); i < end; ++i) {
            int ch = str.charAt(i);
            if (ch < '0' || ch > '9') {
                if (ch == '.') {
                    ++commas;
                } else {
                    return -1;
                }
            }
        }
        return commas;
    }

    private OffsetDateTime _fromString(JsonParser p, String string0)  throws IOException
    {
        String string = string0.trim();
        if (string.length() == 0) {
            return _fromEmptyString(p, string);
        }

        int dots = _countPeriods(string);
        if (dots >= 0) { // negative if not simple number
            try {
                if (dots == 0) {
                    return _fromLong(Long.parseLong(string));
                }
                if (dots == 1) {
                    return _fromDecimal(new BigDecimal(string));
                }
            } catch (NumberFormatException e) {
                // fall through to default handling, to get error there
            }
        }

        string = replaceZeroOffsetAsZIfNecessary(string);

        OffsetDateTime value;
        try {
            TemporalAccessor acc = _formatter.parse(string);
            value = parsedToValue.apply(acc);
        } catch (DateTimeException e) {
            return null;
        }
        return value;
    }

    private OffsetDateTime _fromLong(long timestamp)
    {
        return fromMilliseconds.apply(new FromIntegerArguments(
            timestamp, DEFAULT_TIMEZONE_ID));
    }

    private OffsetDateTime _fromDecimal(BigDecimal value)
    {
        FromDecimalArguments args =
            DecimalUtils.extractSecondsAndNanos(value, (s, ns) -> new FromDecimalArguments(s, ns, DEFAULT_TIMEZONE_ID));
        return fromNanoseconds.apply(args);
    }


    private String replaceZeroOffsetAsZIfNecessary(String text)
    {
        if (replaceZeroOffsetAsZ) {
            return ISO8601_UTC_ZERO_OFFSET_SUFFIX_REGEX.matcher(text).replaceFirst("Z");
        }

        return text;
    }

    private OffsetDateTime _fromEmptyString(JsonParser p, String str) throws IOException
    {
        return null;
    }

    private <R> R _handleUnexpectedToken(JsonParser parser, JsonToken... expTypes)  {
        return null;
    }

    private static class FromIntegerArguments // since 2.8.3
    {
        public final long value;
        public final ZoneId zoneId;

        FromIntegerArguments(long value, ZoneId zoneId)
        {
            this.value = value;
            this.zoneId = zoneId;
        }
    }

    private static class FromDecimalArguments // since 2.8.3
    {
        public final long integer;
        public final int fraction;
        public final ZoneId zoneId;

        FromDecimalArguments(long integer, int fraction, ZoneId zoneId)
        {
            this.integer = integer;
            this.fraction = fraction;
            this.zoneId = zoneId;
        }
    }
}
