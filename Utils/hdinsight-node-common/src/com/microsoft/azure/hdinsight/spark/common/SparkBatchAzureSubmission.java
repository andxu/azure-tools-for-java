/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.util.Collections;

public class SparkBatchAzureSubmission extends SparkBatchSubmission {
    @Nullable
    private String tenantId;

    private SparkBatchAzureSubmission() {
        super();
    }

    // Lazy Singleton Instance
    private static SparkBatchAzureSubmission instance = null;

    public static SparkBatchAzureSubmission getInstance() {
        SparkBatchAzureSubmission localRef = instance;
        if(localRef == null){
            synchronized (SparkBatchAzureSubmission.class) {
                localRef = instance;

                if(localRef == null){
                    localRef = new SparkBatchAzureSubmission();
                    instance = localRef;
                }
            }
        }

        return localRef;
    }

    @NotNull
    public SparkBatchAzureSubmission setTenantId(String tid) {
        this.tenantId = tid;

        return this;
    }

    @Override
    public void setCredentialsProvider(String username, String password) {
        getCredentialsProvider().clear();
    }

    @NotNull
    String getAccessToken() throws IOException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) {
            throw new AuthException("Not signed in. Can't send out the request.");
        }

        return azureManager.getAccessToken(tenantId);
    }

    @NotNull
    @Override
    protected CloseableHttpClient getHttpClient() throws IOException {
        return HttpClients.custom()
                .setDefaultHeaders(Collections.singletonList(new BasicHeader("Authorization", "Bearer " + getAccessToken())))
                .build();
    }

    @Nullable
    public String getTenantId() {
        return tenantId;
    }
}
