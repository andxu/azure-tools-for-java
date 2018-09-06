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
package com.microsoft.azuretools.webapp.ui;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.JdkModel;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azuretools.core.ui.ErrorWindow;
import com.microsoft.azuretools.core.utils.MavenUtils;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.core.utils.ProgressDialog;
import com.microsoft.azuretools.core.utils.UpdateProgressIndicator;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.azuretools.webapp.Activator;

public class AppServiceCreateDialog extends AzureTitleAreaDialogWrapper {

    private static final String WEB_CONFIG_PACKAGE_PATH = "/webapp/web.config";

    // validation error string constants
    private static final String SELECT_WEB_CONTAINER = "Select a valid web container.";
    private static final String SELECT_JAVA_VERSION = "Select Java version.";
    private static final String SELECT_RESOURCE_GROUP = "Select a valid resource group.";
    private static final String ENTER_RESOURCE_GROUP = "Enter a valid resource group name";
    private static final String SELECT_APP_SERVICE_PLAN = "Select a valid App Service Plan.";
    private static final String SELECT_LOCATION = "Select a location.";
    private static final String SELECT_A_VALID_SUBSCRIPTION = "Select a valid subscription.";
    private static final String ENTER_APP_SERVICE_PLAN_NAME = "Enter a valid App Service Plan name.";
    private static final String NAME_ALREADY_TAKEN = "The name is already taken";
    private static final String APP_SERVICE_PLAN_NAME_MUST_UNUQUE = "App service plan name must be unuque in each subscription.";
    private static final String APP_SERVICE_PLAN_NAME_INVALID_MSG = "App Service Plan name can only include alphanumeric characters and hyphens.";
    private static final String RESOURCE_GROUP_NAME_INVALID_MSG = "Resounce group name can only include alphanumeric characters, periods, underscores, hyphens, and parenthesis and can't end in a period.";
    private static final String WEB_APP_NAME_INVALID_MSG = "The name can contain letters, numbers and hyphens but the first and last characters must be a letter or number. The length must be between 2 and 60 characters.";

    // validation regex
    private static final String WEB_APP_NAME_REGEX = "^[A-Za-z0-9][A-Za-z0-9-]*[A-Za-z0-9]$";
    private static final String APP_SERVICE_PLAN_NAME_REGEX = "^[A-Za-z0-9-]*[A-Za-z0-9-]$";
    private static final String RESOURCE_GROUP_NAME_REGEX = "^[A-Za-z0-9-_()\\.]*[A-Za-z0-9-_()]$";

    // widgets
    private static final String LBL_WEB_CONTAINER = "Web container";
    private static final String LBL_JAVA = "Java version";
    private static final String LBL_PRICING_TIER = "Pricing tier";
    private static final String LBL_LOCATION = "Location";
    private static final String LBL_APP_NAME = "Enter name";
    private static final String LBL_SUBSCRIPTION = "Subscription";

    private static final String TXT_APP_NAME_MSG = "<enter name>";

    private static final String BTN_USE_EXISTING = "Use existing";
    private static final String BTN_CREATE_NEW = "Create new";
    private static final String BTN_OK = "Create";

    private static final String TAB_APP_SERVICE_PLAN = "App service plan";
    private static final String TAB_RESOURCE_GROUP = "Resource group";
    private static final String TAB_JAVA = "Java";

    private static final String PRICING_URL = "https://azure.microsoft.com/en-us/pricing/details/app-service/";
    private static final String LNK_PRICING = "<a>App service pricing details</a>";
    private static final String NOT_AVAILABLE = "N/A";
    private static final String RESOURCE_GROUP_PREFIX = "rg-webapp-";
    private static final String APP_SERVICE_PLAN_PREFIX = "asp-";
    private static final String URL_SUFFIX = ".azurewebsites.net";
    private static final String WEB_APP_PREFIX = "webapp-";
    private static final String DATE_FORMAT = "yyMMddHHmmss";

    // dialog
    private static final String FORM_VALIDATION_ERROR = "Form validation error.";
    private static final String CREATING_APP_SERVICE = "Creating App Service....";
    private static final String VALIDATING_FORM_FIELDS = "Validating Form Fields....";
    private static final String CREATE_APP_SERVICE_PROGRESS_TITLE = "Create App Service Progress";
    private static final String ERROR_DIALOG_TITLE = "Create App Service Error";
    private static final String UPDATING_AZURE_LOCAL_CACHE = "Updating Azure local cache...";
    private static final String GETTING_APP_SERVICES = "Getting App Services...";
    private static final String DIALOG_TITLE = "Create App Service";
    private static final String DIALOG_MESSAGE = "Create Azure App Service";

