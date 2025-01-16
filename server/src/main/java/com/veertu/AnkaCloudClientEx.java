package com.veertu;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.diagnostic.Logger;
import com.veertu.common.AnkaConstants;

import jetbrains.buildServer.clouds.CanStartNewInstanceResult;
import jetbrains.buildServer.clouds.CloudClientEx;
import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudException;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.clouds.CloudInstanceUserData;
import jetbrains.buildServer.clouds.QuotaException;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.AgentDescription;
import jetbrains.buildServer.serverSide.BuildAgentManagerEx;

/**
 * Created by Asaf Gur.
 */

public class AnkaCloudClientEx implements CloudClientEx {

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

    private final AnkaCloudConnector connector;
    private InstanceUpdater updater;
    private final int maxInstances;
    private final BuildAgentManagerEx buildAgentManager;
    private final ConcurrentHashMap<String, AnkaCloudImage> imagesMap;
    private final AtomicBoolean isControllerRunning = new AtomicBoolean(false);
    private final AtomicBoolean controllerStatusCheckThread = new AtomicBoolean(true);

    public AnkaCloudClientEx(
        AnkaCloudConnector connector,
        InstanceUpdater updater,
        Collection<AnkaCloudImage> images,
        int maxInstances,
        BuildAgentManagerEx buildAgentManager
    ) {
        this.connector = connector;
        this.updater = updater;
        this.maxInstances = maxInstances;
        this.buildAgentManager = buildAgentManager;
        this.imagesMap = new ConcurrentHashMap<>();
        for (AnkaCloudImage ankaCloudImage: images) {
            imagesMap.put(ankaCloudImage.getTemplateId(), ankaCloudImage);
        }
        LOG.info(String.format("Registering AnkaCloudClientEx %s to updater", this.toString()));
        updater.registerClient(this);
        controllerStatusThread();
    }

    @NotNull
    @Override
    public CloudInstance startNewInstance(@NotNull CloudImage cloudImage, @NotNull CloudInstanceUserData userData) throws QuotaException {
        AnkaCloudImage ankaCloudImage = (AnkaCloudImage)cloudImage;
        LOG.info(String.format("==== %s ====", ankaCloudImage.getString()));
        ankaCloudImage.setExternalId(userData.getProfileId());
        LOG.info(String.format("Starting new instance for image %s(%s) on AnkaCloudClientEx, externalId: %s",
            cloudImage.getName(), cloudImage.getId(), ankaCloudImage.getExternalId()));
        return ankaCloudImage.startNewInstance(userData, updater);
    }

    public void unregisterAgent(int agentId) {
        buildAgentManager.unregisterAgent(agentId, "Cloud instance has gone");
    }

    @Override
    public void restartInstance(@NotNull CloudInstance cloudInstance) {
        throw new UnsupportedOperationException("Restart not implemented");
    }

    @Override
    public void terminateInstance(@NotNull CloudInstance cloudInstance) {
        LOG.info(String.format("Terminating instance %s(%s) on AnkaCloudClientEx %s",
            cloudInstance.getName(), cloudInstance.getInstanceId(), this.toString()));
        this.connector.terminateInstance(cloudInstance);
    }

    @Override
    public void dispose() {
        LOG.info(String.format("Disposing AnkaCloudClientEx %s", this.toString()));
        updater.unRegisterClient(this);
        controllerStatusCheckThread.set(false);
    }

    private void controllerStatusThread() {
        final int sleepTime = 1000;
        final int checkInterval = 5000; // 5 seconds
        final long[] lastCheckTime = {0};
        Thread initializationThread = new Thread(() -> {
            while (controllerStatusCheckThread.get()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastCheckTime[0] >= checkInterval) {
                    isControllerRunning.set(connector.isRunning());
                    lastCheckTime[0] = currentTime;
                }
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.error(String.format("Initialization thread interrupted"));
                    break;
                }
            }
        });
        initializationThread.setDaemon(true);
        initializationThread.start();
    }

    @Override
    public boolean isInitialized() {
        // this runs hundreds of times a minute. Do not include API calls here.
        return isControllerRunning.get();
    }

    @Nullable
    @Override
    public CloudImage findImageById(@NotNull String s) throws CloudException {
        return this.imagesMap.getOrDefault(s, null);
    }

    @Nullable
    @Override
    public CloudInstance findInstanceByAgent(@NotNull AgentDescription agentDescription) {
        // this is how tc figures out which agent belongs to which instance
        LOG.info(String.format("Searching instance for %s", agentDescription.toString()));
        Map<String, String> availableParameters = agentDescription.getAvailableParameters();
        String instanceId = availableParameters.get(AnkaConstants.ENV_INSTANCE_ID_KEY);
        String templateId = availableParameters.get(AnkaConstants.ENV_TEMPLATE_ID_KEY);
        if (instanceId == null || templateId == null) {
            LOG.info(String.format("No instance for %s", agentDescription.toString()));
            return null;
        }
        LOG.info(String.format("findInstanceByAgent -> template_id: %s , instance_id: %s", templateId, instanceId));
        CloudImage image = findImageById(templateId);
        if (image != null) {
            LOG.info(String.format("Found instance %s for %s", instanceId, agentDescription.toString()));
            return image.findInstanceById(instanceId);
        }
        LOG.info(String.format("No instance for %s", agentDescription.toString()));
        return null;
    }

    @NotNull
    @Override
    public Collection<? extends CloudImage> getImages() throws CloudException {
        return this.imagesMap.values();

    }

    @Nullable
    @Override
    public CloudErrorInfo getErrorInfo() {
        return null;
    }

    @Override
    public CanStartNewInstanceResult canStartNewInstanceWithDetails(@NotNull CloudImage cloudImage) {
        Collection<? extends CloudInstance> imageInstances = cloudImage.getInstances();
        boolean canStart = imageInstances.size() < this.maxInstances;
        return canStart ? CanStartNewInstanceResult.yes() : CanStartNewInstanceResult.no("Max instances limit reached");
    }

    @Nullable
    @Override
    public String generateAgentName(@NotNull AgentDescription agentDescription) {
        Map<String, String> availableParameters = agentDescription.getAvailableParameters();
        String instanceId = availableParameters.get(AnkaConstants.ENV_INSTANCE_ID_KEY);
        String templateId = availableParameters.get(AnkaConstants.ENV_TEMPLATE_ID_KEY);
        if (instanceId != null && templateId != null) {
            CloudImage image = findImageById(templateId);
            if (image == null) {
                return null;
            }
            return String.format("%s_%s", image.getName(), instanceId);
        }
        return null;
    }
}
