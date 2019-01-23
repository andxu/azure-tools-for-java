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

import com.google.common.collect.ImmutableSet;
import com.microsoft.azure.datalake.store.ADLStoreClient;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.CreateSparkBatchJobParameters;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SchedulerState;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJobResponsePayload;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import rx.Observable;
import rx.Observer;

import java.io.IOException;
import java.net.URI;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.*;
import static rx.exceptions.Exceptions.propagate;

public class CosmosServerlessSparkBatchJob extends SparkBatchJob {
    @NotNull
    private final AzureSparkServerlessAccount account;
    @NotNull
    private String jobUuid;
    @NotNull
    private final Deployable jobDeploy;

    // Parameters for getting Livy submission log
    private int logStartIndex = 0;

    public CosmosServerlessSparkBatchJob(@NotNull AzureSparkServerlessAccount account,
                                         @NotNull Deployable jobDeploy,
                                         @NotNull CreateSparkBatchJobParameters submissionParameter,
                                         @NotNull SparkBatchSubmission sparkBatchSubmission,
                                         @NotNull Observer<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject) {
        super(account, submissionParameter, sparkBatchSubmission, ctrlSubject, null, null, null);
        this.account = account;
        this.jobUuid = UUID.randomUUID().toString();
        this.jobDeploy = jobDeploy;
    }

    public int getLogStartIndex() {
        return logStartIndex;
    }

    public void setLogStartIndex(int logStartIndex) {
        this.logStartIndex = logStartIndex;
    }

    @NotNull
    public AzureSparkServerlessAccount getAccount() {
        return account;
    }

    @NotNull
    public AzureHttpObservable getHttp() {
        return getAccount().getHttp();
    }

    @NotNull
    public CreateSparkBatchJobParameters getSubmissionParameter() {
        return (CreateSparkBatchJobParameters) super.getSubmissionParameter();
    }

    @NotNull
    public String getJobUuid() {
        return jobUuid;
    }

    /**
     * Prepare spark event log path for job submission
     * @return whether spark events log path preparation succeed or not
     */
    public Observable<Boolean> prepareSparkEventsLogFolder() {
        return Observable.fromCallable(() -> {
            try {
                String path = getSubmissionParameter().sparkEventsDirectoryPath();
                String accessToken = getHttp().getAccessToken();
                ADLStoreClient storeClient = ADLStoreClient.createClient(URI.create(getAccount().getStorageRootPath()).getHost(), accessToken);
                if (storeClient.checkExists(path)) {
                    return true;
                } else {
                    return storeClient.createDirectory(path);
                }
            } catch (Exception ex) {
                throw new IOException("Spark events log path preparation failed", ex);
            }
        });
    }

    @NotNull
    @Override
    public Observable<? extends ISparkBatchJob> submit() {
        return prepareSparkEventsLogFolder()
                .flatMap(isSucceed -> {
                            if (isSucceed) {
                                return getAccount().createSparkBatchJobRequest(getJobUuid(), getSubmissionParameter());
                            } else {
                                String errorMsg = "Spark events log path preparation failed.";
                                log().warn(errorMsg);
                                return Observable.error(new IOException(errorMsg));
                            }
                        })
                .map(sparkBatchJob -> this);
    }

    @NotNull
    @Override
    public Observable<? extends ISparkBatchJob> deploy(@NotNull String artifactPath) {
        return jobDeploy.deploy(artifactPath)
                .map(path -> {
                    ctrlInfo(String.format("Upload to Azure Datalake store %s successfully", path));
                    getSubmissionParameter().setFilePath(path);
                    return this;
                });
    }

    @Override
    public Observable<? extends ISparkBatchJob> killBatchJob() {
        return getAccount().killSparkBatchJobRequest(getJobUuid())
                .map(resp -> this)
                .onErrorReturn(err -> {
                    String errMsg = String.format("Failed to stop spark job. %s", ExceptionUtils.getStackTrace(err));
                    ctrlInfo(errMsg);
                    log().warn(errMsg);
                    return this;
                });
    }

    private Observable<com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob> getSparkBatchJob() {
        return getAccount().getSparkBatchJobRequest(getJobUuid())
                .flatMap(sparkBatchJob -> {
                    if (sparkBatchJob.schedulerState() != null &&
                            (sparkBatchJob.schedulerState().equals(SchedulerState.FINALIZING) ||
                                    sparkBatchJob.schedulerState().equals(SchedulerState.ENDED))) {
                        String errorMsg = "Job is in " + sparkBatchJob.schedulerState().toString() + " state.";
                        log().warn(errorMsg);
                        return Observable.error(new SparkJobFinishedException(errorMsg));
                    } else {
                        return Observable.just(sparkBatchJob);
                    }
                });
    }


