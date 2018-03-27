/*
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

package com.microsoft.azure.hdinsight.spark.run;

import com.google.common.net.HostAndPort;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.project.Project;
import com.intellij.remote.RemoteProcess;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.*;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import org.apache.commons.io.output.NullOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.Info;
import static rx.exceptions.Exceptions.propagate;

public class SparkBatchJobRemoteProcess extends RemoteProcess {
    @NotNull
    private Project project;
    @NotNull
    private IdeaSchedulers schedulers;
    @NotNull
    private SparkSubmitModel submitModel;
    @NotNull
    private final PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject;
    @NotNull
    private SparkJobLogInputStream jobStdoutLogInputSteam;
    @NotNull
    private SparkJobLogInputStream jobStderrLogInputSteam;
    @Nullable
    private Subscription jobSubscription;
    @Nullable
    private SparkBatchJob sparkJob;
    @NotNull
    private final PublishSubject<SparkBatchJobSubmissionEvent> eventSubject = PublishSubject.create();

    private boolean isDisconnected;

    public SparkBatchJobRemoteProcess(@NotNull Project project, @NotNull SparkSubmitModel sparkSubmitModel,
                                      @NotNull PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject)
            throws ExecutionException {
        this.project = project;
        this.schedulers = new IdeaSchedulers(project);
        this.submitModel = sparkSubmitModel;
        this.ctrlSubject = ctrlSubject;

        this.jobStdoutLogInputSteam = new SparkJobLogInputStream("stdout");
        this.jobStderrLogInputSteam = new SparkJobLogInputStream("stderr");
    }

    /**
     * To Kill the remote job.
     *
     * @return is the remote Spark Job killed
     */
    @Override
    public boolean killProcessTree() {
        return false;
    }

    /**
     * Is the Spark job session connected
     *
     * @return is the Spark Job log getting session still connected
     */
    @Override
    public boolean isDisconnected() {
        return isDisconnected;
    }

    @Nullable
    @Override
    public HostAndPort getLocalTunnel(int i) {
        return null;
    }

    @Override
    public OutputStream getOutputStream() {
        return new NullOutputStream();
    }

    @Override
    public InputStream getInputStream() {
        return jobStdoutLogInputSteam;
    }

    @Override
    public InputStream getErrorStream() {
        return jobStderrLogInputSteam;
    }

    @Override
    public int waitFor() throws InterruptedException {
        return 0;
    }

    @Override
    public int exitValue() {
        return 0;
    }

    @Override
    public void destroy() {
        getSparkJob().ifPresent(sparkBatchJob -> {
            try {
                sparkBatchJob.killBatchJob();
            } catch (IOException ignored) {
            }
        });

        this.disconnect();
    }

    public Optional<SparkBatchJob> getSparkJob() {
        return Optional.ofNullable(sparkJob);
    }

    public Optional<Subscription> getJobSubscription() {
        return Optional.ofNullable(jobSubscription);
    }

    public SparkBatchJob createJobToSubmit(IClusterDetail cluster) throws SparkJobException {
        return new SparkBatchJob(
                URI.create(JobUtils.getLivyConnectionURL(cluster)),
                submitModel.getSubmissionParameter(),
                SparkBatchSubmission.getInstance());
    }

    public void start() {
        // Build, deploy and wait for the job done.
        jobSubscription = SparkSubmitHelper.getInstance().buildArtifact(project, submitModel.isLocalArtifact(), submitModel.getArtifact())
                .flatMap(artifact -> JobUtils.deployArtifact(
                            submitModel.getArtifactPath(artifact.getName())
                                    .orElseThrow(() -> propagate(new SparkJobException("Can't find jar path to upload"))),
                            submitModel.getSubmissionParameter().getClusterName(),
                            ctrlSubject)
                    .subscribeOn(schedulers.processBarVisibleAsync("Deploy the jar file into cluster")))
                .toObservable()
                .flatMap(this::submitJob)
                .flatMap(job -> Observable.zip(
                        attachJobInputStream(jobStderrLogInputSteam, job),
                        attachJobInputStream(jobStdoutLogInputSteam, job),
                        (job1, job2) -> {
                            sparkJob = job;
                            return job;
                        }))
                .flatMap(runningJob -> runningJob.getJobDoneObservable()
                        .subscribeOn(schedulers.processBarVisibleAsync("Spark batch job is running")))
                .subscribe(sdPair -> {
                    if (sdPair.getKey() == SparkBatchJobState.SUCCESS) {
                        logInfo("Job run successfully.");
                    } else {
                        ctrlSubject.onNext(new SimpleImmutableEntry<>(MessageInfoType.Error, "Job state is " + sdPair.getKey().toString()));
                        ctrlSubject.onNext(new SimpleImmutableEntry<>(MessageInfoType.Error, "Diagnostics: " + sdPair.getValue()));
                    }
                }, err -> {
                    ctrlSubject.onError(err);
                    disconnect();
                }, () -> {
                    disconnect();
                });
    }

    private Observable<SparkBatchJob> attachJobInputStream(SparkJobLogInputStream inputStream, SparkBatchJob job) {
        return Observable.just(inputStream)
                .map(stream -> stream.attachJob(job))
                .subscribeOn(schedulers.processBarVisibleAsync("Attach Spark batch job outputs " + inputStream.getLogType()))
                .retryWhen(attempts -> attempts.flatMap(err -> {
                    try {
                        final String state = job.getState();

                        if (state.equals("starting") || state.equals("not_started") || state.equals("running")) {
                            logInfo("Job is waiting for start due to cluster busy, please wait or disconnect (The job will run when the cluster is free).");

                            return Observable.timer(5, TimeUnit.SECONDS);
                        }
                    } catch (IOException ignored) {
                    }

                    return Observable.error(new SparkJobException("Spark Job Service not available, please check HDInsight cluster status.", err));
                }));
    }

    public void disconnect() {
        this.isDisconnected = true;

        this.ctrlSubject.onCompleted();
        this.eventSubject.onCompleted();

        this.getJobSubscription().ifPresent(Subscription::unsubscribe);
    }

    private void logInfo(String message) {
        ctrlSubject.onNext(new SimpleImmutableEntry<>(Info, message));
    }

    @NotNull
    public PublishSubject<SparkBatchJobSubmissionEvent> getEventSubject() {
        return eventSubject;
    }

    private Observable<SparkBatchJob> startJobSubmissionLogReceiver(SparkBatchJob job) {
        getEventSubject().onNext(new SparkBatchJobSubmissionEvent(SparkBatchJobSubmissionEvent.Type.SUBMITTED, job));

        return job.getSubmissionLog()
                .doOnNext(ctrlSubject::onNext)
                .doOnError(ctrlSubject::onError)
                .last()
                .map(messageTypeText -> job);

    }

    private Observable<SparkBatchJob> submitJob(SimpleImmutableEntry<IClusterDetail, String> clusterArtifactUriPair) {
        IClusterDetail cluster = clusterArtifactUriPair.getKey();
        submitModel.getSubmissionParameter().setFilePath(clusterArtifactUriPair.getValue());
        return JobUtils.submit(cluster, submitModel.getSubmissionParameter())
                .subscribeOn(schedulers.processBarVisibleAsync("Submit the Spark batch job"))
                .toObservable()
                .flatMap(this::startJobSubmissionLogReceiver);   // To receive the Livy submission log
    }
    @NotNull
    public SparkSubmitModel getSubmitModel() {
        return submitModel;
    }

}
