package com.veertu;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.AnkaVmFactory;
import com.veertu.ankaMgmtSdk.AnkaVmTemplate;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import jetbrains.buildServer.clouds.CloudException;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.clouds.CloudInstanceUserData;

import java.io.IOException;
import java.util.*;


/// AnkaCloudConnector is a facade connecting TC cloud profile to anka cloud
/// Each profile should have one AnkaCloudConnector
///

public class AnkaCloudConnector {

    private final String host;
    private final String port;
    private final String agentPath;
    private final String serverUrl;
    private String imageId;
    private String imageTag;
    private String sshUser;
    private String sshPassword;
    private final AnkaVmFactory vmFactory;

    public AnkaCloudConnector(String host, String port, String imageId, String imageTag,
                              String sshUser, String sshPassword, String agentPath, String serverUrl) {
        this.host = host;
        this.port = port;
        this.imageId = imageId;
        this.imageTag = imageTag;
        this.sshUser = sshUser;
        this.sshPassword = sshPassword;
        this.agentPath = agentPath;
        this.serverUrl = serverUrl;
        this.vmFactory = AnkaVmFactory.getInstance();

        // TODO: add list of images that will be used for this profile,
        // maybe return them instead of creating new instances all the time
    }

    public AnkaCloudInstance startNewInstance(CloudImage cloudImage, CloudInstanceUserData userData) {
        String imageId = cloudImage.getId();
        try {
            AnkaMgmtVm vm = this.vmFactory.makeAnkaVm(this.host, this.port, imageId, null, null, 22);
            vm.waitForBoot();
            HashMap<String, String > properties = new HashMap<>();
            if (this.serverUrl != null && this.serverUrl.length() > 0) {
                properties.put("serverUrl", this.serverUrl);
            }
            properties.put("env.INSTANCE_ID", vm.getId());
            properties.put("env.IMAGE_ID", cloudImage.getId());
            // TODO: put a variable that says the this istance belongs to anka cloud
            AnkaSSHPropertiesSetter propertiesSetter = new AnkaSSHPropertiesSetter(vm, sshUser, sshPassword, agentPath);
            propertiesSetter.setProperties(properties);
            return new AnkaCloudInstance(vm, cloudImage);

        } catch (AnkaMgmtException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void terminateInstance(CloudInstance cloudInstance) {
        AnkaCloudInstance instance = (AnkaCloudInstance)cloudInstance;
        AnkaMgmtVm vm = instance.getVm();
        vm.terminate();
    }

    public Collection<? extends CloudImage> getImages() throws CloudException {
        ArrayList<CloudImage> images = new ArrayList<>();
        try {
            List<AnkaVmTemplate> ankaVmTemplates = this.vmFactory.listTemplates(this.host, this.port);
            for (AnkaVmTemplate template: ankaVmTemplates) {
                List<String> tags = this.vmFactory.listTemplateTags(this.host, this.port, template.getId());
                AnkaCloudImage newImage = new AnkaCloudImage(this, template.getId(), template.getName(), tags);
                images.add(newImage);
            }
            return images;
        } catch (AnkaMgmtException e) {
//            throw new CloudException(e.getMessage());
            return images;
        }
    }

    public CloudImage findImageById(String id)  throws CloudException {
        try {
            List<AnkaVmTemplate> ankaVmTemplates = this.vmFactory.listTemplates(this.host, this.port);
            for (AnkaVmTemplate template : ankaVmTemplates) {
                if (template.getId().equals(id)) {
                    List<String> tags = vmFactory.listTemplateTags(this.host, this.port, template.getId());
                    AnkaCloudImage newImage = new AnkaCloudImage(this, template.getId(), template.getName(), tags);
                    return newImage;
                }
            }

            return null;
        } catch (AnkaMgmtException e) {
            throw new CloudException(e.getMessage());
        }
    }

    public Collection<? extends CloudInstance> getImageInstances(AnkaCloudImage image) {
        List<AnkaCloudInstance> instances = new ArrayList<>();
        try {
            List<AnkaMgmtVm> ankaMgmtVms = this.vmFactory.listVms(this.host, this.port);

            for (AnkaMgmtVm vm: ankaMgmtVms) {
                if (vm.getImageId().equals(image.getId())) {
                    AnkaCloudInstance instance = new AnkaCloudInstance(vm, image);
                    instances.add(instance);

                }
            }
            return instances;
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
            return instances;
        }

    }

    public CloudInstance getInstanceById(String id, AnkaCloudImage image) {
        try {
            AnkaMgmtVm vm = this.vmFactory.getVm(this.host, this.port, id);
            return new AnkaCloudInstance(vm, image);
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
            return null;
        }
    }
}
