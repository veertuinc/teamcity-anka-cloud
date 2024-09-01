package com.veertu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.diagnostic.Logger;
import com.veertu.ankaMgmtSdk.AuthType;
import com.veertu.common.AnkaConstants;
import com.veertu.utils.AnkaCloudPropertiesProcesser;

import jetbrains.buildServer.clouds.CloudClientEx;
import jetbrains.buildServer.clouds.CloudClientFactory;
import jetbrains.buildServer.clouds.CloudClientParameters;
import jetbrains.buildServer.clouds.CloudRegistrar;
import jetbrains.buildServer.clouds.CloudState;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.AgentDescription;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.ServerSettings;
import jetbrains.buildServer.web.openapi.PluginDescriptor;


/**
 * Created by Asaf Gur.
 */

public class AnkaCloudClientFactory implements CloudClientFactory {

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);


    @NotNull
    private final CloudRegistrar cloudRegistrar;
    @NotNull private final String cloudProfileSettings;
    @NotNull
    private final ServerSettings serverSettings;

    @NotNull
    private final InstanceUpdater updater;


    public AnkaCloudClientFactory(@NotNull final CloudRegistrar cloudRegistrar,
                                  @NotNull final PluginDescriptor pluginDescriptor,
                                  @NotNull final ServerSettings serverSettings,
                                  @NotNull final InstanceUpdater updater) {
        this.cloudRegistrar = cloudRegistrar;
        cloudProfileSettings = pluginDescriptor.getPluginResourcesPath(AnkaConstants.PROFILE_SETTING_HTML);
        this.serverSettings = serverSettings;
        this.updater = updater;
        LOG.info("Registering Anka Cloud plugin in cloud registrar");
        cloudRegistrar.registerCloudFactory(this);
    }

    @NotNull
    @Override
    public CloudClientEx createNewClient(@NotNull CloudState cloudState, @NotNull CloudClientParameters cloudClientParameters) {
        String mgmtURL = cloudClientParameters.getParameter(AnkaConstants.CONTROLLER_URL_NAME);
        String sshUser = cloudClientParameters.getParameter(AnkaConstants.SSH_USER);
        String sshPassword = cloudClientParameters.getParameter(AnkaConstants.SSH_PASSWORD);
        String imageId = cloudClientParameters.getParameter(AnkaConstants.IMAGE_ID);
        String templateName = cloudClientParameters.getParameter(AnkaConstants.TEMPLATE_NAME);
        String templateTag = cloudClientParameters.getParameter(AnkaConstants.TEMPLATE_TAG);
        String agentPath = cloudClientParameters.getParameter(AnkaConstants.AGENT_PATH);
        String serverUrl = cloudClientParameters.getParameter(AnkaConstants.OPTIONAL_SERVER_URL);
        String groupId = cloudClientParameters.getParameter(AnkaConstants.GROUP_ID);
        String vmNameTemplate = cloudClientParameters.getParameter(AnkaConstants.VM_NAME_TEMPLATE);
        if (groupId != null && groupId.isEmpty()) {
            groupId = null;
        }
        Integer agentPoolId = null;
        String agentPoolIdVal = cloudClientParameters.getParameter(AnkaConstants.AGENT_POOL_ID);
        String priorityVal = cloudClientParameters.getParameter(AnkaConstants.PRIORITY);
        int priority = 0;
        try {
            if (!priorityVal.isEmpty()) {
                priority = Integer.parseInt(priorityVal);
            }
        } catch (NullPointerException | NumberFormatException e) {
            // so nothing priority will be 0
        }
        try {
            if (!agentPoolIdVal.isEmpty()) {
                agentPoolId = Integer.valueOf(agentPoolIdVal);
            }
        } catch (NullPointerException | NumberFormatException e) {
            // do nothing - agentPoolId will just be null...
        }
        int sshForwardingPort = 22;
        String sshForwardingPortString = cloudClientParameters.getParameter(AnkaConstants.SSH_FORWARDING_PORT);
        if (sshForwardingPortString != null && !sshForwardingPortString.isEmpty()) {
            try {
                sshForwardingPort = Integer.parseInt(sshForwardingPortString);
            } catch (NumberFormatException e) {
                LOG.error(String.format("Invalid SSH forwarded port: %s", sshForwardingPortString));
            }
        }

        Integer maxInstances = Integer.MAX_VALUE;
        try {
            String maxInstancesString = cloudClientParameters.getParameter(AnkaConstants.MAX_INSTANCES);
            maxInstances = Integer.valueOf(maxInstancesString);
        } catch (NullPointerException | NumberFormatException e) {
            // do nothing - maxInstances will just be MAX (unlimited)...
        }
        String skipTLSVerificationString = cloudClientParameters.getParameter(AnkaConstants.SKIP_TLS_VERIFICATION);
        boolean skipTLSVerification = false;
        if (skipTLSVerificationString != null && skipTLSVerificationString.equals("true")) {
            skipTLSVerification = true;
        }
        String rootCA = null;
        String rootCAParam =  cloudClientParameters.getParameter(AnkaConstants.ROOT_CA);
        if (rootCAParam != null && !rootCAParam.isEmpty()) {
            rootCA = rootCAParam;
        }

        String profileId = cloudClientParameters.getParameter("system.cloud.profile_id");

        AnkaCloudConnector connector;

        String authMethod = cloudClientParameters.getParameter(AnkaConstants.AUTH_METHOD);

        if (authMethod != null && authMethod.equals(AnkaConstants.AUTH_METHOD_CERT)) {
            String cert = cloudClientParameters.getParameter(AnkaConstants.CERT_STRING);
            String key = cloudClientParameters.getParameter(AnkaConstants.CERT_KEY_STRING);
            if (cert != null && !cert.isEmpty() && key != null && !key.isEmpty()) {
                connector = new AnkaCloudConnector(mgmtURL, skipTLSVerification, 
                    sshUser,sshPassword, sshForwardingPort, 
                    agentPath, serverUrl, agentPoolId, profileId, priority,
                        cert, key, AuthType.CERTIFICATE, rootCA);
            } else {
                connector = new AnkaCloudConnector(mgmtURL, 
                        sshUser, sshPassword, sshForwardingPort, 
                        agentPath, serverUrl, agentPoolId, profileId, priority, rootCA);
            }


        } else if (authMethod != null && authMethod.equals(AnkaConstants.AUTH_METHID_OIDC)) {
            String client = cloudClientParameters.getParameter(AnkaConstants.OIDC_CLIENT_ID);
            String secret = cloudClientParameters.getParameter(AnkaConstants.OIDC_CLIENT_SECRET);
            connector = new AnkaCloudConnector(mgmtURL, skipTLSVerification,
                    sshUser, sshPassword, sshForwardingPort, 
                    agentPath, serverUrl, agentPoolId, profileId, priority,
                    client, secret, AuthType.OPENID_CONNECT, rootCA);
        } else {
            connector = new AnkaCloudConnector(mgmtURL, 
                sshUser, sshPassword, sshForwardingPort, 
                agentPath, serverUrl, agentPoolId, profileId, priority, rootCA);
        }

        AnkaCloudImage newImage = new AnkaCloudImage(connector, imageId, templateName, templateTag, groupId, vmNameTemplate);
        ArrayList<AnkaCloudImage> images = new ArrayList<>();
        images.add(newImage);
        if (templateTag != null && templateTag.length() > 0) {
            LOG.info(String.format("Creating AnkaCloudClientEx for server %s , image %s(%s) tag %s",
                    mgmtURL, templateName, imageId, templateTag));
        } else {
            LOG.info(String.format("Creating AnkaCloudClientEx for server %s , image %s(%s), and latest tag",
                    mgmtURL, templateName, imageId));
        }

        return new AnkaCloudClientEx(connector, updater, images, maxInstances);

    }

    @NotNull
    @Override
    public String getCloudCode() {
        return AnkaConstants.CLOUD_CODE;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return AnkaConstants.CLOUD_DISPLAY_NAME;
    }

    @Nullable
    @Override
    public String getEditProfileUrl() {
        return cloudProfileSettings;
    }

    @NotNull
    @Override
    public Map<String, String> getInitialParameterValues() {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(AnkaConstants.CONTROLLER_URL_NAME, "");
        parameters.put(AnkaConstants.SSH_PASSWORD, "admin");
        parameters.put(AnkaConstants.SSH_USER, "anka");
        parameters.put(AnkaConstants.SSH_FORWARDING_PORT, "22");
        parameters.put(AnkaConstants.AGENT_PATH, "/Users/anka/buildAgent");
        parameters.put(AnkaConstants.VM_NAME_TEMPLATE, "$ts");
        return parameters;
    }

    @NotNull
    @Override
    public PropertiesProcessor getPropertiesProcessor() {
        return new AnkaCloudPropertiesProcesser();
    }

    @Override
    public boolean canBeAgentOfType(@NotNull AgentDescription agentDescription) {
        Map<String, String> availableParameters = agentDescription.getAvailableParameters();
        String ankaCloudKey = availableParameters.get(AnkaConstants.ENV_ANKA_CLOUD_KEY);
        return (ankaCloudKey != null && ankaCloudKey.equals(AnkaConstants.ENV_ANKA_CLOUD_VALUE));
    }
}
