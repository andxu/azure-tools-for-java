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

import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext
import com.microsoft.azure.hdinsight.common.logger.ILogger
import java.lang.reflect.Method

// A bridge class to Scala plugin related Action Promoter
open class SparkActionPromoterScalaDelegate(sparkScalaPromoterClassName: String) : ActionPromoter, ILogger {
    private val delegate = SparkScalaPluginDelegate(sparkScalaPromoterClassName)

    private val promoteMethod: Method?
        get() = delegate.getMethod("promote", List::class.java, DataContext::class.java)

    override fun promote(actions: MutableList<AnAction>?, context: DataContext?): MutableList<AnAction>? {
        return (promoteMethod?.invoke(delegate.sparkScalaObj, actions, context) as? MutableList<*>)
                ?.filterIsInstance(AnAction::class.java)?.toMutableList()
    }
}

class SparkExecuteInConsoleActionPromoterDelegate : SparkActionPromoterScalaDelegate("com.microsoft.azure.hdinsight.spark.console.SparkExecuteInConsoleActionPromoter")