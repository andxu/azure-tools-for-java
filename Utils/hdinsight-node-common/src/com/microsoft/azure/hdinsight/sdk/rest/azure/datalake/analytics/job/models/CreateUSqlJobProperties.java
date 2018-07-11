/**
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
 *
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * U-SQL job properties used when submitting U-SQL jobs.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("USql")
public class CreateUSqlJobProperties extends CreateJobProperties {
    /**
     * The specific compilation mode for the job used during execution. If this is not specified during submission, the
     * server will determine the optimal compilation mode. Possible values include: 'Semantic', 'Full', 'SingleBox'.
     */
    @JsonProperty(value = "compileMode")
    private CompileMode compileMode;

    /**
     * Get the specific compilation mode for the job used during execution. If this is not specified during submission, the server will determine the optimal compilation mode. Possible values include: 'Semantic', 'Full', 'SingleBox'.
     *
     * @return the compileMode value
     */
    public CompileMode compileMode() {
        return this.compileMode;
    }

    /**
     * Set the specific compilation mode for the job used during execution. If this is not specified during submission, the server will determine the optimal compilation mode. Possible values include: 'Semantic', 'Full', 'SingleBox'.
     *
     * @param compileMode the compileMode value to set
     * @return the CreateUSqlJobProperties object itself.
     */
    public CreateUSqlJobProperties withCompileMode(CompileMode compileMode) {
        this.compileMode = compileMode;
        return this;
    }

}
