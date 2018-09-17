package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import java.io.IOException;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.AzureUIRefreshListener;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebAppModule extends AzureRefreshableNode implements WebAppModuleView {
    private static final String REDIS_SERVICE_MODULE_ID = WebAppModule.class.getName();
    private static final String ICON_PATH = "WebApp_16.png";
    private static final String BASE_MODULE_NAME = "Web Apps";
    private final WebAppModulePresenter<WebAppModule> webAppModulePresenter;

    /**
     * Create the node containing all the Web App resources.
     *
     * @param parent The parent node of this node
     */
    public WebAppModule(Node parent) {
        super(REDIS_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
        webAppModulePresenter = new WebAppModulePresenter<>();
        webAppModulePresenter.onAttachView(WebAppModule.this);
        createListener();
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        webAppModulePresenter.onModuleRefresh();
    }

    @Override
    public void removeNode(String sid, String id, Node node) {
        try {
            webAppModulePresenter.onDeleteWebApp(sid, id);
            removeDirectChildNode(node);
        } catch (IOException e) {
            DefaultLoader.getUIHelper().showException("An error occurred while attempting to delete the Web App ",
                    e, "Azure Services Explorer - Error Deleting Web App for Containers", false, true);
        }
    }

    private void createListener() {
        String id = "WebAppModule";
        AzureUIRefreshListener listener = new AzureUIRefreshListener() {
            @Override
            public void run() {
                if (event.opsType == AzureUIRefreshEvent.EventType.SIGNIN || event.opsType == AzureUIRefreshEvent
                        .EventType.SIGNOUT) {
                    removeAllChildNodes();
                } else if (event.object == null && (event.opsType == AzureUIRefreshEvent.EventType.UPDATE || event
                        .opsType == AzureUIRefreshEvent.EventType.REMOVE)) {
                    if (hasChildNodes()) {
                        load(true);
                    }
                } else if (event.object == null && event.opsType == AzureUIRefreshEvent.EventType.REFRESH) {
                    load(true);
                } else if (event.object != null && event.object.getClass().toString().equals(WebAppUtils
                        .WebAppDetails.class.toString())) {
                    WebAppUtils.WebAppDetails webAppDetails = (WebAppUtils.WebAppDetails) event.object;
                    switch (event.opsType) {
                        case ADD:
                            DefaultLoader.getIdeHelper().invokeLater(() -> {
                                try {
                                    addChildNode(new WebAppNode(WebAppModule.this,
                                            ResourceId.fromString(webAppDetails.webApp.id()).subscriptionId(),
                                            webAppDetails.webApp.id(),
                                            webAppDetails.webApp.name(),
                                            webAppDetails.webApp.state(),
                                            webAppDetails.webApp.defaultHostName(),
                                            null));
                                } catch (Exception ex) {
                                    DefaultLoader.getUIHelper().logError("WebAppModule::createListener ADD", ex);
                                    ex.printStackTrace();
                                }
                            });
                            break;
                        case UPDATE:
                            break;
                        case REMOVE:
                            break;
                        default:
                            break;
                    }
                }
            }
        };
        AzureUIRefreshCore.addListener(id, listener);
    }

    @Override
    public void renderChildren(@NotNull final List<ResourceEx<WebApp>> resourceExes) {
        for (final ResourceEx<WebApp> resourceEx : resourceExes) {
            final WebApp app = resourceEx.getResource();
            final WebAppNode node = new WebAppNode(this,
                resourceEx.getSubscriptionId(), app.id(), app.name(), app.state(), app.defaultHostName(),
                new HashMap<String, String>() {
                    {
                        put("regionName", app.regionName());
                    }
                });

            addChildNode(node);
            node.refreshItems();
        }
    }
}
