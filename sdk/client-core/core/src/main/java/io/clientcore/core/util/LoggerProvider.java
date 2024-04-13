package io.clientcore.core.util;

public class LoggerProvider {
    Logger createLogger(String className);

    /**
     * Returns default implementation of {@code TracerProvider} that uses SPI to resolve tracing implementation.
     * @return an instance of {@code TracerProvider}
     */
    static LoggerProvider getProvider() {
        return DefaultTracerProvider.getInstance();
    }

}
