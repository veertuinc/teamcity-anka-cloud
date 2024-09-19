package com.veertu;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.diagnostic.Logger;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildAgent;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SRunningBuild;

public class AnkaBuildServerAdapter extends BuildServerAdapter {

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

	private final SBuildServer server;
    private AnkaCloudClientEx client;
    // private BuildAgentManagerEx buildAgentManager;

	public AnkaBuildServerAdapter(@NotNull SBuildServer sBuildServer) {
		server = sBuildServer;
	}

    public SBuildAgent getAgentByBuildId(long buildId) {
        SBuild build = server.findBuildInstanceById(buildId);
        if (build != null) {
            return build.getAgent();
        } else {
            LOG.warn("Build not found for ID: " + buildId);
            return null;
        }
    }

	public void register() {
		server.addListener(this);
		LOG.info("AnkaBuildServerAdapter.register()");
	}

    @Override
    public void agentRegistered(SBuildAgent agent, long currentlyRunningBuildId) {
        LOG.info("Agent registered =========: " + agent.toString());
        super.agentRegistered(agent, currentlyRunningBuildId);
    }

    @Override
    public void agentUnregistered(SBuildAgent agent) {
        LOG.info("Agent unregistered =========: " + agent.toString());
        super.agentUnregistered(agent);
    }
    
	@Override
	public void changesLoaded(SRunningBuild build) {
		if (build.getBuildType() != null) {
			LOG.info(String.format("BUILD STARTED ===========================> %s", build.getBuildType().getName()));
		} else {
			LOG.info("BUILD STARTED ===========================> unknown build type");
		}
        client.unregisterAgent(build.getAgent().getId());
        // buildAgentManager.unregisterAgent(build.getAgent().getId(), "Cloud instance has gone");
        // SBuildAgent agent = getAgentByBuildId(build.getBuildId());
        // CloudInstance instance = this.client.findInstanceByAgent(agent);
        // if (instance != null) {
        //     this.client.terminateInstance(instance);
        //     LOG.info("BUILD STARTED ===========================> Instance terminated: " + instance.getInstanceId());
        // } else {
        //     LOG.warn("BUILD STARTED ===========================> Instance not found to terminate for agent: " + agent.toString());
        // }
		super.changesLoaded(build);
	}
	
	@Override
	public void buildFinished(SRunningBuild build) {
        if (build.getBuildType() != null) {
			LOG.info(String.format("BUILD FINISHED ===========================> %s", build.getBuildType().getName()));
		} else {
			LOG.info("BUILD FINISHED ===========================> unknown build type");
		}
		super.buildFinished(build);
	}
	
	@Override
	public void buildInterrupted(SRunningBuild build) {
        LOG.info("BUILD INTERRUPTED ===========================> " + build.toString());
        if (build.getBuildType() != null) {
			LOG.info(String.format("BUILD INTERRUPTED ===========================> %s", build.getBuildType().getName()));
		} else {
			LOG.info("BUILD INTERRUPTED ===========================> unknown build type");
		}
        // SBuildAgent agent = getAgentByBuildId(build.getBuildId());
        // CloudInstance instance = this.client.findInstanceByAgent(agent);
        // if (instance != null) {
        //     this.client.terminateInstance(instance);
        //     LOG.info("BUILD INTERRUPTED ===========================> Instance terminated: " + instance.getInstanceId());
        // } else {
        //     LOG.warn("BUILD INTERRUPTED ===========================> Instance not found to terminate for agent: " + agent.toString());
        // }
		super.buildInterrupted(build);
	}
	
	@Override
	public void serverStartup() {
		LOG.info("Server startup");
	}

	@Override
	public void serverShutdown() {
		LOG.info("Server shutdown");
	}
}