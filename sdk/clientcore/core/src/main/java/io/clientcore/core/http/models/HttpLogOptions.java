// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.http.models;

import io.clientcore.core.util.configuration.Configuration;
import io.clientcore.core.util.configuration.ConfigurationProperty;
import io.clientcore.core.util.configuration.ConfigurationPropertyBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The log configurations for HTTP messages.
 */
public final class HttpLogOptions {
    private boolean isLoggingEnabled;
    private boolean isContentLoggingEnabled;
    private boolean isRedactedHeaderNamesLoggingEnabled;
    private Set<HttpHeaderName> allowedHeaderNames;
    private Set<String> allowedQueryParamNames;
    private static final List<HttpHeaderName> DEFAULT_HEADERS_ALLOWLIST
        = Arrays.asList(HttpHeaderName.TRACEPARENT, HttpHeaderName.ACCEPT, HttpHeaderName.CACHE_CONTROL,
            HttpHeaderName.CONNECTION, HttpHeaderName.CONTENT_LENGTH, HttpHeaderName.CONTENT_TYPE, HttpHeaderName.DATE,
            HttpHeaderName.ETAG, HttpHeaderName.EXPIRES, HttpHeaderName.IF_MATCH, HttpHeaderName.IF_MODIFIED_SINCE,
            HttpHeaderName.IF_NONE_MATCH, HttpHeaderName.IF_UNMODIFIED_SINCE, HttpHeaderName.LAST_MODIFIED,
            HttpHeaderName.PRAGMA, HttpHeaderName.RETRY_AFTER, HttpHeaderName.SERVER, HttpHeaderName.TRANSFER_ENCODING,
            HttpHeaderName.USER_AGENT, HttpHeaderName.WWW_AUTHENTICATE);

    private static final List<String> DEFAULT_QUERY_PARAMS_ALLOWLIST = Collections.singletonList("api-version");

    private static final ConfigurationProperty<Boolean> HTTP_LOGGING_ENABLED
        = ConfigurationPropertyBuilder.ofBoolean("http.logging.enabled")
            .shared(true)
            .environmentVariableName(Configuration.PROPERTY_HTTP_LOGGING_ENABLED)
            .defaultValue(false)
            .build();

    private static final boolean DEFAULT_LOGGING_ENABLED
        = Configuration.getGlobalConfiguration().get(HTTP_LOGGING_ENABLED);

    /**
     * Creates a new instance that does not log any information about HTTP requests or responses.
     */
    public HttpLogOptions() {
        isLoggingEnabled = DEFAULT_LOGGING_ENABLED;
        isContentLoggingEnabled = false;
        isRedactedHeaderNamesLoggingEnabled = true;
        allowedHeaderNames = new HashSet<>(DEFAULT_HEADERS_ALLOWLIST);
        allowedQueryParamNames = new HashSet<>(DEFAULT_QUERY_PARAMS_ALLOWLIST);
    }

    /**
     * Flag indicating whether HTTP request and response logging is enabled.
     * False by default.
     * <p>
     * When HTTP logging is disabled, basic information about the request and response is still recorded
     * via distributed tracing.
     *
     * @return True if logging is enabled, false otherwise.
     */
    public boolean isLoggingEnabled() {
        return isLoggingEnabled;
    }

    /**
     * Flag indicating whether HTTP request and response header values are added to the logs
     * when their name is not explicitly allowed via {@link HttpLogOptions#setAllowedHeaderNames(Set)} or
     * {@link HttpLogOptions#addAllowedHeaderName(HttpHeaderName)}.
     * True by default.
     *
     * @return True if redacted header names logging is enabled, false otherwise.
     */
    public boolean isRedactedHeaderNamesLoggingEnabled() {
        return isRedactedHeaderNamesLoggingEnabled;
    }

    /**
     * Enables or disables logging of redacted header names.
     * @param redactedHeaderNamesLoggingEnabled True to enable logging of redacted header names, false otherwise.
     *                                          Default is true.
     * @return The updated {@link HttpLogOptions} object.
     */
    public HttpLogOptions setRedactedHeaderNamesLoggingEnabled(boolean redactedHeaderNamesLoggingEnabled) {
        isRedactedHeaderNamesLoggingEnabled = redactedHeaderNamesLoggingEnabled;
        return this;
    }

