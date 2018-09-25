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

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ProgramRunner
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.Session
import org.jetbrains.plugins.scala.console.ScalaLanguageConsole

class SparkScalaLivyConsoleRunProfileState(private val consoleBuilder: SparkScalaConsoleBuilder, val session: Session): RunProfileState {
    private val postStartCodes = """
        val __welcome = List(
            "Spark context available as 'sc' (master = " + sc.master + ", app id = " + sc.getConf.getAppId + ").",
            "Spark session available as 'spark'.",
            "Spark Version: " + sc.version,
            util.Properties.versionMsg
        ).mkString("\n")

        println(__welcome)
    """.trimIndent()

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult? {
        try {
            session.create()
                    .flatMap { it.awaitReady() }
                    .toBlocking()
                    .single()
        } catch (ex: Exception) {
            throw ExecutionException("Can't start Livy interactive session: ${ex.message}")
        }

        val console = consoleBuilder.console
        (console as? ScalaLanguageConsole)?.apply {
            // Customize the Spark Livy interactive console
            prompt = "\nSpark>"
        }

        val livySessionProcessHandler = SparkLivySessionProcessHandler(SparkLivySessionProcess(session))

        console.attachToProcess(livySessionProcessHandler)
        livySessionProcessHandler.execute(postStartCodes)

        return DefaultExecutionResult(console, livySessionProcessHandler)
    }
}