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

package com.microsoft.azure.sparkserverless.serverexplore.ui;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HideableDecorator;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.sparkserverless.common.IntegerWithErrorHintedField;
import com.microsoft.azure.sparkserverless.common.JXHyperLinkWithUri;
import com.microsoft.azure.sparkserverless.common.TextWithErrorHintedField;
import com.microsoft.azure.sparkserverless.serverexplore.SparkServerlessClusterProvisionCtrlProvider;
import com.microsoft.azure.sparkserverless.serverexplore.SparkServerlessClusterProvisionSettingsModel;
import com.microsoft.azure.sparkserverless.serverexplore.sparkserverlessnode.SparkServerlessADLAccountNode;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public class SparkServerlessProvisionDialog extends DialogWrapper
        implements SettableControl<SparkServerlessClusterProvisionSettingsModel>, ILogger {
    @NotNull
    private SparkServerlessClusterProvisionCtrlProvider ctrlProvider;
    @NotNull
    private SparkServerlessADLAccountNode adlAccountNode;

    protected TextWithErrorHintedField clusterNameField;
    protected JTextField adlAccountField;
    protected TextWithErrorHintedField sparkEventsField;
    protected IntegerWithErrorHintedField masterCoresField;
    protected IntegerWithErrorHintedField masterMemoryField;
    protected IntegerWithErrorHintedField workerCoresField;
    protected IntegerWithErrorHintedField workerMemoryField;
    protected IntegerWithErrorHintedField workerNumberOfContainersField;
    protected JTextField availableAUField;
    protected JTextField totalAUField;
    protected JTextField calculatedAUField;
    protected JLabel masterMemoryLabel;
    protected JLabel masterCoresLabel;
    protected JLabel clusterNameLabel;
    protected JLabel adlAccountLabel;
    protected JLabel SparkEventsLabel;
    protected JLabel availableAULabel;
    protected JLabel calculatedAULabel;
    protected JLabel workerCoresLabel;
    protected JLabel workerMemoryLabel;
    protected JLabel workerNumberOfContainersLabel;
    protected JPanel provisionDialogPanel;
    protected JButton refreshButton;
    protected JLabel storageRootPathLabel;
    protected JComboBox sparkVersionComboBox;
    protected JLabel sparkVersionLabel;
    private JXHyperLinkWithUri jobQueueHyperLink;
    protected JPanel errorMessagePanel;
    protected JPanel errorMessagePanelHolder;
    protected JPanel configPanel;
    protected JPanel auPanel;
    protected ConsoleViewImpl consoleViewPanel;
    protected HideableDecorator errorMessageDecorator;
    @NotNull
    private final List<TextWithErrorHintedField> allTextFields = Arrays.asList(clusterNameField, sparkEventsField);
    @NotNull
    private final List<IntegerWithErrorHintedField> allAURelatedFields = Arrays.asList(masterCoresField, workerCoresField,
            masterMemoryField, workerMemoryField, workerNumberOfContainersField);

    public SparkServerlessProvisionDialog(@NotNull SparkServerlessADLAccountNode adlAccountNode,
                                          @NotNull AzureSparkServerlessAccount account) {
        // TODO: refactor the design of getProject Method for Node Class
        // TODO: get project through ProjectUtils.theProject()
        super((Project) adlAccountNode.getProject(), true);
        this.ctrlProvider = new SparkServerlessClusterProvisionCtrlProvider(
                this, new IdeaSchedulers((Project) adlAccountNode.getProject()), account);
        this.adlAccountNode = adlAccountNode;

        init();
        this.setTitle("Provision Spark Cluster");
        availableAUField.setBorder(BorderFactory.createEmptyBorder());
        totalAUField.setBorder(BorderFactory.createEmptyBorder());
        calculatedAUField.setBorder(BorderFactory.createEmptyBorder());
        this.setModal(true);

        // make error message widget hideable
        errorMessagePanel.setBorder(BorderFactory.createEmptyBorder());
        errorMessageDecorator = new HideableDecorator(errorMessagePanelHolder, "Log", true);
        errorMessageDecorator.setContentComponent(errorMessagePanel);
        errorMessageDecorator.setOn(false);

        // add console view panel to error message panel
        consoleViewPanel = new ConsoleViewImpl((Project) adlAccountNode.getProject(), false);
        errorMessagePanel.add(consoleViewPanel.getComponent(), BorderLayout.CENTER);
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("provisionLog",
                new DefaultActionGroup(consoleViewPanel.createConsoleActions()), false);
        errorMessagePanel.add(toolbar.getComponent(), BorderLayout.WEST);

        this.jobQueueHyperLink.setURI(account.getJobManagementURI());

        this.enableClusterNameUniquenessCheck();
        // We can determine the ADL account since we provision on a specific ADL account Node
        this.adlAccountField.setText(adlAccountNode.getAdlAccount().getName());
        this.storageRootPathLabel.setText(Optional.ofNullable(account.getStorageRootPath()).orElse(""));

        refreshButton.addActionListener(e -> updateAvailableAU());
        allAURelatedFields.forEach(comp ->
                comp.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        if (isAllAURelatedFieldsLegal()) {
                            updateCalculatedAU();
                        }
                        super.focusLost(e);
                    }
                }));
        // These action listeners promise that Ok button can only be clicked until all the fields are legal
        Stream.concat(allTextFields.stream(), allAURelatedFields.stream()).forEach(comp ->
                comp.getDocument().addDocumentListener(new DocumentAdapter() {
                    @Override
                    protected void textChanged(DocumentEvent e) {
                        getOKAction().setEnabled(isAllFieldsLegal());
                    }
                }));
        getOKAction().setEnabled(false);
        this.getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                updateAvailableAUAndTotalAU();
                updateCalculatedAU();
                super.windowOpened(e);
            }
        });
    }

    @Override
    protected void dispose() {
        super.dispose();

        consoleViewPanel.dispose();
    }

    protected void enableClusterNameUniquenessCheck() {
        try {
            clusterNameField.setNotAllowedValues(
                    new HashSet<>(ctrlProvider.getClusterNames().toBlocking().singleOrDefault(new ArrayList<>())));

            sparkEventsField.setPatternAndErrorMessage(null);
            // The text setting is necessary. By default, '/' is not allowed for TextWithErrorHintedField, leading to
            // error tooltip. We have to set the text to trigger the validator of the new pattern.
            sparkEventsField.setText("spark-events/");
        } catch (Exception ex) {
            log().warn("Got exceptions when getting cluster names: " + ex);
        }
    }

    private void updateAvailableAUAndTotalAU() {
        if (!refreshButton.isEnabled()) {
            return;
        }
        refreshButton.setEnabled(false);
        ctrlProvider.getAvailableAUAndTotalAU()
                .doOnEach(pair -> refreshButton.setEnabled(true))
                .subscribe(pair -> {
                    availableAUField.setText(String.valueOf(pair.getLeft()));
                    totalAUField.setText(String.valueOf(pair.getRight()));
                });
    }

    private void updateAvailableAU() {
        if (!refreshButton.isEnabled()) {
            return;
        }
        refreshButton.setEnabled(false);
        ctrlProvider.getAvailableAU()
                .doOnEach(au -> refreshButton.setEnabled(true))
                .subscribe(au -> availableAUField.setText(String.valueOf(au)));
    }

    private void updateCalculatedAU() {
        calculatedAUField.setText(String.valueOf(ctrlProvider.getCalculatedAU(
                Integer.valueOf(masterCoresField.getText()),
                Integer.valueOf(workerCoresField.getText()),
                Integer.valueOf(masterMemoryField.getText()),
                Integer.valueOf(workerMemoryField.getText()),
                Integer.valueOf(workerNumberOfContainersField.getText()))));
    }

    protected void printLogLine(@NotNull ConsoleViewContentType logLevel, @NotNull String log) {
        consoleViewPanel.print(LocalDateTime.now().toString() + " " + logLevel.toString().toUpperCase() + " " + log + "\n", logLevel);
    }

    // Data -> Components
    @Override
    public void setData(@NotNull SparkServerlessClusterProvisionSettingsModel data) {
        if (!StringUtils.isEmpty(data.getErrorMessage())) {
            if (!errorMessageDecorator.isExpanded()) {
                errorMessageDecorator.setOn(true);
            }
            printLogLine(ConsoleViewContentType.ERROR_OUTPUT, data.getErrorMessage());
        }
        printLogLine(ConsoleViewContentType.NORMAL_OUTPUT, "x-ms-request-id: " + data.getRequestId());
        printLogLine(ConsoleViewContentType.NORMAL_OUTPUT, "cluster guid: " + data.getClusterGuid());
    }

    // Components -> Data
    @Override
    public void getData(@NotNull SparkServerlessClusterProvisionSettingsModel data) {
        data.setClusterName(clusterNameField.getText())
                .setAdlAccount(adlAccountField.getText())
                .setSparkEvents(sparkEventsField.getText())
                .setAvailableAU(NumberUtils.toInt(availableAUField.getText()))
                .setTotalAU(NumberUtils.toInt(totalAUField.getText()))
                .setCalculatedAU(NumberUtils.toInt(calculatedAUField.getText()))
                .setMasterCores(masterCoresField.getValue())
                .setMasterMemory(masterMemoryField.getValue())
                .setWorkerCores(workerCoresField.getValue())
                .setWorkerMemory(workerMemoryField.getValue())
                .setWorkerNumberOfContainers(workerNumberOfContainersField.getValue())
                .setStorageRootPathLabelTitle(storageRootPathLabel.getText());
    }

    @Override
    protected void doOKAction() {
        if (!getOKAction().isEnabled()) {
            return;
        }

        getOKAction().setEnabled(false);
        ctrlProvider
                .validateAndProvision()
                .doOnEach(notification -> getOKAction().setEnabled(true))
                .subscribe(toUpdate -> {
                    // TODO: replace load with refreshWithoutAsync
                    adlAccountNode.load(false);
                    super.doOKAction();
                }, err -> log().warn("Error provision a cluster. " + err.toString()));
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[0];
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return provisionDialogPanel;
    }

    private boolean isAllFieldsLegal() {
        return Stream.concat(allTextFields.stream(), allAURelatedFields.stream()).allMatch(comp -> comp.isLegal());
    }

    private boolean isAllAURelatedFieldsLegal() {
        return allAURelatedFields.stream().allMatch(comp -> comp.isLegal());
    }
}