    @NotNull
    private Observable<SparkBatchJobResponsePayload> getResponsePayloadWithState() {
        return getSparkBatchJob()
                .filter(sparkBatchJob -> sparkBatchJob.properties() != null
                        && sparkBatchJob.properties().responsePayload() != null
                        && StringUtils.isNotEmpty(sparkBatchJob.properties().responsePayload().getState()))
                .map(sparkBatchJob -> sparkBatchJob.properties().responsePayload());
    }

    @Override
    protected Observable<AbstractMap.SimpleImmutableEntry<String, String>> getJobDoneObservable() {
        // Refer parent class "SparkBatchJob" for delay interval
        final int GET_JOB_DONE_REPEAT_DELAY_SECONDS = 1;
        return getResponsePayloadWithState()
                .repeatWhen(ob -> ob.delay(GET_JOB_DONE_REPEAT_DELAY_SECONDS, TimeUnit.SECONDS))
                .takeUntil(responsePayload -> isDone(responsePayload.getState()))
                .filter(responsePayload -> isDone(responsePayload.getState()))
                .map(responsePayload ->
                        new AbstractMap.SimpleImmutableEntry<>(
                                responsePayload.getState(),
                                String.join("\n", responsePayload.getLog())))
                .onErrorResumeNext(err -> {
                    if (err instanceof SparkJobFinishedException) {
                        return Observable.error(err);
                    } else {
                        String errHint = "Error getting job status.";
                        log().warn(errHint + " " + ExceptionUtils.getStackTrace(err));
                        return Observable.just(new AbstractMap.SimpleImmutableEntry<>("unknown", errHint + err.getMessage()));
                    }
                });
    }

    @Override
    public Observable<String> awaitStarted() {
        return getResponsePayloadWithState()
                .retryWhen(error -> error.flatMap(exception -> {
                    if (exception instanceof SparkJobFinishedException) {
                        throw propagate(exception);
                    } else {
                        // Retry with limited times
                        return Observable.just(1)
                                .zipWith(Observable.range(1, getRetriesMax()), (n, i) -> i)
                                .delay(getDelaySeconds(), TimeUnit.SECONDS);
                    }
                }))
                .repeatWhen(ob ->
                        ob.doOnNext(ignored ->
                                getCtrlSubject().onNext(
                                        new AbstractMap.SimpleImmutableEntry<>(Info, "The Spark job is starting...")))
                                .delay(getDelaySeconds(), TimeUnit.SECONDS))
                .takeUntil(responsePayload -> isDone(responsePayload.getState()) || isRunning(responsePayload.getState()))
                .filter(responsePayload -> isDone(responsePayload.getState()) || isRunning(responsePayload.getState()))
                .flatMap(responsePayload -> {
                    if (isDone(responsePayload.getState()) && !isSuccess(responsePayload.getState())) {
                        String errorMsg = "The Spark job failed to start due to "
                                + String.join("\n", responsePayload.getLog());
                        log().warn(errorMsg);
                        return Observable.error(
                                new SparkJobException(errorMsg));
                    } else {
                        return Observable.just(responsePayload.getState());
                    }
                });
    }

    @NotNull
    @Override
    protected Observable<String> getJobLogAggregationDoneObservable() {
        // TODO: enable yarn log aggregation
        return Observable.just("SUCCEEDED");
    }

    @NotNull
    public Observable<SparkJobLog> getSubmissionLogRequest(@NotNull String livyUrl,
                                                           int batchId,
                                                           int startIndex,
                                                           int maxLinePerGet) {
        String requestUrl = String.format("%s/%d/log?from=%d&size=%d", livyUrl, batchId, startIndex, maxLinePerGet);
        return getHttp()
                .withUuidUserAgent()
                .get(requestUrl, null, null, SparkJobLog.class);
    }

