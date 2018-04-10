package com.veertu;

import com.veertu.utils.AnkaConstants;
import com.veertu.utils.AnkaCloudPropertiesProcesser;
import jetbrains.buildServer.clouds.*;
import jetbrains.buildServer.serverSide.AgentDescription;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class AnkaCloudClientFactory implements CloudClientFactory {


    @NotNull private final String cloudProfileSettings;
    @NotNull private final ServerPaths serverPaths;



    public AnkaCloudClientFactory(@NotNull final CloudRegistrar cloudRegistrar,
                                  @NotNull final PluginDescriptor pluginDescriptor,
                                  @NotNull final ServerPaths serverPaths) {
        cloudProfileSettings = pluginDescriptor.getPluginResourcesPath("profile-settings.jsp");
        this.serverPaths = serverPaths;
        cloudRegistrar.registerCloudFactory(this);
    }

    @NotNull
    @Override
    public CloudClientEx createNewClient(@NotNull CloudState cloudState, @NotNull CloudClientParameters cloudClientParameters) {
        String host = cloudClientParameters.getParameter(AnkaConstants.HOST_NAME);
        String port = cloudClientParameters.getParameter(AnkaConstants.PORT);
        return new AnkaCloudClientEx(host, port);

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
        parameters.put(AnkaConstants.HOST_NAME, "18.236.6.136");
        parameters.put(AnkaConstants.PORT, "8090");
        return parameters;
    }

    @NotNull
    @Override
    public PropertiesProcessor getPropertiesProcessor() {
        return new AnkaCloudPropertiesProcesser();
    }

    @Override
    public boolean canBeAgentOfType(@NotNull AgentDescription agentDescription) {
        final Map<String, String> configParams = agentDescription.getConfigurationParameters();
        return configParams.containsKey(AnkaConstants.IMAGE_NAME);
    }
}
