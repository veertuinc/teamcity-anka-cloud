package com.veertu;

import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AnkaCloudImage implements CloudImage {

    private final String id;
    private final String name;
    private final String tag;
    private final AnkaCloudConnector connector;
    private Map<String, AnkaCloudInstance> instances;

    public AnkaCloudImage(AnkaCloudConnector connector, String id, String name, String tag) {
        this.connector = connector;
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.instances = new HashMap<>();
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


    @NotNull
    @Override
    public Collection<? extends CloudInstance> getInstances() {
        if (this.instances.isEmpty()) {
            Collection<AnkaCloudInstance> imageInstances = this.connector.getImageInstances(this);
            for (AnkaCloudInstance instance: imageInstances) {
                this.instances.put(instance.getInstanceId(), instance);
            }
        }
        return this.instances.values();
    }

    @Nullable
    @Override
    public CloudInstance findInstanceById(@NotNull String id) {
        if (this.instances.isEmpty()) {
            Collection<AnkaCloudInstance> imageInstances = this.connector.getImageInstances(this);
            for (AnkaCloudInstance instance: imageInstances) {
                this.instances.put(instance.getInstanceId(), instance);
            }
        }
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
        return null;
        // TODO: check if we need this
    }

    public void removeInstance(AnkaCloudInstance instance) {
        if (!this.instances.isEmpty()) {
            this.instances.remove(instance.getInstanceId());
        }
    }
}
