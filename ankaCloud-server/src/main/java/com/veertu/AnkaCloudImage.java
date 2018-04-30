package com.veertu;

import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.clouds.CloudInstanceUserData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class AnkaCloudImage implements CloudImage {

    private final String id;
    private final String name;
    private final String tag;
    private final AnkaCloudConnector connector;
    private final ConcurrentHashMap<String, AnkaCloudInstance> instances;
    private String errorMsg;

    public AnkaCloudImage(AnkaCloudConnector connector, String id, String name, String tag) {
        this.connector = connector;
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.instances = new ConcurrentHashMap<>();
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

    public AnkaCloudInstance startNewInstance(CloudInstanceUserData userData) {
        try {
            AnkaCloudInstance instance = this.connector.startNewInstance(this);
            populateInstances();
            return instance;
        } catch (AnkaUnreachableInstanceException e) {
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
