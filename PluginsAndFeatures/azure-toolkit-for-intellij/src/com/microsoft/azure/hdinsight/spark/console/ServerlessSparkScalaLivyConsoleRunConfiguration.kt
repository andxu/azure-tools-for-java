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

package com.microsoft.azure.hdinsight.spark.console

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessClusterManager
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.ServerlessSparkSession
import com.microsoft.azure.hdinsight.spark.run.configuration.RemoteDebugRunConfiguration
import java.net.URI

class ServerlessSparkScalaLivyConsoleRunConfiguration(project: Project,
                                            configurationFactory: SparkScalaLivyConsoleRunConfigurationFactory,
                                            batchRunConfiguration: RemoteDebugRunConfiguration?,
                                            name: String)
    : SparkScalaLivyConsoleRunConfiguration(
        project, configurationFactory, batchRunConfiguration, name)
{
    override val runConfigurationTypeName = "Azure Data Lake Spark Run Configuration"

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
        val sparkCluster = cluster as? AzureSparkServerlessCluster ?: return null

        val livyUrl = (sparkCluster.livyUri?.toString() ?: return null).trimEnd('/') + "/"

        val session = ServerlessSparkSession(
                name,
                URI.create(livyUrl),
                sparkCluster.tenantId,
                sparkCluster.account)

        return SparkScalaLivyConsoleRunProfileState(SparkScalaConsoleBuilder(project), session)
    }

    override fun checkSettingsBeforeRun() {
        cluster = AzureSparkServerlessClusterManager
                .getInstance()
                .clusters
                .find { it.name == this.clusterName }
                ?:throw RuntimeConfigurationError("Can't find the target cluster $clusterName")
    }
}
