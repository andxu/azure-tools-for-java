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

package com.microsoft.azure.hdinsight.spark.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.uiDesigner.core.GridConstraints.*
import com.microsoft.azure.hdinsight.common.AbfsUri
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.common.viewmodels.ImmutableComboBoxModelDelegated
import com.microsoft.azure.hdinsight.common.viewmodels.ComboBoxSelectionDelegated
import com.microsoft.azure.hdinsight.sdk.cluster.AzureAdAccountDetail
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.common.ADLSGen2OAuthHttpObservable
import com.microsoft.azure.hdinsight.sdk.common.SharedKeyHttpObservable
import com.microsoft.azure.hdinsight.sdk.storage.ADLSGen2StorageAccount
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.azure.hdinsight.spark.ui.ImmutableComboBoxModel.Companion.empty
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionJobUploadStorageCtrl.*
import com.microsoft.azure.hdinsight.spark.ui.filesystem.ADLSGen2FileSystem
import com.microsoft.azure.hdinsight.spark.ui.filesystem.AdlsGen2VirtualFile
import com.microsoft.azure.hdinsight.spark.ui.filesystem.AzureStorageVirtualFile
import com.microsoft.azure.hdinsight.spark.ui.filesystem.AzureStorageVirtualFileSystem
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.rxjava.DisposableObservers
import org.apache.commons.lang3.StringUtils
import rx.subjects.PublishSubject
import java.awt.CardLayout
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.ItemEvent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel

open class SparkSubmissionJobUploadStoragePanel: JPanel(), Disposable, ILogger {

    private val notFinishCheckMessage = "job upload storage validation check is not finished"
    private val storageTypeLabel = JLabel("Storage Type")
    val azureBlobCard = SparkSubmissionJobUploadStorageAzureBlobCard()
    val sparkInteractiveSessionCard = SparkSubmissionJobUploadStorageSparkInteractiveSessionCard()
    val clusterDefaultStorageCard = SparkSubmissionJobUploadStorageClusterDefaultStorageCard()
    val notSupportStorageCard = SparkSubmissionJobUploadStorageClusterNotSupportStorageCard()
    val accountDefaultStorageCard = SparkSubmissionJobUploadStorageAccountDefaultStorageCard()
    val adlsGen2Card = SparkSubmissionJobUploadStorageGen2Card()
    val adlsGen2OAuthCard = SparkSubmissionJobUploadStorageGen2OAuthCard()

