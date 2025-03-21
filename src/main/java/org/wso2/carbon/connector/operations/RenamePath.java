/*
 *  Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.connector.operations;

import com.azure.storage.file.datalake.DataLakeFileClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.models.DataLakeRequestConditions;
import com.azure.storage.file.datalake.models.DataLakeStorageException;
import org.apache.synapse.MessageContext;
import org.json.JSONObject;
import org.wso2.carbon.connector.connection.AzureStorageConnectionHandler;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.connection.ConnectionHandler;
import org.wso2.carbon.connector.util.AbstractAzureMediator;
import org.wso2.carbon.connector.util.AzureConstants;
import org.wso2.carbon.connector.util.Error;

import java.time.Duration;

/**
 * Renames a path in the Azure Data Lake Storage.
 */
public class RenamePath extends AbstractAzureMediator {

    @Override
    public void execute(MessageContext messageContext, String responseVariable, Boolean overwriteBody) {

        String connectionName = getProperty(messageContext, AzureConstants.CONNECTION_NAME, String.class, false);
        String fileSystemName =
                getMediatorParameter(messageContext, AzureConstants.FILE_SYSTEM_NAME, String.class, false);
        String sourceIfModifiedSince =
                getMediatorParameter(messageContext, AzureConstants.SOURCE_IF_MODIFIED_SINCE, String.class, true);
        String sourceIfUnmodifiedSince =
                getMediatorParameter(messageContext, AzureConstants.SOURCE_IF_UNMODIFIED_SINCE, String.class, true);
        String sourceLeaseId = getMediatorParameter(messageContext, AzureConstants.SOURCE_LEASE_ID, String.class, true);
        String destinationLeaseId =
                getMediatorParameter(messageContext, AzureConstants.DESTINATION_LEASE_ID, String.class, true);
        String destinationIfMatch =
                getMediatorParameter(messageContext, AzureConstants.DESTINATION_IF_MATCH, String.class, true);
        String destinationIfNoneMatch =
                getMediatorParameter(messageContext, AzureConstants.DESTINATION_IF_NONE_MATCH, String.class, true);
        String sourceIfMatch = getMediatorParameter(messageContext, AzureConstants.SOURCE_IF_MATCH, String.class, true);
        String sourceIfNoneMatch =
                getMediatorParameter(messageContext, AzureConstants.SOURCE_IF_NONE_MATCH, String.class, true);
        String directoryName = getMediatorParameter(messageContext, AzureConstants.DIRECTORY_NAME, String.class, false);
        String newDirectoryName =
                getMediatorParameter(messageContext, AzureConstants.NEW_DIRECTORY_NAME, String.class, false);
        String newFileSystemName =
                getMediatorParameter(messageContext, AzureConstants.NEW_FILE_SYSTEM_NAME, String.class, false);
        Integer timeout = getMediatorParameter(messageContext, AzureConstants.TIMEOUT, Integer.class, true);
        String destinationIfModifiedSince =
                getMediatorParameter(messageContext, AzureConstants.DESTINATION_IF_MODIFIED_SINCE, String.class, true);
        String destinationIfUnmodifiedSince =
                getMediatorParameter(messageContext, AzureConstants.DESTINATION_IF_UNMODIFIED_SINCE, String.class,
                        true);

        ConnectionHandler handler = ConnectionHandler.getConnectionHandler();

        try {
            AzureStorageConnectionHandler azureStorageConnectionHandler =
                    (AzureStorageConnectionHandler) handler.getConnection(AzureConstants.CONNECTOR_NAME,
                            connectionName);
            DataLakeRequestConditions destinationRequestConditions = getRequestConditions(
                    destinationLeaseId, destinationIfMatch, destinationIfNoneMatch, destinationIfModifiedSince,
                    destinationIfUnmodifiedSince);
            DataLakeServiceClient dataLakeServiceClient = azureStorageConnectionHandler.getDataLakeServiceClient();

            DataLakeFileSystemClient dataLakeFileSystemClient =
                    dataLakeServiceClient.getFileSystemClient(fileSystemName);

            DataLakeRequestConditions sourceRequestConditions =
                    getRequestConditions(sourceLeaseId, sourceIfMatch, sourceIfNoneMatch, sourceIfModifiedSince,
                            sourceIfUnmodifiedSince);

            DataLakeFileClient dataLakeFileClient =
                    dataLakeFileSystemClient.getFileClient(directoryName);

            dataLakeFileClient
                    .renameWithResponse(newFileSystemName, newDirectoryName, sourceRequestConditions,
                            destinationRequestConditions,
                            timeout != null ? Duration.ofSeconds(timeout.longValue()) : null, null);

            JSONObject responseObject = new JSONObject();
            responseObject.put("success", true);
            responseObject.put("message", "Successfully renamed the path");

            handleConnectorResponse(messageContext, responseVariable, overwriteBody, responseObject, null,
                    null);

        } catch (DataLakeStorageException e) {
            handleConnectorException(Error.DATA_LAKE_STORAGE_GEN2_ERROR, messageContext, e);

        } catch (ConnectException e) {
            handleConnectorException(Error.CONNECTION_ERROR, messageContext, e);
        } catch (Exception e) {
            handleConnectorException(Error.GENERAL_ERROR, messageContext, e);
        }
    }

}
