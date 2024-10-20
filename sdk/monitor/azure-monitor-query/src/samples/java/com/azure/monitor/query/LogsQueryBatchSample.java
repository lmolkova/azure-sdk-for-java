// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.monitor.query;

import com.azure.core.util.Configuration;
import com.azure.core.util.Context;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.monitor.query.models.LogsQueryBatch;
import com.azure.monitor.query.models.LogsQueryBatchResult;
import com.azure.monitor.query.models.LogsQueryBatchResultCollection;
import com.azure.monitor.query.models.LogsQueryResult;
import com.azure.monitor.query.models.LogsTable;
import com.azure.monitor.query.models.LogsTableRow;

import java.util.List;

/**
 * A sample to demonstrate querying for logs from Azure Monitor using a batch of Kusto queries.
 */
public class LogsQueryBatchSample {
    /**
     * The main method to execute the sample.
     * @param args Ignored args.
     */
    public static void main(String[] args) {
        ClientSecretCredential tokenCredential = new ClientSecretCredentialBuilder()
                .clientId(Configuration.getGlobalConfiguration().get("AZURE_MONITOR_CLIENT_ID"))
                .clientSecret(Configuration.getGlobalConfiguration().get("AZURE_MONITOR_CLIENT_SECRET"))
                .tenantId(Configuration.getGlobalConfiguration().get("AZURE_TENANT_ID"))
                .build();

        LogsClient logsClient = new LogsClientBuilder()
                .credential(tokenCredential)
                .buildClient();

        LogsQueryBatch logsQueryBatch = new LogsQueryBatch()
                .addQuery("d2d0e126-fa1e-4b0a-b647-250cdd471e68", "AppRequests | take 2", null)
                .addQuery("d2d0e126-fa1e-4b0a-b647-250cdd471e68", "AppRequests | take 3", null);

        LogsQueryBatchResultCollection batchResultCollection = logsClient
                .queryLogsBatchWithResponse(logsQueryBatch, Context.NONE).getValue();

        List<LogsQueryBatchResult> responses = batchResultCollection.getBatchResults();

        for (LogsQueryBatchResult response : responses) {
            LogsQueryResult queryResult = response.getQueryResult();

            // Sample to iterate by row
            for (LogsTable table : queryResult.getLogsTables()) {
                for (LogsTableRow row : table.getTableRows()) {
                    System.out.println("Row index " + row.getRowIndex());
                    row.getTableRow()
                            .forEach(cell -> System.out.println("Column = " + cell.getColumnName() + "; value = " + cell.getValueAsString()));
                }
            }
        }
    }
}
