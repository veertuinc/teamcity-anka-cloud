package com.veertu;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.diagnostic.Logger;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ServerExtension;
import jetbrains.buildServer.serverSide.agentTypes.AgentTypeKey;

public class AgentParametersProvider implements ServerExtension {

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);
    
    public AgentParametersProvider() {
        LOG.info("AgentParametersProvider initialized");
    }

    public boolean disableUpdateFromAgent(@NotNull AgentTypeKey agentTypeKey) {
        LOG.info("disableUpdateFromAgent called with agentTypeKey: " + agentTypeKey);
        return false;
    }

}