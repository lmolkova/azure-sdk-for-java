package io.clientcore.core.opentelemetry;

import io.clientcore.core.util.InstrumentationOptions;
import io.opentelemetry.api.OpenTelemetry;

public class OpenTelemetryInstrumentationOptions extends InstrumentationOptions {
    private OpenTelemetry openTelemetry;
    public OpenTelemetryInstrumentationOptions() {
        super(OpenTelemetryInstrumentationProvider.class);
    }

    public OpenTelemetryInstrumentationOptions setOpenTelemetry(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        return this;
    }

    public OpenTelemetry getOpenTelemetry() {
        return openTelemetry;
    }
}
