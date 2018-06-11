/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Lake Analytics Spark Resource Pool URI Collection.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SparkResourcePoolUriItemProperties {
    /**
     * Livy Server Url.
     */
    @JsonProperty(value = "livyAPI", access = JsonProperty.Access.WRITE_ONLY)
    private String livyServerUrl;

    /**
     * Livy Server Url.
     */
    @JsonProperty(value = "livyUI", access = JsonProperty.Access.WRITE_ONLY)
    private String livyUiUrl;

    /**
     * Spark History server Web UI URL.
     */
    @JsonProperty(value = "sparkHistoryUI", access = JsonProperty.Access.WRITE_ONLY)
    private String sparkHistoryWebUiUrl;

    /**
     * Spark Master Web UI URL.
     */
    @JsonProperty(value = "sparkMasterUI", access = JsonProperty.Access.WRITE_ONLY)
    private String sparkMasterWebUiUrl;

    /**
     * Get the livyServerUrl value.
     *
     * @return the livyServerUrl value
     */
    public String livyServerUrl() {
        return this.livyServerUrl;
    }

    /**
     * Get the livyUiUrl value.
     *
     * @return the livyUiUrl value
     */
    public String livyUiUrl() {
        return this.livyUiUrl;
    }
    /**
     * Get the sparkHistoryWebUiUrl value.
     *
     * @return the sparkHistoryWebUiUrl value
     */
    public String sparkHistoryWebUiUrl() {
        return this.sparkHistoryWebUiUrl;
    }

    /**
     * Get the sparkMasterWebUiUrl value.
     *
     * @return the sparkMasterWebUiUrl value
     */
    public String sparkMasterWebUiUrl() {
        return this.sparkMasterWebUiUrl;
    }

}