    @NotNull
    @Override
    public Observable<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>> getSubmissionLog() {
        ImmutableSet<String> ignoredEmptyLines = ImmutableSet.of("stdout:", "stderr:", "yarn diagnostics:");
        final int MAX_LOG_LINES_PER_REQUEST = 128;
        final int GET_LOG_REPEAT_DELAY_MILLISECONDS = 200;
        // We need to repeatly call getSparkBatchJob() since "livyServerApi" field does not always exist in response but
        // only appeared for a while and after that we can't get the "livyServerApi" field.
        final int GET_LIVY_URL_REPEAT_DELAY_MILLISECONDS = 500;
        ctrlInfo("Trying to get livy URL...");
        return getSparkBatchJob()
                .repeatWhen(ob -> ob.delay(GET_LIVY_URL_REPEAT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS))
                .takeUntil(sparkBatchJob -> sparkBatchJob.properties() != null
                        && StringUtils.isNotEmpty(sparkBatchJob.properties().livyServerAPI()))
                .filter(sparkBatchJob -> sparkBatchJob.properties() != null
                        && StringUtils.isNotEmpty(sparkBatchJob.properties().livyServerAPI()))
                .map(sparkBatchJob -> sparkBatchJob.properties().livyServerAPI())
                .map(url -> url + "?adlaAccountName=" + getAccount().getName())
                .doOnNext(url -> {
                    ctrlInfo("Successfully get livy URL: " + url);
                    ctrlInfo("Trying to retrieve livy submission logs...");
                })
                // Get submission log
                .flatMap(livyUrl ->
                        getResponsePayloadWithState()
                                // get batch id before get submission log
                                .doOnNext(responsePayload -> setBatchId(responsePayload.getId()))
                                .flatMap(responsePayload ->
                                        // TODO: I wonder whether it's possible to replace logStartIndex with Rxjava scan operator
                                        getSubmissionLogRequest(livyUrl, getBatchId(), getLogStartIndex(), MAX_LOG_LINES_PER_REQUEST))
                                .map(sparkJobLog ->
                                        sparkJobLog.getLog() == null
                                                ? Collections.<String>emptyList()
                                                : sparkJobLog.getLog())
                                .doOnNext(logs -> setLogStartIndex(getLogStartIndex() + logs.size()))
                                .map(logs -> logs.stream()
                                        .filter(logLine -> !ignoredEmptyLines.contains(logLine.trim().toLowerCase()))
                                        .collect(Collectors.toList()))
                                .flatMap(logLines -> {
                                    if (logLines.size() > 0) {
                                        // If logs we get is not empty, we send onNext() message
                                        // so that these logs can be printed on console view later
                                        return Observable.from(logLines)
                                                .map(line -> new AbstractMap.SimpleImmutableEntry<>(Log, line));
                                    } else {
                                        // If logs we get is empty, we send an onError() message,
                                        // which will get into RetryWhen and then retry with limited times
                                        String errorMsg = "Livy submission log is empty.";
                                        log().warn(errorMsg);
                                        ctrlInfo(errorMsg);
                                        return Observable.error(new SparkJobException(errorMsg));
                                    }
                                })
                                // repeat getting submission log until job started
                                .repeatWhen(ob -> ob.delay(GET_LOG_REPEAT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS))
                                .retryWhen(error -> error.flatMap(exception -> {
                                    if (exception instanceof SparkJobFinishedException) {
                                        throw propagate(exception);
                                    } else {
                                        // delay sometime and retry with limited times
                                        return Observable.just(1)
                                                .zipWith(Observable.range(1, getRetriesMax()), (n, i) -> i)
                                                .doOnNext(i -> ctrlInfo("Retry retrieving livy submission log..."))
                                                .delay(getDelaySeconds(), TimeUnit.SECONDS);
                                    }
                                }))
                                .flatMap(logEntry ->
                                        getResponsePayloadWithState()
                                                .map(responsePayload -> Pair.of(logEntry, responsePayload.getState())))
                                // FIXME:
                                // For HDI spark job, the ending condition is `jobState != starting || appIdIsAllocated`
                                // However, currently the response has no appId all the time so we ignored the app id check
                                .takeUntil(logEntryAndStatePair ->
                                        !logEntryAndStatePair.getRight().equalsIgnoreCase(SparkBatchJobState.STARTING.toString()))
                                .doOnNext(ob -> ctrlInfo("Successfully retrieved livy submission log."))
                                .map(logEntryAndStatePair -> logEntryAndStatePair.getLeft()))
                .onErrorResumeNext(err -> {
                    if (err instanceof SparkJobFinishedException || err.getCause() instanceof SparkJobFinishedException) {
                        return Observable.error(err);
                    } else {
                        String errHint = "Error retrieving livy submission log. ";
                        log().warn(errHint + " " + ExceptionUtils.getStackTrace(err));
                        return Observable.just(new AbstractMap.SimpleImmutableEntry<>(Error, errHint + " " + err.getMessage()));
                    }
                });

    }

    private void ctrlInfo(@NotNull String message) {
        getCtrlSubject().onNext(new AbstractMap.SimpleImmutableEntry<>(MessageInfoType.Info, message));
    }
}
