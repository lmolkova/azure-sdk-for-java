package io.clientcore.core.opentelemetry.implementation;

import io.clientcore.core.util.Context;

public class OTelUtils {
    private OTelUtils() {
    }

    public static io.opentelemetry.context.Context toOtelContext(Object context) {
        if (context instanceof io.opentelemetry.context.Context) {
            return (io.opentelemetry.context.Context) context;
        }

        return null;
    }
}
