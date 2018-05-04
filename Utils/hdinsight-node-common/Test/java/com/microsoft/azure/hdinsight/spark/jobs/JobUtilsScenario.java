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
 */

package com.microsoft.azure.hdinsight.spark.jobs;

import com.microsoft.azure.hdinsight.spark.common.MockHttpService;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import rx.Observable;
import rx.Subscription;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JobUtilsScenario {
    private MockHttpService httpServerMock;

    @Before
    public void setUp() {
        httpServerMock = new MockHttpService();
    }

    @Given("^mock a http service in JobUtilsScenario for (.+) request '(.+)' to return '(.+)' with status code (\\d+)$")
    public void mockHttpService(String action, String serviceUrl, String response, int statusCode) throws Throwable {
        httpServerMock.stub(action, serviceUrl, statusCode, response);
    }

    @Then("^Yarn log observable from '(.*)' should produce events:$")
    public void checkYarnLogObservable(String logUrl, List<String> logs) throws Throwable {
        List<String> logsGot = JobUtils.createYarnLogObservable(null, null, httpServerMock.completeUrl(logUrl), "stderr", 10)
                .takeUntil(line -> line.isEmpty())
                .filter(line -> !line.isEmpty())
                .toList()
                .toBlocking()
                .singleOrDefault(null);

        assertTrue("There are unmatched requests. All requests (reversed) are: \n" +
                        httpServerMock.getLivyServerMock().getAllServeEvents().stream()
                            .map(event -> event.getRequest().getUrl())
                            .reduce("", (a, b) -> a + "\n" + b),
                httpServerMock.getLivyServerMock().findAllUnmatchedRequests().isEmpty());

        assertThat(logsGot).containsExactlyElementsOf(logs);

    }
}