    val adlsCard = SparkSubmissionJobUploadStorageAdlsCard().apply {
        // handle sign in/out action when sign in/out link is clicked
        arrayOf(signInCard.signInLink, signOutCard.signOutLink)
                .forEach {
                    it.addActionListener {
                        AzureSignInAction.onAzureSignIn(null)
                        viewModel.storageCheckSubject.onNext(StorageCheckSignInOutEvent())
                    }
                }

        // validate storage info when ADLS Root Path field focus lost
        adlsRootPathField.addFocusListener( object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                viewModel.storageCheckSubject.onNext(StorageCheckPathFocusLostEvent("ADLS"))
            }
        })
    }

    val webHdfsCard = SparkSubmissionJobUploadStorageWebHdfsCard().apply {
        // validate storage info when webhdfs root path field lost
        webHdfsRootPathField.addFocusListener( object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                viewModel.storageCheckSubject.onNext(StorageCheckPathFocusLostEvent("WEBHDFS"))
            }
        })
    }


    private val storageTypeComboBox = ComboBox<SparkSubmitStorageType>(empty()).apply {
        name = "storageTypeComboBox"
        // validate storage info after storage type is selected
        addItemListener { itemEvent ->
            // change panel
            val curLayout = storageCardsPanel.layout as? CardLayout ?: return@addItemListener

            if (itemEvent?.stateChange == ItemEvent.SELECTED) {
                val selectedType = itemEvent.item as? SparkSubmitStorageType ?: return@addItemListener

                curLayout.show(storageCardsPanel, selectedType.description)
                viewModel.storageCheckSubject.onNext(StorageCheckSelectedStorageTypeEvent(selectedType.description))
            }
        }

        @Suppress("MissingRecentApi") // Supported after 192.3099
        renderer = object: SimpleListCellRenderer<SparkSubmitStorageType>() {
            override fun customize(list: JList<out SparkSubmitStorageType>?, type: SparkSubmitStorageType?, index: Int, selected: Boolean, hasFocus: Boolean) {
                text = type?.description
            }
        }
    }

    private val storageCardsPanel = JPanel(CardLayout()).apply {
        add(azureBlobCard, azureBlobCard.title)
        add(sparkInteractiveSessionCard, sparkInteractiveSessionCard.title)
        add(clusterDefaultStorageCard, clusterDefaultStorageCard.title)
        add(notSupportStorageCard, notSupportStorageCard.title)
        add(adlsCard, adlsCard.title)
        add(adlsGen2Card, adlsGen2Card.title)
        add(adlsGen2OAuthCard, adlsGen2OAuthCard.title)
        add(webHdfsCard, webHdfsCard.title)
        add(accountDefaultStorageCard, accountDefaultStorageCard.title)
    }

    var errorMessage: String? = notFinishCheckMessage
    init {
        val formBuilder = panel {
            columnTemplate {
                col {
                    anchor = ANCHOR_WEST
                }
                col {
                    anchor = ANCHOR_WEST
                    hSizePolicy = SIZEPOLICY_WANT_GROW
                    fill = FILL_HORIZONTAL
                }
            }
            row {
                c(storageTypeLabel.apply { labelFor = storageTypeComboBox }) { indent = 2 }
                        c(storageTypeComboBox) { indent = 3 }
            }
            row {
                c(storageCardsPanel) { indent = 2; colSpan = 2; hSizePolicy = SIZEPOLICY_WANT_GROW; fill = FILL_HORIZONTAL}
            }
        }

        layout = formBuilder.createGridLayoutManager()
        formBuilder.allComponentConstraints.forEach { (component, gridConstrains) -> add(component, gridConstrains) }
    }

    inner class ViewModel : DisposableObservers() {
        var deployStorageTypeSelection: SparkSubmitStorageType? by ComboBoxSelectionDelegated(storageTypeComboBox)
        var deployStorageTypesModel: ImmutableComboBoxModel<SparkSubmitStorageType> by ImmutableComboBoxModelDelegated(storageTypeComboBox)

        val storageCheckSubject: PublishSubject<StorageCheckEvent> = disposableSubjectOf { PublishSubject.create() }

        fun prepareVFSRoot(uploadRootPathUri: AbfsUri, storageAccount: IHDIStorageAccount?, cluster: IClusterDetail?): AzureStorageVirtualFile? {
            var fileSystem: AzureStorageVirtualFileSystem?
            var account: String? = null
            var accessKey: String? = null
            var fsType: AzureStorageVirtualFileSystem.VFSSupportStorageType? = null
            try {
                when (viewModel.deployStorageTypeSelection) {
                    SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT -> {
                        when (storageAccount) {
                            is ADLSGen2StorageAccount  -> {
                                fsType = AzureStorageVirtualFileSystem.VFSSupportStorageType.ADLSGen2
                                account = storageAccount.name
                                accessKey = storageAccount.primaryKey
                            }
                        }
                    }

                    SparkSubmitStorageType.ADLS_GEN2 -> {
                        fsType = AzureStorageVirtualFileSystem.VFSSupportStorageType.ADLSGen2
                        account = uploadRootPathUri.accountName
                        accessKey = adlsGen2Card.storageKeyField.text.trim()
                    }

                    SparkSubmitStorageType.ADLS_GEN2_FOR_OAUTH -> {
                        fsType = AzureStorageVirtualFileSystem.VFSSupportStorageType.ADLSGen2
                        account = uploadRootPathUri.accountName
                    }

                    else -> {
                    }
                }
            } catch (ex: IllegalArgumentException) {
                log().warn("Preparing file system encounter ", ex)
            }

            when (fsType) {
                AzureStorageVirtualFileSystem.VFSSupportStorageType.ADLSGen2 -> {
                    // for issue #3159, upload path maybe not ready if switching cluster fast so path is the last cluster's path
                    // if switching between gen2 clusters, need to check account is matched
                    if (uploadRootPathUri.accountName != account) {
                        return null
                    }

                    // Prepare HttpObservable for different cluster type
                    val http =
                        if (cluster is AzureAdAccountDetail) {
                            // Use Azure AD account to access storage data corresponding to Synapse Spark pool.
                            // In this way at least "Storage Blob Data Reader" role is required, or else we will get
                            // HTTP 403 Error when list files on the storage.
                            // https://docs.microsoft.com/en-us/azure/storage/common/storage-access-blobs-queues-portal
                            ADLSGen2OAuthHttpObservable(cluster.tenantId)
                        } else {
                            if (StringUtils.isBlank(accessKey)) {
                                return null
                            }
                            // Use account access key to access Gen2 storage data corresponding to
                            // HDInsight/Mfa/Linked HDInsight cluster. In this way at least "Storage Account Contributor"
                            // role is required, or else we will get HTTP 403 Error when list files on the storage
                            // https://docs.microsoft.com/en-us/azure/storage/common/storage-access-blobs-queues-portal
                            SharedKeyHttpObservable(account, accessKey)
                        }

                    fileSystem = ADLSGen2FileSystem(http, uploadRootPathUri)
                    return AdlsGen2VirtualFile(uploadRootPathUri, true, fileSystem)
                }
                else -> {
                    return null
                }
            }
        }
    }

    val viewModel = ViewModel().apply {
        Disposer.register(this@SparkSubmissionJobUploadStoragePanel, this@apply)
    }

    override fun dispose() {
    }
}