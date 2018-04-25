package com.veertu;

import jetbrains.buildServer.clouds.*;
import jetbrains.buildServer.serverSide.AgentDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AnkaCloudClientEx implements CloudClientEx {


    private final AnkaCloudConnector connector;
    private Map<String, AnkaCloudImage> images;


    public AnkaCloudClientEx(AnkaCloudConnector connector) {
        this.connector = connector;
        this.images = new HashMap<>();
    }

    @NotNull
    @Override
    public CloudInstance startNewInstance(@NotNull CloudImage cloudImage, @NotNull CloudInstanceUserData userData) throws QuotaException {
        return this.connector.startNewInstance(cloudImage, userData);
    }

    @Override
    public void restartInstance(@NotNull CloudInstance cloudInstance) {
        // TODO: do something about this!
    }

    @Override
    public void terminateInstance(@NotNull CloudInstance cloudInstance) {
        this.connector.terminateInstance(cloudInstance);

    }

    @Override
    public void dispose() {
        // TODO: figure out what should happen here (what am i disposing off?)
    }

    @Override
    public boolean isInitialized() {
        return this.connector.isRunning();
    }

    @Nullable
    @Override
    public CloudImage findImageById(@NotNull String s) throws CloudException {
        if (this.images.isEmpty()) {
            Collection<AnkaCloudImage> images = this.connector.getImages();
            for (AnkaCloudImage image: images) {
                this.images.put(image.getId(), image);
            }
        }
        return this.images.getOrDefault(s, null);
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
        return connector.getInstanceById(instanceId, (AnkaCloudImage)image);
    }

    @NotNull
    @Override
    public Collection<? extends CloudImage> getImages() throws CloudException {
        if (this.images.isEmpty()) {
            Collection<AnkaCloudImage> images = this.connector.getImages();
            for (AnkaCloudImage image: images) {
                this.images.put(image.getId(), image);
            }
        }
        return this.images.values();

    }

    @Nullable
    @Override
    public CloudErrorInfo getErrorInfo() {
        return null; // TODO: figure out what to do here
    }

    @Override
    public boolean canStartNewInstance(@NotNull CloudImage cloudImage) {
        Collection<? extends CloudInstance> imageInstances = connector.getImageInstances((AnkaCloudImage) cloudImage);
        return imageInstances.size() < connector.getMaxInstances();
    }

    @Nullable
    @Override
    public String generateAgentName(@NotNull AgentDescription agentDescription) {
        return null; // TODO: figure out what to do here
    }
}
