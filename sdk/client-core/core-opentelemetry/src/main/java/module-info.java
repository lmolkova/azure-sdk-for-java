// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import io.clientcore.core.opentelemetry.OpenTelemetryInstrumentationProvider;

module io.clientcore.core.opentelemetry  {
    requires io.clientcore.core;
    requires io.opentelemetry.api;
    requires io.opentelemetry.context;
    exports io.clientcore.core.opentelemetry;

    provides io.clientcore.core.util.LoggerProvider with OpenTelemetryInstrumentationProvider;
    provides io.clientcore.core.util.InstrumentationProvider with OpenTelemetryInstrumentationProvider;

    uses io.clientcore.core.util.LoggerProvider;
    uses io.clientcore.core.util.InstrumentationProvider;
}