    private static ILog LOG = Activator.getDefault().getLog();

    private IProject project;

    private Text textAppName;
    private Text textResourceGroupName;
    private Text textAppSevicePlanName;
    private Combo comboWebContainer;
    private Combo comboSubscription;
    private Combo comboResourceGroup;
    private Combo comboAppServicePlan;
    private Combo comboAppServicePlanLocation;
    private Combo comboAppServicePlanPricingTier;
    private Combo cbJavaVersion;
    private Label lblJavaVersion;
    private Label lblAppSevicePlanLocation;
    private Label lblAppServicePlanPricingTier;
    private Label lblWebContainer;
    private Label lblExistingAppServicePlanName;
    private Label lblNewAppServicePlanName;

    private ControlDecoration dec_textAppName;
    private ControlDecoration dec_textNewResGrName;
    private ControlDecoration dec_textAppSevicePlanName;
    private ControlDecoration dec_comboWebContainer;
    private ControlDecoration dec_comboSubscription;
    private ControlDecoration dec_comboSelectResGr;
    private ControlDecoration dec_comboAppServicePlan;
    private ControlDecoration dec_comboAppServicePlanLocation;
    // private ControlDecoration dec_comboAppServicePlanPricingTier;
    private ControlDecoration dec_cbJavaVersion;

    // controls to types bindings by index
    private List<WebAppUtils.WebContainerMod> binderWebConteiners;
    private List<SubscriptionDetail> binderSubscriptionDetails;
    private List<ResourceGroup> binderResourceGroup;
    private List<AppServicePlan> binderAppServicePlan;
    private List<Location> binderAppServicePlanLocation;
    private List<PricingTier> binderAppServicePlanPricingTier;
    private List<JdkModel> javaVersions;

    private TabFolder tabFolder;

    private TabItem tabItemAppServicePlan;
    private Composite compositeAppServicePlan;
    private Button btnAppServiceCreateNew;
    private Label lblAppServiceCreateNewPricingTier;
    private Label lblAppServiceCreateNewLocation;

    private Button btnAppServiceUseExisting;
    private Label lblAppServiceUseExictingLocation;
    private Label lblAppServiceUseExistiogPrisingTier;
    private Link linkAppServicePricing;

    private TabItem tabItemResourceGroup;
    private Composite compositeResourceGroup;
    private Button btnResourceGroupCreateNew;
    private Button btnResourceGroupUseExisting;

    private TabItem tabItemJDK;
    private Composite compositeJDK;

    protected WebApp webApp;
    private String packaging = WebAppUtils.TYPE_WAR;

    public WebApp getWebApp() {
        return this.webApp;
    }

    public static AppServiceCreateDialog go(Shell parentShell, IProject project) {
        AppServiceCreateDialog d = new AppServiceCreateDialog(parentShell, project);
        if (d.open() == Window.OK) {
            return d;
        }
        return null;
    }

