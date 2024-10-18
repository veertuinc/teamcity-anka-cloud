package com.veertu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.intellij.openapi.diagnostic.Logger;
import com.veertu.ankaMgmtSdk.AnkaAPI;
import com.veertu.ankaMgmtSdk.AnkaCloudStatus;
import com.veertu.ankaMgmtSdk.AnkaVmInstance;
import com.veertu.ankaMgmtSdk.AnkaVmTemplate;
import com.veertu.ankaMgmtSdk.AuthType;
import com.veertu.ankaMgmtSdk.NodeGroup;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.common.AnkaConstants;

import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.clouds.CloudInstanceUserData;
import jetbrains.buildServer.log.Loggers;

/**
 * Created by Asaf Gur.
 */

/// AnkaCloudConnector is a facade connecting TC cloud profile to anka cloud
/// Each profile should have one AnkaCloudConnector
///
public class AnkaCloudConnector {
    private final String mgmtURL;
    private final String agentPath;
    private final String serverUrl;
    private final Integer agentPoolId;
    private final String profileId;
    private String sshUser;
    private String sshPassword;
    private int sshForwardingPort;
    private final int priority;
    private final AnkaAPI ankaAPI;
    private final int waitUnit = 4000;
    private final int maxRunningTimeout = waitUnit * 30;
    private final int maxSchedulingTimeout = 1000 * 60 * 60; // 1 hour
    private final int maxIpTimeout = waitUnit * 30;

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

    // Only to be used by EditProfileController to retrieve info on cloud profile configuration
    public AnkaCloudConnector(
        String mgmtURL, 
        boolean skipTLSVerification, 
        AuthType authType,
        String clientCert, 
        String clientCertKey, 
        String oidcClientId, 
        String oidcClientSecret,
        String rootCA, 
        String serverUrl
    ) {
        this.mgmtURL = mgmtURL;
        this.profileId = "";
        this.agentPoolId = 0;
        this.priority = 0;
        this.agentPath = "";
        this.serverUrl = serverUrl;

        switch (authType) {
            case OPENID_CONNECT:
                this.ankaAPI = new AnkaAPI(mgmtURL, skipTLSVerification, oidcClientId, oidcClientSecret, authType, rootCA);
                break;
            case CERTIFICATE:
                this.ankaAPI = new AnkaAPI(mgmtURL, skipTLSVerification, clientCert, clientCertKey, authType, rootCA);
                break;
            default:
                this.ankaAPI = new AnkaAPI(mgmtURL,skipTLSVerification, rootCA);
        }
    }

    public AnkaCloudConnector(
        String mgmtURL, 
        String sshUser, 
        String sshPassword, 
        int sshForwardingPort, 
        String agentPath, 
        Integer agentPoolId, 
        String profileId, 
        int priority, 
        String rootCA,
        String serverUrl
    ) {
        this.mgmtURL = mgmtURL;
        this.sshUser = sshUser;
        this.sshPassword = sshPassword;
        this.sshForwardingPort = sshForwardingPort;
        this.agentPath = agentPath;
        this.serverUrl = serverUrl;
        this.agentPoolId = agentPoolId;
        this.profileId = profileId;
        this.priority = priority;
        this.ankaAPI = new AnkaAPI(mgmtURL,false, rootCA);
    }

    public AnkaCloudConnector(
        String mgmtURL, 
        boolean skipTLSVerification, 
        String sshUser, 
        String sshPassword, 
        int sshForwardingPort, 
        String agentPath,
        Integer agentPoolId, 
        String profileId, 
        int priority,
        String cert, 
        String key, 
        AuthType authType,
        String rootCA,
        String serverUrl
    ) {
        this.mgmtURL = mgmtURL;
        this.sshUser = sshUser;
        this.sshPassword = sshPassword;
        this.sshForwardingPort = sshForwardingPort;
        this.agentPath = agentPath;
        this.serverUrl = serverUrl;
        this.agentPoolId = agentPoolId;
        this.profileId = profileId;
        this.priority = priority;
        this.ankaAPI = new AnkaAPI(mgmtURL, skipTLSVerification, cert, key, authType, rootCA);
    }

