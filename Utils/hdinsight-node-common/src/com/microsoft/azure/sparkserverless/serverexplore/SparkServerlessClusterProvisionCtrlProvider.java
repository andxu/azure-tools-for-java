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
package com.microsoft.azure.sparkserverless.serverexplore;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.common.SparkAzureDataLakePoolServiceException;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.List;

public class SparkServerlessClusterProvisionCtrlProvider implements ILogger {

    @NotNull
    private SettableControl<SparkServerlessClusterProvisionSettingsModel> controllableView;

    @NotNull
    private IdeSchedulers ideSchedulers;

    @NotNull
    private AzureSparkServerlessAccount account;
    public SparkServerlessClusterProvisionCtrlProvider(
            @NotNull SettableControl<SparkServerlessClusterProvisionSettingsModel> controllableView,
            @NotNull IdeSchedulers ideSchedulers,
            @NotNull AzureSparkServerlessAccount account) {
        this.controllableView = controllableView;
        this.ideSchedulers = ideSchedulers;
        this.account = account;
    }

    public Observable<List<String>> getClusterNames() {
        return account.get()
                .onErrorReturn(err -> {
                    log().warn(String.format("Can't get the account %s details: %s", account.getName(), err));
                    return account;
                })
                .flatMap(acc -> Observable.from(acc.getClusters()))
                .map(cluster -> cluster.getName())
                .toList();
    }

    public static int getCalculatedAU(int masterCores,
                               int workerCores,
                               int masterMemory,
                               int workerMemory,
                               int workerContainer) {
        return (int) Math.max(
                Math.ceil((masterCores + workerCores * workerContainer) / 2.0),
                Math.ceil((masterMemory + workerMemory * workerContainer) / 6.0));
    }

    private Observable<Integer> getTotalAU() {
        return account.get()
                .onErrorReturn(err -> {
                    log().warn(String.format("Can't get the account %s details: %s", account.getName(), err));
                    return account;
                })
                .subscribeOn(Schedulers.io())
                .map(account -> account.getSystemMaxDegreeOfParallelism());
    }

    private Observable<Integer> getUsedAU() {
        return account.getJobDegreeOfParallelism()
                .subscribeOn(Schedulers.io());
    }

    private int getAvailableAU(int totalAU, int usedAU) {
        return totalAU < usedAU ? 0 : totalAU - usedAU;
    }

    public Observable<Integer> getAvailableAU() {
        return getUsedAU()
                .map(usedAU -> getAvailableAU(account.getSystemMaxDegreeOfParallelism(), usedAU));
    }

    /**
     * The result of availableAU replies on totalAU, here getAvailableAUAndTotalAU() is defined to make sure that
     * availableAU is calculated after totalAU is ready. Notice that totalAU is calculated once for all,
     * getAvailableAUAndTotalAU() should only be called once, next time getAvailableAU() is enough to calculate availableAU
     */
    public Observable<Pair<Integer, Integer>> getAvailableAUAndTotalAU() {
        return Observable.zip(getTotalAU(), getUsedAU(),
                (totalAU, usedAU) -> Pair.of(getAvailableAU(totalAU, usedAU), totalAU));
    }

    @NotNull
    private Observable<AzureSparkServerlessCluster> buildCluster(@NotNull SparkServerlessClusterProvisionSettingsModel toUpdate) {
        return Observable.just(new AzureSparkServerlessCluster.Builder(account)
                .name(toUpdate.getClusterName())
                .masterPerInstanceCores(toUpdate.getMasterCores())
                .masterPerInstanceMemory(toUpdate.getMasterMemory())
                .workerPerInstanceCores(toUpdate.getWorkerCores())
                .workerPerInstanceMemory(toUpdate.getWorkerMemory())
                .workerInstances(toUpdate.getWorkerNumberOfContainers())
                .sparkEventsPath(toUpdate.getSparkEvents())
                .userStorageAccount(account.getDetailResponse().defaultDataLakeStoreAccount())
                .build());
    }

        public Observable<SparkServerlessClusterProvisionSettingsModel> validateAndProvision() {
        // TODO: AU adequation check
        return Observable.just(new SparkServerlessClusterProvisionSettingsModel())
                .doOnNext(controllableView::getData)
                .observeOn(ideSchedulers.processBarVisibleAsync("Provisioning cluster..."))
                .map(toUpdate -> toUpdate.setErrorMessage(null))
                .flatMap(toUpdate ->
                        buildCluster(toUpdate)
                                .doOnNext(cluster -> toUpdate.setClusterGuid(cluster.getGuid()))
                                .flatMap(cluster -> cluster.provision())
                                .map(cluster -> toUpdate)
                                .onErrorReturn(err -> {
                                    log().warn("Error provision a cluster. " + ExceptionUtils.getStackTrace(err));
                                    if (err instanceof SparkAzureDataLakePoolServiceException) {
                                        String requestId = ((SparkAzureDataLakePoolServiceException) err).getRequestId();
                                        toUpdate.setRequestId(requestId);
                                        log().info("x-ms-request-id: " + requestId);
                                    }
                                    log().info("Cluster guid: " + toUpdate.getClusterGuid());
                                    return toUpdate.setErrorMessage(err.getMessage());
                                })
                )
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData)
                .filter(data -> StringUtils.isEmpty(data.getErrorMessage()));
    }
}
