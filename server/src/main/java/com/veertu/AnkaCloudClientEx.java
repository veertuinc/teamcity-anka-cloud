package com.veertu;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

/**
 * Created by Asaf Gur.
 */

public class AnkaCloudClientEx implements CloudClientEx {

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

    private final AnkaCloudConnector connector;
    private InstanceUpdater updater;
    private final int maxInstances;
    private final ConcurrentHashMap<String, AnkaCloudImage> imagesMap;


    public AnkaCloudClientEx(AnkaCloudConnector connector, InstanceUpdater updater, Collection<AnkaCloudImage> images, int maxInstances) {
        this.connector = connector;
        this.updater = updater;
        this.maxInstances = maxInstances;
        this.imagesMap = new ConcurrentHashMap<>();
        for (AnkaCloudImage image: images) {
            imagesMap.put(image.getId(), image);
        }
        LOG.info(String.format("Registering AnkaCloudClientEx %s to updater", this.toString()));
        updater.registerClient(this);
    }

    @NotNull
    @Override
    public CloudInstance startNewInstance(@NotNull CloudImage cloudImage, @NotNull CloudInstanceUserData userData) throws QuotaException {
        LOG.info(String.format("Starting new instance for image %s(%s) on AnkaCloudClientEx %s",
                cloudImage.getName(), cloudImage.getId(), this.toString()));

        AnkaCloudImage image = (AnkaCloudImage)cloudImage;
        return image.startNewInstance(userData, updater);
//        return this.connector.startNewInstance(cloudImage, userData);
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
        updater.unRegisterClient(this);
    }

    @Override
    public boolean isInitialized() {
        return this.connector.isRunning();
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
        String imageId = availableParameters.get(AnkaConstants.ENV_IMAGE_ID_KEY);
        if (instanceId == null || imageId == null) {
            LOG.info(String.format("No instance for %s", agentDescription.toString()));
            return null;
        }
        LOG.info(String.format("findInstanceByAgent -> image id: %s , instance_id", imageId, instanceId));
        CloudImage image = findImageById(imageId);
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
        String imageId = availableParameters.get(AnkaConstants.ENV_IMAGE_ID_KEY);
        if (instanceId != null && imageId != null) {
            CloudImage image = findImageById(imageId);
            if (image == null) {
                return null;
            }
            return String.format("%s_%s", image.getName(), instanceId);
        }
        return null;
    }
}
