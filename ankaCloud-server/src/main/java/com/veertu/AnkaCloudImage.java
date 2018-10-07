package com.veertu;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.clouds.CloudInstanceUserData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by Asaf Gur.
 */

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
        if (tag != null && tag.length() > 0) {
            this.tag = tag;
        } else {
            this.tag = null;
        }
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
        synchronized (this.instances) {
            return this.instances.values();
        }
    }

    @Nullable
    @Override
    public CloudInstance findInstanceById(@NotNull String id) {
        synchronized (this.instances) {
            return this.instances.get(id);
        }
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
            AnkaCloudInstance instance = this.connector.startNewInstance(this, updater);
            synchronized (this.instances) {
                this.instances.put(instance.getInstanceId(), instance);
            }
            populateInstances();
            return instance;
        } catch (AnkaMgmtException e) {
            this.errorMsg = e.getMessage();
            return null;
        }
    }

    public  void removeInstance(String id) {
        synchronized (this.instances) {
            this.instances.remove(id);
        }
    }

    public void populateInstances() {
        Collection<AnkaCloudInstance> imageInstances = this.connector.getImageInstances(this);
        Set<String> ids = new HashSet<>();

        synchronized (this.instances) {
            for (AnkaCloudInstance instance: imageInstances) {
                ids.add(instance.getInstanceId());
                if (instances.containsKey(instance.getInstanceId())) {
                    this.instances.put(instance.getInstanceId(), instance);
                }
            }
            for (String instanceId: instances.keySet()) {
                if (!ids.contains(instanceId))
                    instances.remove(instanceId);

            }
        }
    }
}
