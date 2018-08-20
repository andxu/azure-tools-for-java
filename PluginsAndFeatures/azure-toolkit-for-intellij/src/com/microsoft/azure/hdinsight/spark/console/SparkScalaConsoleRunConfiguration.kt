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
import com.intellij.execution.configurations.JavaCommandLineState
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.spark.run.SparkBatchLocalRunState
import com.microsoft.azure.hdinsight.spark.run.configuration.RemoteDebugRunConfiguration
import org.jetbrains.plugins.scala.console.ScalaConsoleRunConfiguration

class SparkScalaConsoleRunConfiguration(
        project: Project,
        configurationFactory: SparkScalaConsoleRunConfigurationFactory,
        name: String)
    : ScalaConsoleRunConfiguration(project, configurationFactory, name) {

    lateinit var batchRunConfiguration: RemoteDebugRunConfiguration

    override fun mainClass(): String = "org.apache.spark.deploy.SparkSubmit"

    override fun createParams(): JavaParameters {
        val localRunParams = SparkBatchLocalRunState(project, batchRunConfiguration.model.localRunConfigurableModel).createParams()
        val params = super.createParams()
        params.classPath.clear()
        params.classPath.addAll(localRunParams.classPath.pathList)
        params.mainClass = mainClass()
//        params.programParametersList.add("org.apache.spark.deploy.SparkSubmit")
        params.programParametersList.add("--class", "org.apache.spark.repl.Main")
        params.programParametersList.add("--name", "Spark shell")
        params.programParametersList.add("spark-shell")
        params.addEnv("SPARK_SUBMIT_OPTS", "-Dscala.usejavacp=true")

        return params
    }

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
        val state = object : JavaCommandLineState(env) {
            override fun createJavaParameters() : JavaParameters {
                val params = createParams()

                params.programParametersList.addParametersString(consoleArgs())
                return params
            }
        }

        state.consoleBuilder = SparkScalaConsoleBuilder(project)

        return state
    }
}
