// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.implementation.util;

/**
 * Constants used as keys in semantic logging. Logging keys unify
 * how core logs HTTP requests, responses or anything else and simplify log analysis.
 *
 * When logging in client libraries, please do the best effort to stay consistent
 * with these keys, but copy the value.
 */
public final class LoggingKeys {
    private LoggingKeys() {

    }

    /**
     * Key representing HTTP method.
     */
    public static final String HTTP_METHOD_KEY = "http.request.method";

    /**
     * Key representing try count, the value starts with {@code 0} on the first try
     * and should be an {@code int} number.
     */
    public static final String TRY_COUNT_KEY = "http.request.resend_count";

    /**
     * Key representing duration of call in milliseconds, the value should be a number.
     */
    public static final String DURATION_MS_KEY = "http.client.request.duration";


    /**
     * Key representing request URL.
     */
    public static final String URL_KEY = "url.full";

    /**
     * Key representing request body content length.
     */
    public static final String REQUEST_CONTENT_LENGTH_KEY = "http.request.body.size";
    public static final String RESPONSE_CONTENT_LENGTH_KEY = "http.response.body.size";
    /**
     * Key representing request body. The value should be populated conditionally
     * if populated at all.
     */
    public static final String BODY_KEY = "http.request.body.content";

    /**
     * Key representing response status code. The value should be a number.
     */
    public static final String STATUS_CODE_KEY = "http.response.status_code";
}
