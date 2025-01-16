// This logic runs on the agent. LOGs are sent to the buildAgent/logs/*
package com.veertu;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.diagnostic.Logger;

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildAgent;
import jetbrains.buildServer.agent.BuildAgentConfigurationEx;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.EventDispatcher;

public class AnkaCloudPropertiesSetter extends AgentLifeCycleAdapter {

    private static final Logger LOG = Loggers.AGENT;

    private final BuildAgentConfigurationEx agentConfiguration;
    @NotNull
    private final EventDispatcher<AgentLifeCycleListener> eventsListener;

    public AnkaCloudPropertiesSetter(final BuildAgentConfigurationEx agentConfiguration,
                                     @NotNull EventDispatcher<AgentLifeCycleListener> eventsListener) {
        LOG.info("Created ContainerCloudAgentPropertiesSetter");
        this.agentConfiguration = agentConfiguration;
        this.eventsListener = eventsListener;

        eventsListener.addListener(this);
    }

    String execCmd(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String v = r.readLine();
            LOG.info(String.format("Executing: '%s', res: '%s'", cmd, v));
            return v;
        } catch (Exception e) {
        }
        return null;
    }

    // @Override
    // public void agentStarted(@NotNull BuildAgent agent) {
    //     LOG.info("agentStarted");
    // }

    @Override
    public void afterAgentConfigurationLoaded(@NotNull BuildAgent agent) {
        final Map<String, String> env = agentConfiguration.getBuildParameters().getEnvironmentVariables();
        LOG.info("ankaAgent: config");
        /*String val = execCmd("launchctl getenv " + AnkaConstants.ENV_ANKA_CLOUD_KEY);
        if (val != null && val.length() > 0) {
            agentConfiguration.addConfigurationParameter(AnkaConstants.ENV_ANKA_CLOUD_KEY, val);
            LOG.info("ankaAgent: Detected cloud VM");
        }
        val = execCmd("launchctl getenv " + AnkaConstants.ENV_AGENT_NAME_KEY);
        if (val != null && val.length() > 0) {
            agentConfiguration.setName(val);
            LOG.info(String.format("ankaAgent: name %s", val));
        }
        val = execCmd("launchctl getenv " + AnkaConstants.ENV_SERVER_URL_KEY);
        if (val != null && val.length() > 0) {
            agentConfiguration.setServerUrl(val);
            LOG.info(String.format("ankaAgent: serverUrl %s", val));
        }*/
    }
}