    /**
     * Flag indicating whether HTTP request and response body is logged.
     * False by default.
     * <p>
     * Note: even when content logging is explicitly enabled, it's not logged in the
     * following cases:
     * <ul>
     *     <li>When the content length is not known.</li>
     *     <li>When the content length is greater than 16KB.</li>
     * </ul>
     *
     * @return True if content logging is enabled, false otherwise.
     */
    public boolean isContentLoggingEnabled() {
        return isContentLoggingEnabled;
    }

    /**
     * Enables or disables logging of HTTP request and response.
     * False by default.
     *
     * When HTTP logging is disabled, basic information about the request and response is still recorded
     * via distributed tracing.
     *
     * @param isLoggingEnabled True to enable logging, false otherwise.
     * @return The updated {@link HttpLogOptions} object.
     */
    public HttpLogOptions setLoggingEnabled(boolean isLoggingEnabled) {
        this.isLoggingEnabled = isLoggingEnabled;
        return this;
    }

    /**
     * Enables or disables logging of HTTP request and response body.
     * False by default.
     * <p>
     * Note: even when content logging is explicitly enabled, it's not logged in the
     * following cases:
     * <ul>
     *     <li>When the content length is not known.</li>
     *     <li>When the content length is greater than 16KB.</li>
     * </ul>
     *
     * @param isContentLoggingEnabled True to enable content logging, false otherwise.
     * @return The updated {@link HttpLogOptions} object.
     */
    public HttpLogOptions setContentLoggingEnabled(boolean isContentLoggingEnabled) {
        this.isLoggingEnabled |= isContentLoggingEnabled;
        this.isContentLoggingEnabled = isContentLoggingEnabled;
        return this;
    }

    /**
     * Gets the allowed headers that should be logged.
     *
     * @return The list of allowed headers.
     */
    public Set<HttpHeaderName> getAllowedHeaderNames() {
        return Collections.unmodifiableSet(allowedHeaderNames);
    }

    /**
     * Sets the given allowed headers that should be logged.
     *
     * <p>
     * This method sets the provided header names to be the allowed header names which will be logged for all HTTP
     * requests and responses, overwriting any previously configured headers. Additionally, users can use
     * {@link HttpLogOptions#addAllowedHeaderName(HttpHeaderName)} or {@link HttpLogOptions#getAllowedHeaderNames()} to add or
     * remove more headers names to the existing set of allowed header names.
     * </p>
     *
     * @param allowedHeaderNames The list of allowed header names from the user.
     *
     * @return The updated HttpLogOptions object.
     */
    public HttpLogOptions setAllowedHeaderNames(final Set<HttpHeaderName> allowedHeaderNames) {
        this.allowedHeaderNames = allowedHeaderNames == null ? new HashSet<>() : allowedHeaderNames;

        return this;
    }

    /**
     * Sets the given allowed header to the default header set that should be logged.
     *
     * @param allowedHeaderName The allowed header name from the user.
     *
     * @return The updated HttpLogOptions object.
     *
     * @throws NullPointerException If {@code allowedHeaderName} is {@code null}.
     */
    public HttpLogOptions addAllowedHeaderName(final HttpHeaderName allowedHeaderName) {
        Objects.requireNonNull(allowedHeaderName);
        this.allowedHeaderNames.add(allowedHeaderName);

        return this;
    }

    /**
     * Gets the allowed query parameters.
     *
     * @return The list of allowed query parameters.
     */
    public Set<String> getAllowedQueryParamNames() {
        return Collections.unmodifiableSet(allowedQueryParamNames);
    }

    /**
     * Sets the given allowed query params to be displayed in the logging info.
     *
     * @param allowedQueryParamNames The list of allowed query params from the user.
     *
     * @return The updated HttpLogOptions object.
     */
    public HttpLogOptions setAllowedQueryParamNames(final Set<String> allowedQueryParamNames) {
        this.allowedQueryParamNames = allowedQueryParamNames == null ? new HashSet<>() : allowedQueryParamNames;

        return this;
    }

    /**
     * Sets the given allowed query param that should be logged.
     *
     * @param allowedQueryParamName The allowed query param name from the user.
     *
     * @return The updated HttpLogOptions object.
     *
     * @throws NullPointerException If {@code allowedQueryParamName} is {@code null}.
     */
    public HttpLogOptions addAllowedQueryParamName(final String allowedQueryParamName) {
        this.allowedQueryParamNames.add(allowedQueryParamName);
        return this;
    }
}
