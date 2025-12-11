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

    private final String templateId;
    private final String templateName;
    private final String templateTag;
    private final String groupId;
    private final String vmNameTemplate;
    private String externalId;
    private Integer vCpuCount;
    private Integer ramSize;
    private final AnkaCloudConnector connector;
    private final ConcurrentHashMap<String, AnkaCloudInstance> instances;
    private String errorMsg;
    private long errorTimestamp;
    
    // Error display duration in milliseconds (30 seconds)
    private static final long ERROR_DISPLAY_DURATION_MS = 30000;

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

    public AnkaCloudImage(
        AnkaCloudConnector connector, 
        String templateId, 
        String templateName, 
        String templateTag, 
        String groupId, 
        String vmNameTemplate,
        String externalId,
        Integer vCpuCount,
        Integer ramSize
    ) {
        this.connector = connector;
        this.templateId = templateId;
        this.templateName = templateName;
        this.groupId = groupId;
        if (templateTag != null && templateTag.length() > 0) {
            this.templateTag = templateTag;
        } else {
            this.templateTag = null;
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

    public String getString() {
        return String.format("AnkaCloudImage{templateId=%s, templateName=%s, templateTag=%s, groupId=%s, vmNameTemplate=%s, externalId=%s, vCpuCount=%d, ramSize=%d}",
            templateId, templateName, templateTag, groupId, vmNameTemplate, externalId, vCpuCount, ramSize);
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
        return getTemplateId();
    }
    public String getTemplateId() {
        return this.templateId;
    }

    @NotNull
    @Override
    public String getName() {
        return gettemplateName();
    }
    public String gettemplateName() {
        return this.templateName;
    }

    public String getTemplateTag() {
        return this.templateTag;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getVmNameTemplate() {
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
        // Auto-expire error after display duration to allow retries
        if (this.errorMsg != null) {
            long elapsedMs = System.currentTimeMillis() - this.errorTimestamp;
            if (elapsedMs < ERROR_DISPLAY_DURATION_MS) {
                long remainingSeconds = (ERROR_DISPLAY_DURATION_MS - elapsedMs) / 1000;
                String retryMessage = String.format("%s (retry available in %d seconds)", this.errorMsg, remainingSeconds);
                return new CloudErrorInfo(retryMessage);
            } else {
                // Error has expired, clear it
                this.errorMsg = null;
            }
        }
        return null;
    }

    public void clearError() {
        this.errorMsg = null;
        this.errorTimestamp = 0;
    }

    public synchronized AnkaCloudInstance startNewInstance(CloudInstanceUserData userData, InstanceUpdater updater) {
        LOG.info(String.format("[startNewInstance:Image] ENTER - templateId=%s, externalId=%s, currentInstanceCount=%d, instanceIds=%s",
            this.templateId, userData.getProfileId(), this.instances.size(), this.instances.keySet()));
        try {
            LOG.info(String.format("[startNewInstance:Image] Calling connector.startNewInstance for templateId=%s", this.templateId));
            AnkaCloudInstance instance = this.connector.startNewInstance(this, updater, userData);
            LOG.info(String.format("[startNewInstance:Image] Connector returned instanceId=%s", instance.getInstanceId()));
            
            // Add instance to local map immediately to prevent duplicate starts
            // while waiting for the controller to reflect the new instance
            this.instances.put(instance.getInstanceId(), instance);
            LOG.info(String.format("[startNewInstance:Image] Added to local map, instanceCount=%d, instanceIds=%s",
                this.instances.size(), this.instances.keySet()));
            
            populateInstances();
            LOG.info(String.format("[startNewInstance:Image] After populateInstances, instanceCount=%d, instanceIds=%s",
                this.instances.size(), this.instances.keySet()));
            
            this.errorMsg = null; // Clear any previous error on success
            LOG.info(String.format("[startNewInstance:Image] EXIT SUCCESS - templateId=%s, instanceId=%s", this.templateId, instance.getInstanceId()));
            return instance;
        } catch (AnkaMgmtException e) {
            this.errorMsg = e.getMessage();
            this.errorTimestamp = System.currentTimeMillis();
            LOG.error(String.format("[startNewInstance:Image] EXIT ERROR - templateId=%s, error=%s", this.templateId, e.getMessage()), e);
            return null;
        }
    }

    public synchronized void populateInstances() {
        LOG.info(String.format("[populateInstances] ENTER - templateId=%s, beforeCount=%d, beforeIds=%s",
            this.templateId, this.instances.size(), this.instances.keySet()));
        
        Collection<AnkaCloudInstance> imageInstances = this.connector.getImageInstances(this);
        
        StringBuilder fetchedIds = new StringBuilder();
        for (AnkaCloudInstance inst : imageInstances) {
            if (fetchedIds.length() > 0) fetchedIds.append(", ");
            fetchedIds.append(inst.getInstanceId());
        }
        LOG.info(String.format("[populateInstances] Fetched from controller: count=%d, ids=[%s]",
            imageInstances.size(), fetchedIds.toString()));
        
        this.instances.clear();
        for (AnkaCloudInstance instance: imageInstances) {
            this.instances.put(instance.getInstanceId(), instance);
        }
        
        LOG.info(String.format("[populateInstances] EXIT - templateId=%s, afterCount=%d, afterIds=%s",
            this.templateId, this.instances.size(), this.instances.keySet()));
    }


}