    private AppServiceCreateDialog(Shell parentShell, IProject project) {
        super(parentShell);
        setHelpAvailable(false);
        this.project = project;
        setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        Image image = PluginUtil.getImage("icons/large/Azure.png");
        if (image != null) {
            setTitleImage(image);
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage(DIALOG_MESSAGE);
        setTitle(DIALOG_TITLE);
        Composite area = (Composite) super.createDialogArea(parent);

        Composite composite = new Composite(area, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

        Group grpAppService = new Group(composite, SWT.NONE);
        grpAppService.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        grpAppService.setLayout(new GridLayout(3, false));

        Label lblAppName = new Label(grpAppService, SWT.NONE);
        lblAppName.setText(LBL_APP_NAME);

        textAppName = new Text(grpAppService, SWT.BORDER);
        textAppName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });
        textAppName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textAppName.setMessage(TXT_APP_NAME_MSG);
        dec_textAppName = decorateContorolAndRegister(textAppName);

        Label lblazurewebsitescom = new Label(grpAppService, SWT.NONE);
        lblazurewebsitescom.setText(URL_SUFFIX);

        Label lblSubscription = new Label(grpAppService, SWT.NONE);
        lblSubscription.setText(LBL_SUBSCRIPTION);

        comboSubscription = new Combo(grpAppService, SWT.READ_ONLY);
        comboSubscription.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fillResourceGroups();
                fillAppServicePlans();
                fillAppServicePlansDetails();
                fillAppServicePlanLocations();
            }
        });
        dec_comboSubscription = decorateContorolAndRegister(comboSubscription);
        comboSubscription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        // ====

        tabFolder = new TabFolder(composite, SWT.NONE);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        tabItemAppServicePlan = new TabItem(tabFolder, SWT.NONE);
        tabItemAppServicePlan.setText(TAB_APP_SERVICE_PLAN);

        compositeAppServicePlan = new Composite(tabFolder, SWT.NONE);
        tabItemAppServicePlan.setControl(compositeAppServicePlan);
        compositeAppServicePlan.setLayout(new GridLayout(2, false));

        btnAppServiceCreateNew = new Button(compositeAppServicePlan, SWT.RADIO);
        btnAppServiceCreateNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioAppServicePlanLogic();
            }
        });
        btnAppServiceCreateNew.setSelection(true);
        btnAppServiceCreateNew.setBounds(0, 0, 90, 16);
        btnAppServiceCreateNew.setText(BTN_CREATE_NEW);
        new Label(compositeAppServicePlan, SWT.NONE);

        lblNewAppServicePlanName = new Label(compositeAppServicePlan, SWT.NONE);
        GridData gd_lblNewAppServicePlanName = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblNewAppServicePlanName.horizontalIndent = 15;
        lblNewAppServicePlanName.setLayoutData(gd_lblNewAppServicePlanName);
        lblNewAppServicePlanName.setText("Name");

        textAppSevicePlanName = new Text(compositeAppServicePlan, SWT.BORDER);
        textAppSevicePlanName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });
        textAppSevicePlanName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textAppSevicePlanName.setMessage(TXT_APP_NAME_MSG);
        dec_textAppSevicePlanName = decorateContorolAndRegister(textAppSevicePlanName);
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        String date = df.format(new Date());
        textAppSevicePlanName.setText(APP_SERVICE_PLAN_PREFIX + date);

        lblAppServiceCreateNewLocation = new Label(compositeAppServicePlan, SWT.NONE);
        GridData gd_lblAppServiceCreateNewLocation = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAppServiceCreateNewLocation.horizontalIndent = 15;
        lblAppServiceCreateNewLocation.setLayoutData(gd_lblAppServiceCreateNewLocation);
        lblAppServiceCreateNewLocation.setText(LBL_LOCATION);

        comboAppServicePlanLocation = new Combo(compositeAppServicePlan, SWT.READ_ONLY);
        comboAppServicePlanLocation.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });
        comboAppServicePlanLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        dec_comboAppServicePlanLocation = decorateContorolAndRegister(comboAppServicePlanLocation);

        lblAppServiceCreateNewPricingTier = new Label(compositeAppServicePlan, SWT.NONE);
        GridData gd_lblAppServiceCreateNewPricingTier = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAppServiceCreateNewPricingTier.horizontalIndent = 15;
        lblAppServiceCreateNewPricingTier.setLayoutData(gd_lblAppServiceCreateNewPricingTier);
        lblAppServiceCreateNewPricingTier.setText(LBL_PRICING_TIER);

        comboAppServicePlanPricingTier = new Combo(compositeAppServicePlan, SWT.READ_ONLY);
        comboAppServicePlanPricingTier.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        tabItemResourceGroup = new TabItem(tabFolder, SWT.NONE);
        tabItemResourceGroup.setText(TAB_RESOURCE_GROUP);

        compositeResourceGroup = new Composite(tabFolder, SWT.NONE);
        tabItemResourceGroup.setControl(compositeResourceGroup);
        compositeResourceGroup.setLayout(new GridLayout(2, false));

        btnResourceGroupCreateNew = new Button(compositeResourceGroup, SWT.RADIO);
        btnResourceGroupCreateNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioResourceGroupLogic();
            }
        });
        btnResourceGroupCreateNew.setSelection(true);
        btnResourceGroupCreateNew.setText(BTN_CREATE_NEW);

        textResourceGroupName = new Text(compositeResourceGroup, SWT.BORDER);
        textResourceGroupName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });
        textResourceGroupName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textResourceGroupName.setBounds(0, 0, 64, 19);
        textResourceGroupName.setMessage(TXT_APP_NAME_MSG);
        dec_textNewResGrName = decorateContorolAndRegister(textResourceGroupName);

        btnResourceGroupUseExisting = new Button(compositeResourceGroup, SWT.RADIO);
        btnResourceGroupUseExisting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioResourceGroupLogic();
            }
        });
        btnResourceGroupUseExisting.setText(BTN_USE_EXISTING);

        comboResourceGroup = new Combo(compositeResourceGroup, SWT.READ_ONLY);
        comboResourceGroup.setEnabled(false);
        comboResourceGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboResourceGroup.setBounds(0, 0, 26, 22);
        dec_comboSelectResGr = decorateContorolAndRegister(comboResourceGroup);

        tabItemJDK = new TabItem(tabFolder, SWT.NONE);
        tabItemJDK.setText(TAB_JAVA);

        compositeJDK = new Composite(tabFolder, SWT.NONE);
        tabItemJDK.setControl(compositeJDK);
        compositeJDK.setLayout(new GridLayout(2, false));

        lblJavaVersion = new Label(compositeJDK, SWT.NONE);
        lblJavaVersion.setText(LBL_JAVA);

        cbJavaVersion = new Combo(compositeJDK, SWT.READ_ONLY);
        cbJavaVersion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        dec_cbJavaVersion = decorateContorolAndRegister(cbJavaVersion);

        lblWebContainer = new Label(compositeJDK, SWT.NONE);
        lblWebContainer.setText(LBL_WEB_CONTAINER);

        comboWebContainer = new Combo(compositeJDK, SWT.READ_ONLY);
        comboWebContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        dec_comboWebContainer = decorateContorolAndRegister(comboWebContainer);

        textAppName.setText(WEB_APP_PREFIX + date);

        btnAppServiceUseExisting = new Button(compositeAppServicePlan, SWT.RADIO);
        btnAppServiceUseExisting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioAppServicePlanLogic();
            }
        });
        btnAppServiceUseExisting.setText(BTN_USE_EXISTING);
        new Label(compositeAppServicePlan, SWT.NONE);

        lblExistingAppServicePlanName = new Label(compositeAppServicePlan, SWT.NONE);
        GridData gd_lblExistingAppServicePlanName = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblExistingAppServicePlanName.horizontalIndent = 15;
        lblExistingAppServicePlanName.setLayoutData(gd_lblExistingAppServicePlanName);
        lblExistingAppServicePlanName.setText("Name");
        lblExistingAppServicePlanName.setEnabled(false);

        comboAppServicePlan = new Combo(compositeAppServicePlan, SWT.READ_ONLY);
        comboAppServicePlan.setEnabled(false);
        comboAppServicePlan.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboAppServicePlan.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fillAppServicePlansDetails();
            }
        });
        comboAppServicePlan.setBounds(0, 0, 26, 22);
        dec_comboAppServicePlan = decorateContorolAndRegister(comboAppServicePlan);

        lblAppServiceUseExictingLocation = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppServiceUseExictingLocation.setEnabled(false);
        GridData gd_lblAppServiceUseExictingLocation = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAppServiceUseExictingLocation.horizontalIndent = 15;
        lblAppServiceUseExictingLocation.setLayoutData(gd_lblAppServiceUseExictingLocation);
        lblAppServiceUseExictingLocation.setText(LBL_LOCATION);

        lblAppSevicePlanLocation = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppSevicePlanLocation.setEnabled(false);
        lblAppSevicePlanLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        lblAppSevicePlanLocation.setText(NOT_AVAILABLE);

        lblAppServiceUseExistiogPrisingTier = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppServiceUseExistiogPrisingTier.setEnabled(false);
        GridData gd_lblAppServiceUseExistiogPrisingTier = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAppServiceUseExistiogPrisingTier.horizontalIndent = 15;
        lblAppServiceUseExistiogPrisingTier.setLayoutData(gd_lblAppServiceUseExistiogPrisingTier);
        lblAppServiceUseExistiogPrisingTier.setText(LBL_PRICING_TIER);

        lblAppServicePlanPricingTier = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppServicePlanPricingTier.setEnabled(false);
        lblAppServicePlanPricingTier.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        lblAppServicePlanPricingTier.setText(NOT_AVAILABLE);
        new Label(compositeAppServicePlan, SWT.NONE);
        // dec_comboAppServicePlanPricingTier =
        // decorateContorolAndRegister(comboAppServicePlanPricingTier);

        linkAppServicePricing = new Link(compositeAppServicePlan, SWT.NONE);
        linkAppServicePricing.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        linkAppServicePricing.setText(LNK_PRICING);
        linkAppServicePricing.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(PRICING_URL));
                } catch (PartInitException | MalformedURLException ex) {
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                            "widgetSelected@SelectionAdapter@linkAppServicePricing@AppServiceCreateDialog", ex));
                }
            }
        });
        textResourceGroupName.setText(RESOURCE_GROUP_PREFIX + date);

        fillWebContainers();
        fillSubscriptions();
        fillResourceGroups();
        fillAppServicePlans();
        fillAppServicePlansDetails();
        fillAppServicePlanLocations();
        fillAppServicePlanPricingTiers();
        fillJavaVersion();

        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        cleanError();
        super.createButtonsForButtonBar(parent);
        Button okButton = getButton(IDialogConstants.OK_ID);
        okButton.setText(BTN_OK);
    }

    private void radioAppServicePlanLogic() {
        cleanError();
        boolean enabled = btnAppServiceCreateNew.getSelection();

        // btnAppServiceCreateNew.setEnabled(enabled);
        textAppSevicePlanName.setEnabled(enabled);

        lblNewAppServicePlanName.setEnabled(enabled);
        lblAppServiceCreateNewLocation.setEnabled(enabled);
        comboAppServicePlanLocation.setEnabled(enabled);

        lblAppServiceCreateNewPricingTier.setEnabled(enabled);
        comboAppServicePlanPricingTier.setEnabled(enabled);

        // btnAppServiceUseExisting.setEnabled(!enabled);
        comboAppServicePlan.setEnabled(!enabled);

        lblAppServiceUseExictingLocation.setEnabled(!enabled);
        lblExistingAppServicePlanName.setEnabled(!enabled);
        lblAppSevicePlanLocation.setEnabled(!enabled);

        lblAppServiceUseExistiogPrisingTier.setEnabled(!enabled);
        lblAppServicePlanPricingTier.setEnabled(!enabled);
    }

    private void radioResourceGroupLogic() {
        cleanError();
        boolean enabled = btnResourceGroupCreateNew.getSelection();
        textResourceGroupName.setEnabled(enabled);
        comboResourceGroup.setEnabled(!enabled);
    }

    protected void fillWebContainers() {
        try {
            if (MavenUtils.isMavenProject(project)) {
                packaging = MavenUtils.getPackaging(project);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "fillWebContainers@AppServiceCreateDialog", e));
        }
        if (packaging.equals(WebAppUtils.TYPE_JAR)) {
            lblWebContainer.setVisible(false);
            comboWebContainer.setVisible(false);
        } else {
            comboWebContainer.removeAll();
            binderWebConteiners = new ArrayList<>();
            for (WebAppUtils.WebContainerMod wc : WebAppUtils.WebContainerMod.values()) {
                comboWebContainer.add(wc.toString());
                binderWebConteiners.add(wc);
            }

            if (comboWebContainer.getItemCount() > 0) {
                comboWebContainer.select(0);
            }
        }
    }

    protected static <T> List<T> createListFromClassFields(Class<?> c) throws IllegalAccessException {
        List<T> list = new LinkedList<T>();

        Field[] declaredFields = c.getDeclaredFields();
        for (Field field : declaredFields) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && Modifier.isPublic(modifiers)) {
                @SuppressWarnings("unchecked")
                T value = (T) field.get(null);
                list.add(value);
            }
        }
        return list;
    }

    protected void fillSubscriptions() {
        if (AzureModel.getInstance().getResourceGroupToWebAppMap() == null) {
            updateAndFillSubscriptions();
        } else {
            doFillSubscriptions();
        }
    }

    private void updateAndFillSubscriptions() {
        try {
            ProgressDialog.get(this.getShell(), GETTING_APP_SERVICES).run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) {
                    monitor.beginTask(UPDATING_AZURE_LOCAL_CACHE, IProgressMonitor.UNKNOWN);
                    if (monitor.isCanceled()) {
                        AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                AppServiceCreateDialog.super.cancelPressed();
                            }
                        });
                    }

                    try {
                        AzureModelController.updateResourceGroupMaps(new UpdateProgressIndicator(monitor));
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                doFillSubscriptions();
                            };
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                                "run@ProgressDialog@updateAndFillSubscriptions@AppServiceCreateDialog", ex));
                    }
                }
            });
        } catch (InvocationTargetException | InterruptedException ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "updateAndFillSubscriptions@AppServiceCreateDialog",
                    ex));
        }
    }

    private void doFillSubscriptions() {
        try {
            // reset model
            Set<SubscriptionDetail> sdl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().keySet();
            if (sdl == null) {
                System.out.println("sdl is null");
                return;
            }

            comboSubscription.removeAll();
            binderSubscriptionDetails = new ArrayList<SubscriptionDetail>();
            for (SubscriptionDetail sd : sdl) {
                if (sd != null && sd.isSelected()) {
                    comboSubscription.add(sd.getSubscriptionName());
                    binderSubscriptionDetails.add(sd);
                }
            }
            if (comboSubscription.getItemCount() > 0) {
                comboSubscription.select(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "doFillSubscriptions@AppServiceCreateDialog", ex));
        }
    }

    protected void fillResourceGroups() {
        int i = comboSubscription.getSelectionIndex();
        if (i < 0) { // empty
            System.out.println("No subscription is selected");
            return;
        }

        List<ResourceGroup> rgl = AzureModel.getInstance().getSubscriptionToResourceGroupMap()
                .get(binderSubscriptionDetails.get(i));
        if (rgl == null) {
            System.out.println("rgl is null");
            return;
        }

        comboResourceGroup.removeAll();
        binderResourceGroup = new ArrayList<ResourceGroup>();
        for (ResourceGroup rg : rgl) {
            comboResourceGroup.add(rg.name());
            binderResourceGroup.add(rg);
        }

        if (comboResourceGroup.getItemCount() > 0) {
            comboResourceGroup.select(0);
        }
    }

    protected void fillAppServicePlans() {
        int i = comboSubscription.getSelectionIndex();
        if (i < 0) { // empty
            System.out.println("No subscription is selected");
            return;
        }

        List<ResourceGroup> rgl = AzureModel.getInstance().getSubscriptionToResourceGroupMap()
                .get(binderSubscriptionDetails.get(i));
        if (rgl == null) {
            System.out.println("rgl is null");
            return;
        }

        comboAppServicePlan.removeAll();
        binderAppServicePlan = new ArrayList<AppServicePlan>();
        for (ResourceGroup rg : rgl) {
            List<AppServicePlan> aspl = AzureModel.getInstance().getResourceGroupToAppServicePlanMap().get(rg);
            if (aspl != null) {
                for (AppServicePlan asp : aspl) {
                    if (asp != null
                            && asp.pricingTier().toSkuDescription().tier().compareToIgnoreCase("dynamic") != 0) {
                        binderAppServicePlan.add(asp);
                        comboAppServicePlan.add(asp.name());
                    }
                }
            }
        }

        if (comboAppServicePlan.getItemCount() > 0) {
            comboAppServicePlan.select(0);
        }
    }

    protected void fillAppServicePlansDetails() {
        int i = comboAppServicePlan.getSelectionIndex();
        if (i < 0) {
            lblAppSevicePlanLocation.setText(NOT_AVAILABLE);
            lblAppServicePlanPricingTier.setText(NOT_AVAILABLE);
        } else {
            AppServicePlan asp = binderAppServicePlan.get(i);
            lblAppSevicePlanLocation.setText(asp.region().label());
            lblAppServicePlanPricingTier.setText(asp.pricingTier().toString());
        }
    }

    protected void fillAppServicePlanLocations() {
        int i = comboSubscription.getSelectionIndex();
        if (i < 0) { // empty
            System.out.println("No subscription is selected");
            return;
        }

        comboAppServicePlanLocation.removeAll();
        binderAppServicePlanLocation = new ArrayList<Location>();
        // List<Location> locl =
        // AzureModel.getInstance().getSubscriptionToLocationMap().get(binderSubscriptionDetails.get(i));
        Map<SubscriptionDetail, List<Location>> sdlocMap = AzureModel.getInstance().getSubscriptionToLocationMap();
        SubscriptionDetail sd = binderSubscriptionDetails.get(i);
        List<Location> locl = sdlocMap.get(sd);
        if (locl != null) {
            for (Location loc : locl) {
                comboAppServicePlanLocation.add(loc.displayName());
                binderAppServicePlanLocation.add(loc);
            }
        }

        if (comboAppServicePlanLocation.getItemCount() > 0) {
            comboAppServicePlanLocation.select(0);
        }
    }

    protected void fillAppServicePlanPricingTiers() {
        try {
            comboAppServicePlanPricingTier.removeAll();
            binderAppServicePlanPricingTier = new ArrayList<PricingTier>();
            List<PricingTier> l = createListFromClassFields(PricingTier.class);
            for (PricingTier aspt : l) {
                if (aspt != null) {
                    comboAppServicePlanPricingTier.add(aspt.toString());
                    binderAppServicePlanPricingTier.add(aspt);
                }
            }
            if (comboAppServicePlanPricingTier.getItemCount() > 0) {
                comboAppServicePlanPricingTier.select(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    "fillAppServicePlanPricingTiers@AppServiceCreateDialog", ex));
        }
    }

    protected void fillJavaVersion() {
        javaVersions = AzureWebAppMvpModel.getInstance().listJdks();
        for (JdkModel jdk : javaVersions) {
            if (jdk != null) {
                cbJavaVersion.add(jdk.toString());
            }
        }
        if (cbJavaVersion.getItemCount() > 0) {
            cbJavaVersion.select(0);
        }
    }

    @Override
    protected void okPressed() {
        String errTitle = ERROR_DIALOG_TITLE;
        cleanError();
        collectData();
        try {
            ProgressDialog.get(this.getShell(), CREATE_APP_SERVICE_PROGRESS_TITLE).run(true, true,
                    new IRunnableWithProgress() {
                        @Override
                        public void run(IProgressMonitor monitor) {
                            monitor.beginTask(VALIDATING_FORM_FIELDS, IProgressMonitor.UNKNOWN);
                            if (!validated()) {
                                return;
                            }
                            monitor.setTaskName(CREATING_APP_SERVICE);
                            if (monitor.isCanceled()) {
                                AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                                Display.getDefault().asyncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        AppServiceCreateDialog.super.cancelPressed();
                                    }
                                });
                            }
                            try {
                                webApp = AzureWebAppMvpModel.getInstance().createWebAppOnWindows(model);
                                if (webApp != null && packaging.equals(WebAppUtils.TYPE_JAR)) {
                                    webApp.stop();
                                    try (InputStream webConfigInput = WebAppUtils.class
                                            .getResourceAsStream(WEB_CONFIG_PACKAGE_PATH)) {
                                        WebAppUtils.uploadWebConfig(webApp, webConfigInput,
                                                new UpdateProgressIndicator(monitor));
                                    }
                                    webApp.start();
                                }
                                monitor.setTaskName(UPDATING_AZURE_LOCAL_CACHE);
                                AzureModelController.updateResourceGroupMaps(new UpdateProgressIndicator(monitor));
                                Display.getDefault().asyncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        AppServiceCreateDialog.super.okPressed();
                                    };
                                });
                                if (AzureUIRefreshCore.listeners != null) {
                                    AzureUIRefreshCore.execute(
                                            new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null));
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                                        "run@ProgressDialog@okPressed@AppServiceCreateDialog", ex));
                                Display.getDefault().asyncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        ErrorWindow.go(getShell(), ex.getMessage(), errTitle);
                                        ;
                                    }
                                });

                            }
                        }
                    });
        } catch (InvocationTargetException | InterruptedException ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "okPressed@AppServiceCreateDialog", ex));
            ErrorWindow.go(getShell(), ex.getMessage(), errTitle);
            ;
        }
    }

    private List<ControlDecoration> decorations = new LinkedList<ControlDecoration>();

    protected ControlDecoration decorateContorolAndRegister(Control c) {
        ControlDecoration d = new ControlDecoration(c, SWT.TOP | SWT.LEFT);
        FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
        Image img = fieldDecoration.getImage();
        d.setImage(img);
        d.hide();
        decorations.add(d);
        return d;
    }

    protected void setError(ControlDecoration d, String message) {
        Display.getDefault().asyncExec(() -> {
            d.setDescriptionText(message);
            setErrorMessage(FORM_VALIDATION_ERROR);
            d.show();
        });
    }

    protected void cleanError() {
        for (ControlDecoration d : decorations) {
            d.hide();
        }
        setErrorMessage(null);
    }

    protected boolean validated() {
        String webappName = model.getWebAppName();
        if (webappName.length() > 60 || !webappName.matches(WEB_APP_NAME_REGEX)) {
            setError(dec_textAppName, WEB_APP_NAME_INVALID_MSG);
            return false;
        } else if (AzureModel.getInstance().getResourceGroupToWebAppMap() != null) {
            for (List<WebApp> wal : AzureModel.getInstance().getResourceGroupToWebAppMap().values()) {
                for (WebApp wa : wal) {
                    if (wa != null && wa.name().toLowerCase().equals(webappName.toLowerCase())) {
                        setError(dec_textAppName, NAME_ALREADY_TAKEN);
                        return false;
                    }
                }
            }
        }

        if (model.getSubscriptionId() == null || model.getSubscriptionId().isEmpty()) {
            setError(dec_comboSubscription, SELECT_A_VALID_SUBSCRIPTION);
            return false;
        }

        if (model.isCreatingAppServicePlan()) {
            if (model.getAppServicePlanName().isEmpty()) {
                setError(dec_textAppSevicePlanName, ENTER_APP_SERVICE_PLAN_NAME);
                return false;
            } else {
                if (!model.getAppServicePlanName().matches(APP_SERVICE_PLAN_NAME_REGEX)) {
                    setError(dec_textAppSevicePlanName, APP_SERVICE_PLAN_NAME_INVALID_MSG);
                    return false;
                }
                // App service plan name must be unique in each subscription
                List<ResourceGroup> rgl = AzureMvpModel.getInstance()
                        .getResourceGroupsBySubscriptionId(model.getSubscriptionId());
                for (ResourceGroup rg : rgl) {
                    List<AppServicePlan> aspl = AzureWebAppMvpModel.getInstance()
                            .listAppServicePlanBySubscriptionIdAndResourceGroupName(model.getSubscriptionId(),
                                    rg.name());
                    for (AppServicePlan asp : aspl) {
                        if (asp != null
                                && asp.name().toLowerCase().equals(model.getAppServicePlanName().toLowerCase())) {
                            setError(dec_textAppSevicePlanName, APP_SERVICE_PLAN_NAME_MUST_UNUQUE);
                            return false;
                        }
                    }
                }
            }
            if (model.getRegion() == null || model.getRegion().isEmpty()) {
                setError(dec_comboAppServicePlanLocation, SELECT_LOCATION);
                return false;
            }
        } else {
            if (model.getAppServicePlanId() == null || model.getAppServicePlanId().isEmpty()) {
                setError(dec_comboAppServicePlan, SELECT_APP_SERVICE_PLAN);
                return false;
            }
        }

        if (model.isCreatingResGrp()) {
            if (model.getResourceGroup() == null || model.getResourceGroup().isEmpty()) {
                setError(dec_textNewResGrName, ENTER_RESOURCE_GROUP);
                return false;
            }
            if (!model.getResourceGroup().matches(RESOURCE_GROUP_NAME_REGEX)) {
                setError(dec_textNewResGrName, RESOURCE_GROUP_NAME_INVALID_MSG);
                return false;
            }
            for (ResourceGroup rg : AzureMvpModel.getInstance()
                    .getResourceGroupsBySubscriptionId(model.getSubscriptionId())) {
                if (rg != null && rg.name().toLowerCase().equals(model.getResourceGroup().toLowerCase())) {
                    setError(dec_textNewResGrName, NAME_ALREADY_TAKEN);
                    return false;
                }
            }
        } else {
            if (model.getResourceGroup() == null || model.getResourceGroup().isEmpty()) {
                setError(dec_comboSelectResGr, SELECT_RESOURCE_GROUP);
                return false;
            }
        }

        if (model.getJdkVersion() == null) {
            setError(dec_cbJavaVersion, SELECT_JAVA_VERSION);
            return false;
        }

        if (model.getWebContainer() == null || model.getWebContainer().isEmpty()) {
            setError(dec_comboWebContainer, SELECT_WEB_CONTAINER);
            return false;
        }

        return true;
    }

    private void collectData() {
        model.setCreatingNew(true);
        model.setWebAppName(textAppName.getText().trim());
        int index;
        index = comboSubscription.getSelectionIndex();
        model.setSubscriptionId(index < 0 ? null : binderSubscriptionDetails.get(index).getSubscriptionId());

        // Resource Group
        boolean isCreatingNewResGrp = btnResourceGroupCreateNew.getSelection();
        model.setCreatingResGrp(isCreatingNewResGrp);
        if (isCreatingNewResGrp) {
            model.setResourceGroup(textResourceGroupName.getText().trim());
        } else {
            index = comboResourceGroup.getSelectionIndex();
            model.setResourceGroup(index < 0 ? null : binderResourceGroup.get(index).name());
        }

        // App Service Plan
        boolean isCreatingAppServicePlan = btnAppServiceCreateNew.getSelection();
        model.setCreatingAppServicePlan(isCreatingAppServicePlan);
        if (isCreatingAppServicePlan) {
            model.setAppServicePlanName(textAppSevicePlanName.getText().trim());

            index = comboAppServicePlanLocation.getSelectionIndex();
            model.setRegion(index < 0 ? null : binderAppServicePlanLocation.get(index).name());

            index = comboAppServicePlanPricingTier.getSelectionIndex();
            model.setPricing(index < 0 ? null : binderAppServicePlanPricingTier.get(index).toString());
        } else {
            index = comboAppServicePlan.getSelectionIndex();
            model.setAppServicePlanId(index < 0 ? null : binderAppServicePlan.get(index).id());
        }

        // Java
        index = cbJavaVersion.getSelectionIndex();
        model.setJdkVersion(index < 0 ? null : javaVersions.get(index).getJavaVersion());

        if (packaging.equals(WebAppUtils.TYPE_JAR)) {
            model.setWebContainer(WebContainer.TOMCAT_8_5_NEWEST.toString());
        } else {
            index = comboWebContainer.getSelectionIndex();
            model.setWebContainer(index < 0 ? null : binderWebConteiners.get(index).toWebContainer().toString());
        }
    }

    protected WebAppSettingModel model = new WebAppSettingModel();
}
