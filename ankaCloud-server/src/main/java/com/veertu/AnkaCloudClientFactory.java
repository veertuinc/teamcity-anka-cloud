package com.veertu;

import com.veertu.ankaMgmtSdk.AuthType;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.common.AnkaConstants;
import com.veertu.utils.AnkaCloudPropertiesProcesser;
import jetbrains.buildServer.clouds.*;
import jetbrains.buildServer.serverSide.AgentDescription;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.ServerSettings;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


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
        String imageName = cloudClientParameters.getParameter(AnkaConstants.IMAGE_NAME);
        String imageTag = cloudClientParameters.getParameter(AnkaConstants.IMAGE_TAG);
        String agentPath = cloudClientParameters.getParameter(AnkaConstants.AGENT_PATH);
        String serverUrl = cloudClientParameters.getParameter(AnkaConstants.OPTIONAL_SERVER_URL);
        String groupId = cloudClientParameters.getParameter(AnkaConstants.GROUP_ID);
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
        Integer maxInstances = Integer.MAX_VALUE;
        try {
            String maxInstancesString = cloudClientParameters.getParameter(AnkaConstants.MAX_INSTANCES);
            maxInstances = Integer.valueOf(maxInstancesString);
        } catch (NullPointerException | NumberFormatException e) {
            // do nothing - maxInstances will just be MAX (unlimited)...
        }

        String profileId = cloudClientParameters.getParameter("system.cloud.profile_id");

        AnkaCloudConnector connector;

        String authMethod = cloudClientParameters.getParameter(AnkaConstants.AUTH_METHOD);
        if (authMethod != null && authMethod.equals(AnkaConstants.AUTH_METHOD_CERT)) {
            String cert = cloudClientParameters.getParameter(AnkaConstants.CERT_STRING);
            String key = cloudClientParameters.getParameter(AnkaConstants.CERT_KEY_STRING);
            connector = new AnkaCloudConnector(mgmtURL, sshUser,
                    sshPassword, agentPath, serverUrl, agentPoolId, profileId, priority,
                    cert, key, AuthType.CERTIFICATE);
        } else if (authMethod != null && authMethod.equals(AnkaConstants.AUTH_METHID_OIDC)) {
            String client = cloudClientParameters.getParameter(AnkaConstants.OIDC_CLIENT_ID);
            String secret = cloudClientParameters.getParameter(AnkaConstants.OIDC_CLIENT_SECRET);
            connector = new AnkaCloudConnector(mgmtURL, sshUser,
                    sshPassword, agentPath, serverUrl, agentPoolId, profileId, priority,
                    client, secret, AuthType.OPENID_CONNECT);
        } else {
            connector = new AnkaCloudConnector(mgmtURL, sshUser,
                    sshPassword, agentPath, serverUrl, agentPoolId, profileId, priority);
        }

        AnkaCloudImage newImage = new AnkaCloudImage(connector, imageId, imageName, imageTag, groupId);
        ArrayList<AnkaCloudImage> images = new ArrayList<>();
        images.add(newImage);
        LOG.info(String.format("Creating AnkaCloudClientEx for server %s , image %s(%s) tag %s",
                mgmtURL, imageName, imageId, imageTag ));

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
        parameters.put(AnkaConstants.AGENT_PATH, "/Users/anka/buildAgent");
        return parameters;
    }

    @NotNull
    @Override
    public PropertiesProcessor getPropertiesProcessor() {
        return new AnkaCloudPropertiesProcesser();
    }

    @Override
    public boolean canBeAgentOfType(@NotNull AgentDescription agentDescription) {
        LOG.info(String.format("Checking if '%s' can be an Anka Agent", agentDescription.toString()));
        Map<String, String> availableParameters = agentDescription.getAvailableParameters();
        String ankaCloudKey = availableParameters.get(AnkaConstants.ENV_ANKA_CLOUD_KEY);
        LOG.info(String.format("Anka Cloud key is : %s, Const value: %s",
                ankaCloudKey == null ? "null" : ankaCloudKey,
                AnkaConstants.ENV_ANKA_CLOUD_VALUE ));
        return (ankaCloudKey != null && ankaCloudKey.equals(AnkaConstants.ENV_ANKA_CLOUD_VALUE));
    }
}
