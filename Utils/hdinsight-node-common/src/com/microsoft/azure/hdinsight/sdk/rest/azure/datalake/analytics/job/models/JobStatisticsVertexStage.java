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

import org.joda.time.Period;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Data Lake Analytics job statistics vertex stage information.
 */
public class JobStatisticsVertexStage {
    /**
     * the amount of data read, in bytes.
     */
    @JsonProperty(value = "dataRead", access = JsonProperty.Access.WRITE_ONLY)
    private Long dataRead;

    /**
     * the amount of data read across multiple pods, in bytes.
     */
    @JsonProperty(value = "dataReadCrossPod", access = JsonProperty.Access.WRITE_ONLY)
    private Long dataReadCrossPod;

    /**
     * the amount of data read in one pod, in bytes.
     */
    @JsonProperty(value = "dataReadIntraPod", access = JsonProperty.Access.WRITE_ONLY)
    private Long dataReadIntraPod;

    /**
     * the amount of data remaining to be read, in bytes.
     */
    @JsonProperty(value = "dataToRead", access = JsonProperty.Access.WRITE_ONLY)
    private Long dataToRead;

    /**
     * the amount of data written, in bytes.
     */
    @JsonProperty(value = "dataWritten", access = JsonProperty.Access.WRITE_ONLY)
    private Long dataWritten;

    /**
     * the number of duplicates that were discarded.
     */
    @JsonProperty(value = "duplicateDiscardCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer duplicateDiscardCount;

    /**
     * the number of failures that occured in this stage.
     */
    @JsonProperty(value = "failedCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer failedCount;

    /**
     * the maximum amount of data read in a single vertex, in bytes.
     */
    @JsonProperty(value = "maxVertexDataRead", access = JsonProperty.Access.WRITE_ONLY)
    private Long maxVertexDataRead;

    /**
     * the minimum amount of data read in a single vertex, in bytes.
     */
    @JsonProperty(value = "minVertexDataRead", access = JsonProperty.Access.WRITE_ONLY)
    private Long minVertexDataRead;

    /**
     * the number of read failures in this stage.
     */
    @JsonProperty(value = "readFailureCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer readFailureCount;

    /**
     * the number of vertices that were revoked during this stage.
     */
    @JsonProperty(value = "revocationCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer revocationCount;

    /**
     * the number of currently running vertices in this stage.
     */
    @JsonProperty(value = "runningCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer runningCount;

    /**
     * the number of currently scheduled vertices in this stage.
     */
    @JsonProperty(value = "scheduledCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer scheduledCount;

    /**
     * the name of this stage in job execution.
     */
    @JsonProperty(value = "stageName", access = JsonProperty.Access.WRITE_ONLY)
    private String stageName;

    /**
     * the number of vertices that succeeded in this stage.
     */
    @JsonProperty(value = "succeededCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer succeededCount;

    /**
     * the amount of temporary data written, in bytes.
     */
    @JsonProperty(value = "tempDataWritten", access = JsonProperty.Access.WRITE_ONLY)
    private Long tempDataWritten;

    /**
     * the total vertex count for this stage.
     */
    @JsonProperty(value = "totalCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer totalCount;

    /**
     * the amount of time that failed vertices took up in this stage.
     */
    @JsonProperty(value = "totalFailedTime", access = JsonProperty.Access.WRITE_ONLY)
    private Period totalFailedTime;

    /**
     * the current progress of this stage, as a percentage.
     */
    @JsonProperty(value = "totalProgress", access = JsonProperty.Access.WRITE_ONLY)
    private Integer totalProgress;

    /**
     * the amount of time all successful vertices took in this stage.
     */
    @JsonProperty(value = "totalSucceededTime", access = JsonProperty.Access.WRITE_ONLY)
    private Period totalSucceededTime;

    /**
     * Get the dataRead value.
     *
     * @return the dataRead value
     */
    public Long dataRead() {
        return this.dataRead;
    }

    /**
     * Get the dataReadCrossPod value.
     *
     * @return the dataReadCrossPod value
     */
    public Long dataReadCrossPod() {
        return this.dataReadCrossPod;
    }

    /**
     * Get the dataReadIntraPod value.
     *
     * @return the dataReadIntraPod value
     */
    public Long dataReadIntraPod() {
        return this.dataReadIntraPod;
    }

    /**
     * Get the dataToRead value.
     *
     * @return the dataToRead value
     */
    public Long dataToRead() {
        return this.dataToRead;
    }

    /**
     * Get the dataWritten value.
     *
     * @return the dataWritten value
     */
    public Long dataWritten() {
        return this.dataWritten;
    }

    /**
     * Get the duplicateDiscardCount value.
     *
     * @return the duplicateDiscardCount value
     */
    public Integer duplicateDiscardCount() {
        return this.duplicateDiscardCount;
    }

    /**
     * Get the failedCount value.
     *
     * @return the failedCount value
     */
    public Integer failedCount() {
        return this.failedCount;
    }

    /**
     * Get the maxVertexDataRead value.
     *
     * @return the maxVertexDataRead value
     */
    public Long maxVertexDataRead() {
        return this.maxVertexDataRead;
    }

    /**
     * Get the minVertexDataRead value.
     *
     * @return the minVertexDataRead value
     */
    public Long minVertexDataRead() {
        return this.minVertexDataRead;
    }

    /**
     * Get the readFailureCount value.
     *
     * @return the readFailureCount value
     */
    public Integer readFailureCount() {
        return this.readFailureCount;
    }

    /**
     * Get the revocationCount value.
     *
     * @return the revocationCount value
     */
    public Integer revocationCount() {
        return this.revocationCount;
    }

    /**
     * Get the runningCount value.
     *
     * @return the runningCount value
     */
    public Integer runningCount() {
        return this.runningCount;
    }

    /**
     * Get the scheduledCount value.
     *
     * @return the scheduledCount value
     */
    public Integer scheduledCount() {
        return this.scheduledCount;
    }

    /**
     * Get the stageName value.
     *
     * @return the stageName value
     */
    public String stageName() {
        return this.stageName;
    }

    /**
     * Get the succeededCount value.
     *
     * @return the succeededCount value
     */
    public Integer succeededCount() {
        return this.succeededCount;
    }

    /**
     * Get the tempDataWritten value.
     *
     * @return the tempDataWritten value
     */
    public Long tempDataWritten() {
        return this.tempDataWritten;
    }

    /**
     * Get the totalCount value.
     *
     * @return the totalCount value
     */
    public Integer totalCount() {
        return this.totalCount;
    }

    /**
     * Get the totalFailedTime value.
     *
     * @return the totalFailedTime value
     */
    public Period totalFailedTime() {
        return this.totalFailedTime;
    }

    /**
     * Get the totalProgress value.
     *
     * @return the totalProgress value
     */
    public Integer totalProgress() {
        return this.totalProgress;
    }

    /**
     * Get the totalSucceededTime value.
     *
     * @return the totalSucceededTime value
     */
    public Period totalSucceededTime() {
        return this.totalSucceededTime;
    }

}
