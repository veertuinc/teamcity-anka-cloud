package com.veertu;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.diagnostic.Logger;
import com.veertu.ankaMgmtSdk.AnkaVmInstance;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.clouds.CloudInstanceUserData;
import jetbrains.buildServer.log.Loggers;

/**
 * Created by Asaf Gur.
 */

public class AnkaCloudImage implements CloudImage {

    private final String id;
    private final String name;
    private final String tag;
    private final String groupId;
    private final String vmNameTemplate;
    private String externalId;
    private Integer vCpuCount;
    private Integer ramSize;
    private final AnkaCloudConnector connector;
    private final ConcurrentHashMap<String, AnkaCloudInstance> instances;
    private String errorMsg;

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

    public AnkaCloudImage(
        AnkaCloudConnector connector, 
        String id, 
        String name, 
        String tag, 
        String groupId, 
        String vmNameTemplate,
        String externalId,
        Integer vCpuCount,
        Integer ramSize
    ) {
        this.connector = connector;
        this.id = id;
        this.name = name;
        this.groupId = groupId;
        if (tag != null && tag.length() > 0) {
            this.tag = tag;
        } else {
            this.tag = null;
        }
        this.instances = new ConcurrentHashMap<>();
        if (vmNameTemplate != null && vmNameTemplate.length() > 0) {
            this.vmNameTemplate = vmNameTemplate;
        } else {
            this.vmNameTemplate = "$ts";
        }
        this.externalId = externalId;
        if (vCpuCount != null) {
            this.vCpuCount = vCpuCount;
        }
        if (ramSize != null) {
            this.ramSize = ramSize;
        }
    }

    public AnkaVmInstance showInstance(String vmId) {
        return connector.showInstance(vmId);
    }

    @NotNull
    public String getExternalId() {
        return externalId;
    }

    public Integer getVCpuCount() {
        return vCpuCount;
    }

    public Integer getRamSize() {
        return ramSize;
    }

    @NotNull
    @Override
    public String getId() {
        return this.id;
    }

    @NotNull
    @Override
    public String getName() {
        return this.name;
    }


    public String getTag() {
        return tag;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getvmNameTemplate() {
        return vmNameTemplate;
    }

    public void setExternalId(String profileId) {
        this.externalId = profileId;
    }

    @NotNull
    @Override
    public Collection<? extends CloudInstance> getInstances() {
        return this.instances.values();
    }

    @Nullable
    @Override
    public CloudInstance findInstanceById(@NotNull String id) {
        return this.instances.get(id);
    }

    @Nullable
    @Override
    public Integer getAgentPoolId() {
        return this.connector.getAgentPoolId();
    }

    @Nullable
    @Override
    public CloudErrorInfo getErrorInfo() {
//        if (this.errorMsg != null) {
//            return new CloudErrorInfo(this.errorMsg);
//        }
        return null;
    }

    public AnkaCloudInstance startNewInstance(CloudInstanceUserData userData, InstanceUpdater updater) {
        try {
            LOG.info(String.format("Starting new instance for image %s(%s) on AnkaCloudImage, externalId: %s",
                this.id, this.name, userData.getProfileId()));
            AnkaCloudInstance instance = this.connector.startNewInstance(this, updater, userData);
            populateInstances();
            return instance;
        } catch (AnkaMgmtException e) {
            this.errorMsg = e.getMessage();
            return null;
        }
    }

    public void populateInstances() {
        Collection<AnkaCloudInstance> imageInstances = this.connector.getImageInstances(this);
        synchronized (this.instances) {
            this.instances.clear();
            for (AnkaCloudInstance instance: imageInstances) {
                this.instances.put(instance.getInstanceId(), instance);
            }
        }
    }


}
