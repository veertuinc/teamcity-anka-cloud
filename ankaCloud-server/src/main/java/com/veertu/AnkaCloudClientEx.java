package com.veertu;

import com.veertu.utils.AnkaConstants;
import jetbrains.buildServer.clouds.*;
import jetbrains.buildServer.serverSide.AgentDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnkaCloudClientEx implements CloudClientEx {


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
        updater.registerClient(this);
    }

    @NotNull
    @Override
    public CloudInstance startNewInstance(@NotNull CloudImage cloudImage, @NotNull CloudInstanceUserData userData) throws QuotaException {
        AnkaCloudImage image = (AnkaCloudImage)cloudImage;
        return image.startNewInstance(userData);
//        return this.connector.startNewInstance(cloudImage, userData);
    }

    @Override
    public void restartInstance(@NotNull CloudInstance cloudInstance) {
        throw new UnsupportedOperationException("Restart not implemented");
    }

    @Override
    public void terminateInstance(@NotNull CloudInstance cloudInstance) {
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
        Map<String, String> availableParameters = agentDescription.getAvailableParameters();
        String instanceId = availableParameters.get("env.INSTANCE_ID");
        String imageId = availableParameters.get("env.IMAGE_ID");
        if (instanceId == null || imageId == null) {
            return null;
        }
        CloudImage image = findImageById(imageId);
        if (image != null) {
            return image.findInstanceById(instanceId);
        }
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
    public boolean canStartNewInstance(@NotNull CloudImage cloudImage) {
        Collection<? extends CloudInstance> imageInstances = cloudImage.getInstances();
        return imageInstances.size() < this.maxInstances;
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
