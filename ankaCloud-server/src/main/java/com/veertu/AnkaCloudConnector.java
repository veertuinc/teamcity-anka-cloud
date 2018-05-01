package com.veertu;

import com.veertu.ankaMgmtSdk.AnkaAPI;
import com.veertu.ankaMgmtSdk.AnkaCloudStatus;
import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.AnkaVmTemplate;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.utils.AnkaConstants;
import jetbrains.buildServer.clouds.CloudException;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.clouds.CloudInstanceUserData;

import java.io.IOException;
import java.util.*;


/**
 * Created by Asaf Gur.
 */

/// AnkaCloudConnector is a facade connecting TC cloud profile to anka cloud
/// Each profile should have one AnkaCloudConnector
///

public class AnkaCloudConnector {

    private final String host;
    private final String port;
    private final String agentPath;
    private final String serverUrl;
    private final Integer agentPoolId;
    private final String profileId;
    private String sshUser;
    private String sshPassword;
    private final AnkaAPI ankaAPI;

    public AnkaCloudConnector(String host, String port, String sshUser,
                              String sshPassword, String agentPath, String serverUrl,
                              Integer agentPoolId, String profileId) {
        this.host = host;
        this.port = port;
        this.sshUser = sshUser;
        this.sshPassword = sshPassword;
        this.agentPath = agentPath;
        this.serverUrl = serverUrl;
        this.agentPoolId = agentPoolId;
        this.profileId = profileId;
        this.ankaAPI = AnkaAPI.getInstance();
    }

    public AnkaCloudInstance startNewInstance(AnkaCloudImage cloudImage) throws AnkaUnreachableInstanceException {
        try {
            AnkaMgmtVm vm = this.ankaAPI.makeAnkaVm(this.host, this.port, cloudImage.getId(), cloudImage.getTag(), null, 22);
            vm.waitForBoot();
            HashMap<String, String > properties = new HashMap<>();
            if (this.serverUrl != null && this.serverUrl.length() > 0) {
                properties.put(AnkaConstants.SERVER_URL_KEY, this.serverUrl);
            }

            properties.put(AnkaConstants.ENV_INSTANCE_ID_KEY, vm.getId());
            properties.put(AnkaConstants.ENV_IMAGE_ID_KEY, cloudImage.getId());
            properties.put(AnkaConstants.ENV_PROFILE_ID, profileId);
            properties.put(AnkaConstants.ENV_ANKA_CLOUD_KEY, AnkaConstants.ENV_ANKA_CLOUD_VALUE);
            String vmName = vm.getName();
            if (vmName != null) {
                properties.put(AnkaConstants.AGENT_NAME, vmName);
            } else {
                properties.put(AnkaConstants.AGENT_NAME, String.format("%s_%s", cloudImage.getId(), vm.getId()));
            }
            AnkaSSHPropertiesSetter propertiesSetter = new AnkaSSHPropertiesSetter(vm, sshUser, sshPassword, agentPath);
            try {
                propertiesSetter.setProperties(properties);
                return new AnkaCloudInstance(vm, cloudImage);
            } catch (AnkaUnreachableInstanceException e) {
                vm.terminate();
                throw e;
            }

        } catch (AnkaMgmtException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void terminateInstance(CloudInstance cloudInstance) {
        AnkaCloudInstance instance = (AnkaCloudInstance)cloudInstance;
        AnkaMgmtVm vm = instance.getVm();
        vm.terminate();
        AnkaCloudImage image = (AnkaCloudImage) instance.getImage();
        image.populateInstances();
    }


    public Collection<AnkaCloudInstance> getImageInstances(AnkaCloudImage image) {
        List<AnkaCloudInstance> instances = new ArrayList<>();
        try {
            List<AnkaMgmtVm> ankaMgmtVms = this.ankaAPI.listVms(this.host, this.port);

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
        AnkaCloudStatus ankaCloudStatus = this.ankaAPI.status(this.host, this.port);
        if (ankaCloudStatus == null) {
            return false;
        }
        return ankaCloudStatus.getStatus().toLowerCase().equals("running");
    }


    public Integer getAgentPoolId() {
        return agentPoolId;
    }

}
