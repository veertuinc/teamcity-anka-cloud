package com.veertu;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.HashMap;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildAgent;
import jetbrains.buildServer.agent.BuildAgentConfigurationEx;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import com.veertu.common.AnkaConstants;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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

    @Override
    public void afterAgentConfigurationLoaded(@NotNull BuildAgent agent) {
        //final Map<String, String> env = agentConfiguration.getBuildParameters().getEnvironmentVariables();
        final Map env = new HashMap<String, String>();

        LOG.info("ankaAgent: config");

        try {
            File file = new File("/tmp/tc-anka.txt");
            FileReader fr = new FileReader(file);
            BufferedReader bf = new BufferedReader(fr);
            String line;
            while ((line = bf.readLine()) != null) {
                String[] p = line.split("=");
                env.put(p[0], p[1]);
                LOG.info(String.format("ankaAgent: key: %s, val: %s", p[0], p[1]));
            }
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String instance_id = (String)env.get(AnkaConstants.INSTANCE_ID);
        if (instance_id == null) {
            LOG.error("Cannot find instance id");
            return;
        }
        agentConfiguration.addConfigurationParameter(AnkaConstants.INSTANCE_ID, instance_id);

        String image_id = (String)env.get(AnkaConstants.IMAGE_ID);
        if (image_id == null) {
            LOG.error("Cannot find image_id");
            return;
        }
        agentConfiguration.addConfigurationParameter(AnkaConstants.IMAGE_ID, image_id);

        String image_name = (String)env.get(AnkaConstants.IMAGE_NAME);
        if (image_id == null) {
            LOG.error("Cannot find image_name");
            return;
        }
        agentConfiguration.addConfigurationParameter(AnkaConstants.IMAGE_NAME, image_name);

        String instance_name = (String)env.get(AnkaConstants.INSTANCE_NAME);
        if (instance_name == null) {
            LOG.error("Cannot find instance_name");
            return;
        }
        agentConfiguration.addConfigurationParameter(AnkaConstants.INSTANCE_NAME, instance_name);
        agentConfiguration.setName(instance_name);


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
