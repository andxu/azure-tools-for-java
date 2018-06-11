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

package com.microsoft.azure.hdinsight.spark.ui;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJobConfigurableModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Optional;

public class SparkBatchJobConfigurable implements SettableControl<SparkBatchJobConfigurableModel> {
    private JTabbedPane executionTypeTabPane;
    private JPanel myWholePanel;
    private SparkLocalRunConfigurable myLocalRunConfigurable;
    private SparkSubmissionContentPanelConfigurable myClusterSubmissionConfigurable;

    @NotNull
    private final Project myProject;

    public SparkBatchJobConfigurable(@NotNull final Project project) {
        this.myProject = project;
    }

    @NotNull
    public JComponent getComponent() {
        return myWholePanel;
    }

    protected void createUIComponents() {
        myLocalRunConfigurable = new SparkLocalRunConfigurable(myProject);
        myClusterSubmissionConfigurable = new SparkSubmissionDebuggablePanelConfigurable(
                myProject, () -> {}, new SparkSubmissionDebuggableContentPanel(null));
    }

    @Override
    public void setData(@NotNull SparkBatchJobConfigurableModel data) {
        // Data -> Component
        myLocalRunConfigurable.setData(data.getLocalRunConfigurableModel());
        myClusterSubmissionConfigurable.setData(data.getSubmitModel());
    }

    @Override
    public void getData(@NotNull SparkBatchJobConfigurableModel data) {
        // Component -> Data
        myLocalRunConfigurable.getData(data.getLocalRunConfigurableModel());
        myClusterSubmissionConfigurable.getData(data.getSubmitModel());
    }

    public SparkLocalRunConfigurable getMyLocalRunConfigurable() {
        return myLocalRunConfigurable;
    }

    public SparkSubmissionContentPanelConfigurable getMyClusterSubmissionConfigurable() {
        return myClusterSubmissionConfigurable;
    }

    @NotNull
    public Project getProject() {
        return myProject;
    }

    protected void setMyLocalRunConfigurable(SparkLocalRunConfigurable myLocalRunConfigurable) {
        this.myLocalRunConfigurable = myLocalRunConfigurable;
    }

    protected void setMyClusterSubmissionConfigurable(SparkSubmissionContentPanelConfigurable myClusterSubmissionConfigurable) {
        this.myClusterSubmissionConfigurable = myClusterSubmissionConfigurable;
    }
}
