package com.microsoft.azuretools.azureexplorer.forms;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.IDialogConstants;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.serverexplore.AddHDInsightAdditionalClusterImpl;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.telemetry.AppInsightsClient;

public class AddNewClusterForm extends AzureTitleAreaDialogWrapper {

	private String clusterName;
	private String userName;
	private String password;

	private String storageName;
	private String storageKey;

	private HDStorageAccount storageAccount;

	private boolean isCarryOnNextStep;

	private Text clusterNameField;
	private Text userNameField;
	private Text storageNameField;
	private Text storageKeyField;
	private Text passwordField;

	private HDInsightRootModule hdInsightModule;

	private static final String URL_PREFIX = "https://";

	public AddNewClusterForm(Shell parentShell, HDInsightRootModule module) {
		super(parentShell);
		this.hdInsightModule = module;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Link New HDInsight Cluster");
	}

	// we cannot just override method 'helpPressed' since the method is defined as private in the parent class 'TrayDialog'
	// so we disable the default help button and mock a button with different usage
	protected void helpPressed() {
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("https://go.microsoft.com/fwlink/?linkid=866472"));
		} catch (PartInitException | MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private ToolBar createHelpImageButton(Composite parent, Image image) {
        ToolBar toolBar = new ToolBar(parent, SWT.FLAT | SWT.NO_FOCUS);
        ((GridLayout) parent.getLayout()).numColumns++;
		toolBar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		final Cursor cursor = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);
		toolBar.setCursor(cursor);
		toolBar.addDisposeListener(e -> cursor.dispose());
		ToolItem fHelpButton = new ToolItem(toolBar, SWT.CHECK);
		fHelpButton.setImage(image);
		fHelpButton.setToolTipText(JFaceResources.getString("helpToolTip")); //$NON-NLS-1$
		fHelpButton.addSelectionListener(widgetSelectedAdapter(e -> helpPressed()));
		return toolBar;
	}
	
	private Link createHelpLink(Composite parent) {
		Link link = new Link(parent, SWT.WRAP | SWT.NO_FOCUS);
        ((GridLayout) parent.getLayout()).numColumns++;
		link.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		link.setText("<a>"+IDialogConstants.HELP_LABEL+"</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		link.setToolTipText(IDialogConstants.HELP_LABEL);
		link.addSelectionListener(widgetSelectedAdapter(e -> helpPressed()));
		return link;
	}
	
	@Override
    protected Control createHelpControl(Composite parent) {
		Image helpImage = JFaceResources.getImage(DLG_IMG_HELP);
		if (helpImage != null) {
			return createHelpImageButton(parent, helpImage);
		}
		return createHelpLink(parent);
    }

	@Override
	protected Control createButtonBar(Composite parent) {
    	Composite composite = new Composite(parent, SWT.NONE);
    	GridLayout layout = new GridLayout();
    	layout.marginWidth = 0;
    	layout.marginHeight = 0;
    	layout.horizontalSpacing = 0;
    	composite.setLayout(layout);
    	composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    	composite.setFont(parent.getFont());

    	// mock a new help button
        Control helpControl = createHelpControl(composite);
        ((GridData) helpControl.getLayoutData()).horizontalIndent = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        Control buttonSection = super.createButtonBar(composite);
        ((GridData) buttonSection.getLayoutData()).grabExcessHorizontalSpace = true;
        return composite;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Link New HDInsight Cluster");
		setMessage("Please enter HDInsight Cluster details");
		// disable the default help button
		setHelpAvailable(false);
		
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		container.setLayout(gridLayout);
		GridData gridData = new GridData();
		gridData.widthHint = 350;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		container.setLayoutData(gridData);

		Label clusterNameLabel = new Label(container, SWT.LEFT);
		clusterNameLabel.setText("Cluster Name:");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		clusterNameLabel.setLayoutData(gridData);
		clusterNameField = new Text(container, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		clusterNameField.setLayoutData(gridData);
		clusterNameField.setToolTipText("The HDInsight cluster name, such as 'mycluster' of cluster URL 'mycluster.azurehdinsight.net'.\n\n Click the '?'(Help) button to get more details.");

		Label storageNameLabel = new Label(container, SWT.LEFT);
		storageNameLabel.setText("Storage Name:");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		storageNameLabel.setLayoutData(gridData);
		storageNameField = new Text(container, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		storageNameField.setLayoutData(gridData);
		storageNameField.setToolTipText("The default storage account of the HDInsight cluster, which can be found from HDInsight cluster properties of Azure portal.\n\n Click the '?'(Help) button to get more details");

		Label storageKeyLabel = new Label(container, SWT.LEFT);
		storageKeyLabel.setText("Storage Key:");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		storageKeyLabel.setLayoutData(gridData);
		storageKeyField = new Text(container, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		storageKeyField.setLayoutData(gridData);
		storageKeyField.setToolTipText("The storage key of the default storage account, which can be found from HDInsight cluster storage accounts of Azure portal.\n\n Click the '?'(Help) button to get more details.");

		Label userNameLabel = new Label(container, SWT.LEFT);
		userNameLabel.setText("User Name:");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		userNameLabel.setLayoutData(gridData);
		userNameField = new Text(container, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		userNameField.setLayoutData(gridData);
		userNameField.setToolTipText("The user name of the HDInsight cluster.\n\n Click the '?'(Help) button to get more details.");
		
		Label passwordLabel = new Label(container, SWT.LEFT);
		passwordLabel.setText("Password:");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		passwordLabel.setLayoutData(gridData);
		passwordField = new Text(container, SWT.PASSWORD | SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		passwordField.setLayoutData(gridData);
		passwordField.setToolTipText("The password of the HDInsight cluster user provided.\n\n Click the '?'(Help) button to get more details.");
		
		return super.createDialogArea(parent);
	}

	@Override
	protected void okPressed() {
		synchronized (AddNewClusterForm.class) {
			isCarryOnNextStep = true;
			setErrorMessage(null);

			AppInsightsClient.create(Messages.HDInsightAddNewClusterAction, null);

			String clusterNameOrUrl = clusterNameField.getText().trim();
			userName = userNameField.getText().trim();
			storageName = storageNameField.getText().trim();

			storageKey = storageKeyField.getText().trim();

			password = passwordField.getText();

			if (StringHelper.isNullOrWhiteSpace(clusterNameOrUrl) || StringHelper.isNullOrWhiteSpace(storageName)
					|| StringHelper.isNullOrWhiteSpace(storageKey) || StringHelper.isNullOrWhiteSpace(userName)
					|| StringHelper.isNullOrWhiteSpace(password)) {
				setErrorMessage("Cluster Name, Storage Key, User Name, or Password shouldn't be empty");
				isCarryOnNextStep = false;
			} else {
				clusterName = getClusterName(clusterNameOrUrl);

				if (clusterName == null) {
					setErrorMessage("Wrong cluster name or endpoint");
					isCarryOnNextStep = false;
				} else {
					int status = ClusterManagerEx.getInstance().isHDInsightAdditionalStorageExist(clusterName,
							storageName);
					if (status == 1) {
						setErrorMessage("Cluster already exist in current list");
						isCarryOnNextStep = false;
					} else if (status == 2) {
						setErrorMessage("Default storage account is required");
						isCarryOnNextStep = false;
					}
				}
			}

			if (isCarryOnNextStep) {
				getStorageAccount();
			}

			if (isCarryOnNextStep) {
				if (storageAccount != null) {
					HDInsightAdditionalClusterDetail hdInsightAdditionalClusterDetail = new HDInsightAdditionalClusterDetail(
							clusterName, userName, password, storageAccount);
					ClusterManagerEx.getInstance().addHDInsightAdditionalCluster(hdInsightAdditionalClusterDetail);
					hdInsightModule.refreshWithoutAsync();
				}
				super.okPressed();
			}
		}
	}

	// format input string
	private static String getClusterName(String userNameOrUrl) {
		if (userNameOrUrl.startsWith(URL_PREFIX)) {
			return StringHelper.getClusterNameFromEndPoint(userNameOrUrl);
		} else {
			return userNameOrUrl;
		}
	}

	private void getStorageAccount() {
		PluginUtil.showBusy(true, getShell());
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					storageAccount = AddHDInsightAdditionalClusterImpl.getStorageAccount(clusterName, storageName,
							storageKey, userName, password);
					isCarryOnNextStep = true;
				} catch (AzureCmdException | HDIException e) {
					isCarryOnNextStep = false;
					setErrorMessage(e.getMessage());
				}
			}
		});
		PluginUtil.showBusy(false, getShell());
	}
	
}
