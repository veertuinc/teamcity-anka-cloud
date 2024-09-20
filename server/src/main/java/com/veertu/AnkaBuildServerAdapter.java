package com.veertu;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.diagnostic.Logger;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuildAgent;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SRunningBuild;

//////////////////
// these run when the build gets an agent and starts running
// not really used, but useful to know it exists.
/////////////////

public class AnkaBuildServerAdapter extends BuildServerAdapter {

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

	private final SBuildServer server;

	public AnkaBuildServerAdapter(@NotNull SBuildServer sBuildServer) {
		server = sBuildServer;
	}

	public void register() {
		server.addListener(this);
	}

    @Override
    public void agentRegistered(SBuildAgent agent, long currentlyRunningBuildId) {
        // LOG.info("Agent registered =========: " + agent.toString());
        super.agentRegistered(agent, currentlyRunningBuildId);
    }

    @Override
    public void agentUnregistered(SBuildAgent agent) {
        // LOG.info("Agent unregistered =========: " + agent.toString());
        super.agentUnregistered(agent);
    }
    
	@Override
	public void changesLoaded(SRunningBuild build) {
		// if (build.getBuildType() != null) {
		// 	LOG.info(String.format("BUILD STARTED ===========================> %s", build.getBuildType().getName()));
		// } else {
		// 	LOG.info("BUILD STARTED ===========================> unknown build type");
		// }
		super.changesLoaded(build);
	}
	
	@Override
	public void buildFinished(SRunningBuild build) {
        // if (build.getBuildType() != null) {
		// 	LOG.info(String.format("BUILD FINISHED ===========================> %s", build.getBuildType().getName()));
		// } else {
		// 	LOG.info("BUILD FINISHED ===========================> unknown build type");
		// }
		super.buildFinished(build);
	}
	
	@Override
	public void buildInterrupted(SRunningBuild build) {
        // LOG.info("BUILD INTERRUPTED ===========================> " + build.toString());
        // if (build.getBuildType() != null) {
		// 	LOG.info(String.format("BUILD INTERRUPTED ===========================> %s", build.getBuildType().getName()));
		// } else {
		// 	LOG.info("BUILD INTERRUPTED ===========================> unknown build type");
		// }
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

    // @Override
    // public void agentStatusChanged(SBuildAgent agent, boolean wasEnabled, boolean wasAuthorized) {
    //     LOG.info(" ============= Agent status changed: " + agent.toString() + " wasEnabled: " + wasEnabled + " wasAuthorized: " + wasAuthorized);
    //     super.agentStatusChanged(agent, wasEnabled, wasAuthorized);
    // }

}