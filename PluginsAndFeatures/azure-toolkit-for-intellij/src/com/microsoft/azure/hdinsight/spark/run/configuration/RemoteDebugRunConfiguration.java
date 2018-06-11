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
package com.microsoft.azure.hdinsight.spark.run.configuration;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTask;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJobConfigurableModel;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobDebugExecutor;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobDebuggerRunner;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobRunExecutor;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobSubmissionState;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RemoteDebugRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule>
{
    enum RunMode {
        LOCAL,
        REMOTE,
        REMOTE_DEBUG_EXECUTOR
    }

    @NotNull
    private RunMode mode = RunMode.LOCAL;

    // The prop to store the action trigger source if it can be got, such as Run Context
    final public static String ACTION_TRIGGER_PROP = "ActionTrigger";

    @NotNull
    private SparkBatchJobConfigurableModel jobModel;
    @NotNull
    final private Properties actionProperties = new Properties();

    public RemoteDebugRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, @NotNull RunConfigurationModule configurationModule, @NotNull String name) {
        this(new SparkBatchJobConfigurableModel(project), factory, configurationModule, name);
    }

    public RemoteDebugRunConfiguration(@NotNull SparkBatchJobConfigurableModel jobModel,
                                       @NotNull ConfigurationFactory factory,
                                       @NotNull RunConfigurationModule configurationModule,
                                       @NotNull String name) {
        super(name, configurationModule, factory);

        this.jobModel = jobModel;
    }

    @Override
    public void readExternal(@NotNull Element rootElement) throws InvalidDataException {
        super.readExternal(rootElement);

        jobModel.applyFromElement(rootElement);
    }

    @Override
    public void writeExternal(@NotNull Element rootElement) throws WriteExternalException {
        super.writeExternal(rootElement);

        Element jobConfigElement = jobModel.exportToElement();
        rootElement.addContent(jobConfigElement);
    }

    @NotNull
    public SparkBatchJobConfigurableModel getModel() {
        return jobModel;
    }

    @NotNull
    public SparkSubmitModel getSubmitModel() {
        return getModel().getSubmitModel();
    }

    @NotNull
    public Properties getActionProperties() {
        return actionProperties;
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new RemoteDebugSettingsEditor(this);
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {

    }

    public void setRunMode(@NotNull RunMode mode) {
        this.mode = mode;
    }

    @NotNull
    @Override
    public List<BeforeRunTask> getBeforeRunTasks() {
        Stream<BeforeRunTask> compileTask = super.getBeforeRunTasks().stream()
                .filter(task -> task instanceof CompileStepBeforeRun.MakeBeforeRunTask);
        Stream<BeforeRunTask> buildArtifactTask = super.getBeforeRunTasks().stream()
                .filter(task -> task instanceof BuildArtifactsBeforeRunTask);

        switch (mode) {
        case LOCAL:
            compileTask.forEach(task -> task.setEnabled(true));
            buildArtifactTask.forEach(task -> task.setEnabled(false));
            break;
        case REMOTE:
            compileTask.forEach(task -> task.setEnabled(false));
            buildArtifactTask.forEach(task -> task.setEnabled(true));
            break;
        case REMOTE_DEBUG_EXECUTOR:
            compileTask.forEach(task -> task.setEnabled(false));
            buildArtifactTask.forEach(task -> task.setEnabled(false));
            break;
        }

        return super.getBeforeRunTasks();
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) throws ExecutionException {
        String debugTarget = executionEnvironment.getUserData(SparkBatchJobDebuggerRunner.DebugTargetKey);
        Boolean isExecutor = StringUtils.equals(debugTarget, SparkBatchJobDebuggerRunner.DebugExecutor);

        SparkBatchJobSubmissionState state = new SparkBatchJobSubmissionState(getProject(), jobModel, isExecutor);

        if (!isExecutor) {
            if (executor instanceof SparkBatchJobDebugExecutor ||
                    executor instanceof SparkBatchJobRunExecutor) {
                if (getSubmitModel().getArtifact() != null) {
                    BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun(getProject(), this, getSubmitModel().getArtifact());
                }

                setRunMode(RunMode.REMOTE);
            } else {
                setRunMode(RunMode.LOCAL);
            }

            state.createAppInsightEvent(executor, actionProperties.entrySet().stream().collect(Collectors.toMap(
                    (Map.Entry<Object, Object> entry) -> (String) entry.getKey(),
                    (Map.Entry<Object, Object> entry) -> (String) entry.getValue()
            )));
        } else {
            setRunMode(RunMode.REMOTE_DEBUG_EXECUTOR);
        }

        // Clear the action properties
        actionProperties.clear();

        return state;
    }

    public void setActionProperty(@NotNull final String key, @NotNull final String value) {
        this.actionProperties.put(key, value);
    }

    @Override
    public Collection<Module> getValidModules() {
        return new ArrayList<>();
    }

    @Nullable
    @Override
    public String suggestedName() {
        return Optional.ofNullable(getModel().getLocalRunConfigurableModel().getRunClass())
                .map(JavaExecutionUtil::getPresentableClassName)
                .map(className -> "[Spark Job] " + className)
                .orElse(null);
    }

    public void setAsNew() {
    }
}

