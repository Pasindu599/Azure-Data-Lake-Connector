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

import com.azure.core.http.rest.Response;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.models.DataLakeStorageException;
import com.azure.storage.file.datalake.models.PublicAccessType;
import org.apache.synapse.MessageContext;
import org.apache.synapse.util.InlineExpressionUtil;
import org.json.JSONObject;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.util.AbstractAzureMediator;
import org.wso2.carbon.connector.util.AzureConstants;
import org.wso2.carbon.connector.util.Error;
import org.wso2.carbon.connector.util.Utils;

import java.time.Duration;
import java.util.HashMap;

/**
 * Handles the creation of a filesystem in Azure Data Lake.
 */
public class CreateFileSystem extends AbstractAzureMediator {

    @Override
    public void execute(MessageContext messageContext, String responseVariable, Boolean overwriteBody) {

        String connectionName =
                getProperty(messageContext, AzureConstants.CONNECTION_NAME, String.class, false);
        String fileSystemName =
                getMediatorParameter(messageContext, AzureConstants.FILE_SYSTEM_NAME, String.class, false);
        Integer timeout = getMediatorParameter(messageContext, AzureConstants.TIMEOUT, Integer.class, true);
        String metadata = getMediatorParameter(messageContext, AzureConstants.METADATA, String.class, true);
        String accessType = getMediatorParameter(messageContext, AzureConstants.ACCESS_TYPE, String.class, true);

        PublicAccessType publicAccessType = null;

        switch (accessType) {
            case "BLOB":
                publicAccessType = PublicAccessType.BLOB;
                break;
            case "CONTAINER":
                publicAccessType = PublicAccessType.CONTAINER;
                break;
        }

        try {

            DataLakeFileSystemClient dataLakeFileSystemClient =
                    getDataLakeFileSystemClient(connectionName, fileSystemName);
            metadata = InlineExpressionUtil.processInLineSynapseExpressionTemplate(messageContext, metadata);
            HashMap<String, String> metadataMap = new HashMap<>();

            if (metadata != null) {
                Utils.addDataToMapFromArrayString(metadata, metadataMap);
            }

            Response<?> response = dataLakeFileSystemClient.createIfNotExistsWithResponse(
                    metadata != null ? metadataMap : null, publicAccessType,
                    timeout != null ? Duration.ofSeconds(timeout.longValue()) : null, null);

            if (response.getStatusCode() == 201) {
                JSONObject responseObject = new JSONObject();
                responseObject.put("success", true);
                responseObject.put("message", "Successfully created the filesystem");
                responseObject.put("fileSystemName", fileSystemName);
                responseObject.put("metadata", metadata);

                handleConnectorResponse(messageContext, responseVariable, overwriteBody, responseObject, null,
                        null);
            }

            // No 'else' block is needed because if the create file system operation fails,
            // the SDK throws an exception. We only handle the success case explicitly
            // (status code 201), and let exceptions propagate for error handling.

        } catch (ConnectException e) {
            handleConnectorException(Error.CONNECTION_ERROR, messageContext, e);
        } catch (DataLakeStorageException e) {
            handleConnectorException(Error.DATA_LAKE_STORAGE_GEN2_ERROR, messageContext, e);
        } catch (Exception e) {
            handleConnectorException(Error.GENERAL_ERROR, messageContext, e);
        }

    }
}
