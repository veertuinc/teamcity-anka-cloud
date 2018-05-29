package com.veertu.ankaMgmtSdk;


import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;
import com.veertu.AnkaCloudConnector;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

import java.io.IOException;
import java.util.Date;

/**
 * Created by asafgur on 17/05/2017.
 */
public class ConcAnkaMgmtVm implements AnkaMgmtVm {

    private final AnkaMgmtCommunicator communicator;
    private final String sessionId;
    private final int waitUnit = 4000;
    private final int maxRunningTimeout = waitUnit * 20;
    private final int maxIpTimeout = waitUnit * 20;
    private final int sshConnectionPort;
    private AnkaVmSession cachedVmSession;
    private final int cacheTime = 60 * 5 * 1000; // 5 minutes
    private int lastCached = 0;

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);


    public ConcAnkaMgmtVm(String sessionId, AnkaMgmtCommunicator communicator, int sshConnectionPort) {
        this.communicator = communicator;
        this.sessionId = sessionId;
        this.sshConnectionPort = sshConnectionPort;
        LOG.info(String.format("init VM %s", sessionId));
    }

    public ConcAnkaMgmtVm(AnkaMgmtCommunicator communicator, AnkaVmSession session) {
        this.sessionId = session.id;
        this.cachedVmSession = session;
        this.sshConnectionPort = 0;
        this.communicator = communicator;
    }

    private String getStatus() throws AnkaMgmtException {
        AnkaVmSession session = this.communicator.showVm(this.sessionId);
        if (session == null) {
            throw new AnkaMgmtException(new Exception("No session found"));
        }
        return session.getSessionState();
    }

    public String getState() {
        try {
            return this.getStatus();
        } catch (AnkaMgmtException e) {
            return "";
        }
    }

    private String getIp() throws AnkaMgmtException {
        AnkaVmSession session = this.communicator.showVm(this.sessionId);
        if ( session.getVmInfo() == null) {
            return null;
        }
        String ip = session.getVmInfo().getVmIp();
        if (ip != null && !ip.equals("")) {
            return ip;
        }
        return null;
    }

    private int unixTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    private boolean shouldInvalidate() {
        int timeNow = unixTime();
        if (timeNow - this.lastCached > this.cacheTime) {
            this.lastCached = timeNow;
            return true;
        }
        return false;
    }

    private AnkaVmSession getSessionInfoCache() {
        try {
            if (this.cachedVmSession == null || this.shouldInvalidate()) {
                AnkaVmSession session = this.communicator.showVm(this.sessionId);
                if (session != null) {
                    this.cachedVmSession = session;
                } else {
                    LOG.info("info for vm is null");
                }
            }
            return this.cachedVmSession;
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String waitForBoot() throws InterruptedException, IOException, AnkaMgmtException {
        LOG.info(String.format("waiting for vm %s to boot", this.sessionId));
        int timeWaited = 0;
        String status;

        while (getStatus().equals("Scheduling")) {
            Thread.sleep(waitUnit);
            LOG.info(String.format("waiting for vm %s %d to start", this.sessionId, timeWaited));
        }

        status = getStatus();
        if (!status.equals("Starting") && !status.equals("Started") && !status.equals("Scheduled") && !status.equals("Scheduling")) {
            LOG.info(String.format("vm %s in unexpected state %s, terminating", this.sessionId, status));
            this.terminate();
            throw new IOException("could not start vm");
        }

        while (!getStatus().equals("Started") || getSessionInfoCache() == null) {
            // wait for the vm to spin up TODO: put this in const
            Thread.sleep(waitUnit);
            timeWaited += waitUnit;
            LOG.info(String.format("waiting for vm %s %d to boot", this.sessionId, timeWaited));
            if (timeWaited > maxRunningTimeout) {
                this.terminate();
                throw new IOException("could not start vm");

            }
        }

        String ip;
        timeWaited = 0;
        LOG.info(String.format("waiting for vm %s to get an ip ", this.sessionId));
        while (true) { // wait to get machine ip

            ip = this.getIp();
            if (ip != null)
                break;

            Thread.sleep(waitUnit);
            timeWaited += waitUnit;
            LOG.info(String.format("waiting for vm %s %d to get ip ", this.sessionId, timeWaited));
            if (timeWaited > maxIpTimeout) {
                this.terminate();
                throw new IOException("VM started but couldn't acquire ip");
            }
        }
        // now that we have a running vm we should be able to create a launcher
        return ip;
    }

    public String getId() {
        return sessionId;
    }

    public String getName() {
        AnkaVmSession session = this.getSessionInfoCache();
        AnkaVmInfo vmInfo = session.getVmInfo();
        if (vmInfo != null){
            return vmInfo.getName();
        }
        return null;
    }

    public String getConnectionIp() {
        AnkaVmSession session = this.getSessionInfoCache();
        if (session == null || session.getVmInfo() == null)
            return null;

        return session.getVmInfo().getHostIp();
    }

    public int getConnectionPort() {
        AnkaVmSession session = this.getSessionInfoCache();

        for (PortForwardingRule rule: session.getVmInfo().getPortForwardingRules()) {
            if (rule.getGuestPort() == this.sshConnectionPort) {
                return rule.getHostPort();
            }
        }
        return 0;
    }

    public void terminate() {
        try {
            this.communicator.terminateVm(this.sessionId);
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning() {
        AnkaVmSession session = this.getSessionInfoCache();
        return session.getSessionState().equals("Started") && session.getVmInfo().getStatus().equals("running");
    }

    public String getInfo() {
        AnkaVmSession session = this.getSessionInfoCache();
        return String.format("host: %s, uuid: %s, machine ip: %s",
                session.getVmInfo().getHostIp(), session.getVmInfo().getUuid(), session.getVmInfo().getVmIp());
    }

    public Date getCreatedTime() throws AnkaMgmtException {
        AnkaVmSession session = this.getSessionInfoCache();
        if (session == null) {
            throw new AnkaMgmtException("could not get session");
        }
        return session.getCreated();
    }

    public String getImageId() {
        AnkaVmSession session = this.getSessionInfoCache();
        return session.getVmId();
    }
}
