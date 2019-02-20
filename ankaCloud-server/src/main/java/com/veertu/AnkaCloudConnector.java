package com.veertu;

import com.intellij.openapi.diagnostic.Logger;
import com.veertu.ankaMgmtSdk.AuthType;
import jetbrains.buildServer.log.Loggers;
import com.veertu.ankaMgmtSdk.AnkaAPI;
import com.veertu.ankaMgmtSdk.AnkaCloudStatus;
import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.common.AnkaConstants;
import jetbrains.buildServer.clouds.CloudInstance;

import java.io.IOException;
import java.util.*;


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
    private final int priority;
    private final AnkaAPI ankaAPI;

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

    public AnkaCloudConnector(String mgmtURL, String sshUser,
                              String sshPassword, String agentPath, String serverUrl,
                              Integer agentPoolId, String profileId, int priority) {
        this.mgmtURL = mgmtURL;
        this.sshUser = sshUser;
        this.sshPassword = sshPassword;
        this.agentPath = agentPath;
        this.serverUrl = serverUrl;
        this.agentPoolId = agentPoolId;
        this.profileId = profileId;
        this.priority = priority;
        this.ankaAPI = new AnkaAPI(mgmtURL);
    }

    public AnkaCloudConnector(String mgmtURL, String sshUser, String sshPassword, String agentPath,
                              String serverUrl, Integer agentPoolId, String profileId, int priority,
                              String cert, String key, AuthType authType) {
        this.mgmtURL = mgmtURL;
        this.sshUser = sshUser;
        this.sshPassword = sshPassword;
        this.agentPath = agentPath;
        this.serverUrl = serverUrl;
        this.agentPoolId = agentPoolId;
        this.profileId = profileId;
        this.priority = priority;
        this.ankaAPI = new AnkaAPI(mgmtURL, cert, key, authType);
    }

    public AnkaCloudInstance startNewInstance(AnkaCloudImage cloudImage, InstanceUpdater updater) throws AnkaMgmtException {
        AnkaMgmtVm vm = this.ankaAPI.makeAnkaVm(cloudImage.getId(), cloudImage.getTag(), null, 22, priority, cloudImage.getGroupId());
        updater.executeTaskInBackground(() -> this.waitForBootAndSetVmProperties(vm, cloudImage));
        return new AnkaCloudInstance(vm, cloudImage);
    }

    private void waitForBootAndSetVmProperties(AnkaMgmtVm vm, AnkaCloudImage cloudImage) {
        try {
            HashMap<String, String > properties = new HashMap<>();

            vm.waitForBoot();

            String vmName = vm.getName();
            if (vmName == null)
                vmName = String.format("%s_%s", cloudImage.getId(), vm.getId());

            LOG.info(String.format("VM %s (%s) has booted, starting SSH session...", vmName, vm.getId()));

            if (this.serverUrl != null && this.serverUrl.length() > 0) {
                properties.put(AnkaConstants.ENV_SERVER_URL_KEY, this.serverUrl);
            }
            properties.put(AnkaConstants.ENV_AGENT_NAME_KEY, vmName);
            properties.put(AnkaConstants.ENV_INSTANCE_ID_KEY, vm.getId());
            properties.put(AnkaConstants.ENV_IMAGE_ID_KEY, cloudImage.getId());
            properties.put(AnkaConstants.ENV_PROFILE_ID, profileId);
            properties.put(AnkaConstants.ENV_ANKA_CLOUD_KEY, AnkaConstants.ENV_ANKA_CLOUD_VALUE);

            AnkaSSHPropertiesSetter propertiesSetter = new AnkaSSHPropertiesSetter(vm, sshUser, sshPassword, agentPath);
            try {
                propertiesSetter.setProperties(properties);
            } catch (AnkaUnreachableInstanceException e) {
                LOG.error(e.getMessage());
                vm.terminate();
            }
        } catch (AnkaMgmtException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void terminateInstance(CloudInstance cloudInstance) {

        AnkaCloudInstance instance = (AnkaCloudInstance)cloudInstance;
        AnkaMgmtVm vm = instance.getVm();
        String vmName = vm.getName();
        if (vmName == null)
            vmName = String.format("%s_%s", cloudInstance.getImageId(), vm.getId());

        LOG.info(String.format("terminating instance %s", vmName));

        vm.terminate();
        AnkaCloudImage image = (AnkaCloudImage) instance.getImage();
        image.populateInstances();
    }


    public Collection<AnkaCloudInstance> getImageInstances(AnkaCloudImage image) {
        List<AnkaCloudInstance> instances = new ArrayList<>();
        try {
            List<AnkaMgmtVm> ankaMgmtVms = this.ankaAPI.listVms();

            for (AnkaMgmtVm vm: ankaMgmtVms) {
                if (vm.getImageId().equals(image.getId())) {
                    AnkaCloudInstance instance = new AnkaCloudInstance(vm, image);
                    instances.add(instance);

                }
            }
            return instances;
        } catch (AnkaMgmtException e) {
            return instances;
        }

    }


    public boolean isRunning() {
        AnkaCloudStatus ankaCloudStatus = this.ankaAPI.status();
        if (ankaCloudStatus == null) {
            return false;
        }
        return ankaCloudStatus.getStatus().toLowerCase().equals("running");
    }


    public Integer getAgentPoolId() {
        return agentPoolId;
    }

}