    public AnkaCloudInstance startNewInstance(AnkaCloudImage cloudImage, InstanceUpdater updater, CloudInstanceUserData userData) throws AnkaMgmtException {
        if (cloudImage.getTag() == null) {
            LOG.info(String.format("starting new instance with template %s, latest tag, group %s, externalId %s,", cloudImage.getId(), cloudImage.getGroupId(), cloudImage.getExternalId()));
        } else {
            LOG.info(String.format("starting new instance with template %s, tag %s, group %s, externalId %s", cloudImage.getId(), cloudImage.getTag(), cloudImage.getGroupId(), cloudImage.getExternalId()));
        }
        String vmId = this.ankaAPI.startVM(
            cloudImage.getId(), 
            cloudImage.getTag(), 
            null, 
            cloudImage.getGroupId(),
            priority, 
            null,
            userData.getProfileId(),
            cloudImage.getvmNameTemplate(),
            cloudImage.getVCpuCount(),
            cloudImage.getRamSize()
        );
        updater.executeTaskInBackground(() -> this.waitForBootAndSetVmProperties(vmId, cloudImage, userData));
        return new AnkaCloudInstance(vmId, cloudImage);
    }

    private void waitForBootAndSetVmProperties(String vmId, AnkaCloudImage cloudImage, CloudInstanceUserData userData) {
        try {
            HashMap<String, String > properties = new HashMap<>();
            AnkaVmInstance vm = waitForBoot(vmId);

            String vmName = vm.getName();
            // if (vmName == null)
            //     vmName = String.format("%s_%s", cloudImage.getId(), vm.getId());

            LOG.info(String.format("VM %s (%s) has booted, starting SSH session...", vmName, vmId));

            properties.put(AnkaConstants.ENV_AGENT_NAME_KEY, vmName);
            properties.put(AnkaConstants.ENV_INSTANCE_ID_KEY, vmId);
            properties.put(AnkaConstants.ENV_TEMPLATE_ID_KEY, cloudImage.getId());
            properties.put(AnkaConstants.ENV_PROFILE_ID, profileId);
            properties.put(AnkaConstants.ENV_ANKA_CLOUD_KEY, AnkaConstants.ENV_ANKA_CLOUD_VALUE);
            if (this.serverUrl != null && this.serverUrl.length() > 0) {
                properties.put(AnkaConstants.ENV_SERVER_URL_KEY, this.serverUrl);
            }

            // new requirement that allows auto-authorization to happen
            properties.put("teamcity.agent.startingInstanceId", userData.getCustomAgentConfigurationParameters().get("teamcity.agent.startingInstanceId"));

            AnkaSSHPropertiesSetter propertiesSetter = new AnkaSSHPropertiesSetter(
                vm, 
                sshUser,
                sshPassword,
                agentPath,
                sshForwardingPort,
                this.serverUrl
            );
            try {
                propertiesSetter.setProperties(properties);
            } catch (AnkaUnreachableInstanceException e) {
                LOG.error(e.getMessage());
                ankaAPI.terminateInstance(vmId);
            }
        } catch (AnkaMgmtException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void terminateInstance(CloudInstance cloudInstance) {
        AnkaCloudInstance instance = (AnkaCloudInstance)cloudInstance;
        try {
            AnkaVmInstance vm = instance.getVm();
            String vmName = vm.getName();
            if (vmName == null)
                vmName = String.format("%s_%s", cloudInstance.getImageId(), vm.getId());

            LOG.info(String.format("terminating instance %s", vmName));
            ankaAPI.terminateInstance(vm.getId());
        } catch (AnkaMgmtException e) {
            LOG.error(String.format("failed terminating instance %s. Error: %s", instance.getInstanceId(), e));
        }
        AnkaCloudImage image = (AnkaCloudImage) instance.getImage();
        image.populateInstances();
    }

    public Collection<AnkaCloudInstance> getImageInstances(AnkaCloudImage image) {
        List<AnkaCloudInstance> instances = new ArrayList<>();
        List<AnkaVmInstance> ankaInstances = this.ankaAPI.showInstances();
        for (AnkaVmInstance vm: ankaInstances) {
            if (vm.getExternalId().equals(image.getExternalId())) {
                AnkaCloudInstance instance = new AnkaCloudInstance(vm.getId(), image);
                instances.add(instance);
            }
        }
        return instances;
    }

    public boolean isRunning() {
        try {
            AnkaCloudStatus ankaCloudStatus = this.ankaAPI.getStatus();
            if (ankaCloudStatus == null) {
                return false;
            }
            return ankaCloudStatus.getStatus().toLowerCase().equals("running");
        } catch (AnkaMgmtException e) {
            return false;
        }
    }


    public Integer getAgentPoolId() {
        return agentPoolId;
    }

    private AnkaVmInstance waitForBoot(String vmId) throws InterruptedException, IOException, AnkaMgmtException {
        LOG.info(String.format("waiting for vm %s to boot", vmId));
        int timeWaited = 0;
        int pullTimeWaited = 0;
        AnkaVmInstance vm;

        // Scheduling
        while (true) {
            vm = ankaAPI.showInstance(vmId);
            if (!vm.isScheduling())
                break;

            LOG.info(String.format("waiting for vm %s %d to start", vmId, timeWaited));
            Thread.sleep(waitUnit);
            timeWaited += waitUnit;
            if (timeWaited > maxSchedulingTimeout) {
                ankaAPI.terminateInstance(vmId);
                throw new IOException("VM too long in scheduling state");
            }
        }

        // Validate no unexpected state
        if (!vm.isStarted() && !vm.isScheduling() && !vm.isPulling()) {
            LOG.info(String.format("vm %s in unexpected state %s, message: %s", vmId, vm.getSessionState(), vm.getMessage()));
            ankaAPI.terminateInstance(vmId);
            throw new IOException("could not start vm");
        }

        // Pulling and VM Boot
        timeWaited = 0;
        pullTimeWaited = 0;
        while (true) {
            vm = ankaAPI.showInstance(vmId);
            if (vm.isStarted() && vm.getVmInfo() != null)
                break;

            Thread.sleep(waitUnit);
            if (vm.isPulling()) {
                pullTimeWaited += waitUnit;
                LOG.info(String.format("vm %s is pulling", vmId));
                if (pullTimeWaited > 21600000) { // wait for 6 hours to pull, then timeout
                    LOG.error(String.format("vm %s timed out trying to pull", vmId));
                    ankaAPI.terminateInstance(vmId);
                    throw new IOException("could not start vm");
                }
            } else {
                timeWaited += waitUnit;
                LOG.info(String.format("vm %s is booting", vmId));
                if (timeWaited > maxRunningTimeout) {
                    LOG.error(String.format("vm %s timed out trying to start", vmId));
                    ankaAPI.terminateInstance(vmId);
                    throw new IOException("could not start vm");
                }
            }
        }

        // VM Network
        String ip;
        timeWaited = 0;
        LOG.info(String.format("waiting for vm %s to get an ip ", vmId));
        while (true) { // wait to get machine ip
            vm = ankaAPI.showInstance(vmId);
            ip = vm.getVmInfo().getVmIp();
            if (ip != null && !ip.equals(""))
                break;

            Thread.sleep(waitUnit);
            timeWaited += waitUnit;
            LOG.info(String.format("waiting for vm %s %d to get ip ", vmId, timeWaited));
            if (timeWaited > maxIpTimeout) {
                ankaAPI.terminateInstance(vmId);
                throw new IOException("VM started but couldn't acquire ip");
            }
        }
        // now that we have a running vm with an ip
        return vm;
    }

    public AnkaVmInstance showInstance(String vmId) {
        return ankaAPI.showInstance(vmId);
    }

    public List<String> listTemplateTags(String imageId) throws AnkaMgmtException {
        return ankaAPI.listTemplateTags(imageId);
    }

    public List<AnkaVmTemplate> listTemplates() throws AnkaMgmtException {
        return ankaAPI.listTemplates();
    }

    public List<NodeGroup> getNodeGroups() throws AnkaMgmtException {
        return ankaAPI.getNodeGroups();
    }

    public boolean isEnterpriseLicense() throws AnkaMgmtException {
        return ankaAPI.isEnterpriseLicense();
    }
}
