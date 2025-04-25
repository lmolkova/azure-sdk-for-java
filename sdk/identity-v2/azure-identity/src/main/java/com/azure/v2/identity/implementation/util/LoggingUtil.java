// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.v2.identity.implementation.util;

import com.azure.v2.core.credentials.TokenRequestContext;
import io.clientcore.core.instrumentation.logging.ClientLogger;
import io.clientcore.core.instrumentation.logging.ExceptionLoggingEvent;
import io.clientcore.core.instrumentation.logging.LogLevel;
import io.clientcore.core.utils.CoreUtils;
import io.clientcore.core.utils.configuration.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities to handle logging for credentials.
 */
public final class LoggingUtil {
    public static final String ERROR_MESSAGE = "Azure Identity => ERROR getToken()";
    private static final String SCOPE_KEY = "scopes";
    private static final String SUCCESS_MESSAGE = "Azure Identity => SUCCESS getToken()";

    /**
     * Log a success message for a getToken() call.
     * @param logger the logger to output the log message
     * @param context the context of the getToken() request
     */
    public static void logTokenSuccess(ClientLogger logger, TokenRequestContext context) {
        logger.atVerbose()
            .addKeyValue(SCOPE_KEY, () -> CoreUtils.stringJoin(", ", context.getScopes()))
            .log(SUCCESS_MESSAGE);
    }

    public static <T extends Throwable> T logTokenError(ExceptionLoggingEvent<T> errorLog, TokenRequestContext context,
        Throwable throwable) {
        return errorLog.addKeyValue(SCOPE_KEY, CoreUtils.stringJoin(", ", context.getScopes()))
            .log(ERROR_MESSAGE, throwable);
    }

    private LoggingUtil() {
    }

    /**
     * Log the names of the currently available environment variables among a list of useful environment variables for
     * Azure Identity authentications.
     *
     * @param logger the logger to output the log message
     * @param configuration the configuration store
     */
    public static void logAvailableEnvironmentVariables(ClientLogger logger, Configuration configuration) {
        if (logger.canLogAtLevel(LogLevel.VERBOSE)) {
            List<String> envVars = new ArrayList<>();
            if (configuration.get(IdentityUtil.PROPERTY_AZURE_CLIENT_ID) != null) {
                envVars.add(IdentityUtil.PROPERTY_AZURE_CLIENT_ID);
            }
            if (configuration.get(IdentityUtil.PROPERTY_AZURE_TENANT_ID) != null) {
                envVars.add(IdentityUtil.PROPERTY_AZURE_TENANT_ID);
            }
            if (configuration.get(IdentityUtil.PROPERTY_AZURE_CLIENT_SECRET) != null) {
                envVars.add(IdentityUtil.PROPERTY_AZURE_CLIENT_SECRET);
            }
            if (configuration.get(IdentityUtil.PROPERTY_AZURE_CLIENT_CERTIFICATE_PATH) != null) {
                envVars.add(IdentityUtil.PROPERTY_AZURE_CLIENT_CERTIFICATE_PATH);
            }
            logger.atVerbose()
                .addKeyValue("envVars", CoreUtils.stringJoin(", ", envVars))
                .log("Detected environment variables with credential information.");
        }
    }
}
