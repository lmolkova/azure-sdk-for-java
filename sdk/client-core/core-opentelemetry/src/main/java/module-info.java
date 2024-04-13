// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

module io.clientcore.core.opentelemetry  {
    requires io.clientcore.core;
    requires io.opentelemetry.api;
    requires io.opentelemetry.context;
    exports io.clientcore.core.opentelemetry.implementation to io.clientcore.core;

    uses io.clientcore.core.util.LoggerProvider;
}
