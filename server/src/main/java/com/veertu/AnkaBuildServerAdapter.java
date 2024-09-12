package com.veertu;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.diagnostic.Logger;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SRunningBuild;

public class AnkaBuildServerAdapter extends BuildServerAdapter {

    // public enum TeamCityEvent {
    //     BUILD_FAILED, BUILD_INTERRUPTED, BUILD_STARTED, BUILD_SUCCESSFUL, SERVER_STARTUP, SERVER_SHUTDOWN
    // }

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

	private final SBuildServer server;
    // private final HashMap<TeamCityEvent, String> eventMap;


	public AnkaBuildServerAdapter(@NotNull SBuildServer sBuildServer) {
		server = sBuildServer;
		// this.eventMap = new HashMap<>();
		// this.eventMap.put(TeamCityEvent.BUILD_STARTED, "INFO");
		// this.eventMap.put(TeamCityEvent.BUILD_SUCCESSFUL, "SUCCESS");
		// this.eventMap.put(TeamCityEvent.BUILD_FAILED, "ERROR");
		// this.eventMap.put(TeamCityEvent.BUILD_INTERRUPTED, "WARNING");
		// this.eventMap.put(TeamCityEvent.SERVER_STARTUP, "WARNING");
		// this.eventMap.put(TeamCityEvent.SERVER_SHUTDOWN, "WARNING");
		LOG.debug("HERE ==========================1");
	}

	public void register() {
		LOG.debug("HERE ===========================2");
		server.addListener(this);
		LOG.debug("HERE ===========================3");
	}
	
	@Override
	public void changesLoaded(SRunningBuild build) {
		LOG.debug("HERE ===========================4");
		super.changesLoaded(build);
	}
	
	@Override
	public void buildFinished(SRunningBuild build) {
		LOG.debug("HERE ===========================5");
		super.buildFinished(build);
	}
	
	@Override
	public void buildInterrupted(SRunningBuild build) {
		LOG.debug("HERE ===========================6");
		super.buildInterrupted(build);
	}
	
	@Override
	public void serverStartup() {
		LOG.debug("Server startup");
	}

	@Override
	public void serverShutdown() {
		LOG.debug("Server shutdown");
	}
}