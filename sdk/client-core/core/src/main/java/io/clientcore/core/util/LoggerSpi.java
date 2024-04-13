package io.clientcore.core.util;

import java.util.List;

public interface LoggerSpi {
    public boolean isEnabled(ClientLogger.LogLevel level);
    public void log(ClientLogger.LogLevel level, String body, Throwable throwable, List<ClientLogger.LoggingAttribute> attributes, Context context);
}
