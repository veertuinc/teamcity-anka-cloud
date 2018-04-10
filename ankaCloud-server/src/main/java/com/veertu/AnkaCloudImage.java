package com.veertu;

import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class AnkaCloudImage implements CloudImage {

    private final String id;
    private final String name;

    public AnkaCloudImage(String id, String name) {
        this.id = id;
        this.name = name;
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

    @NotNull
    @Override
    public Collection<? extends CloudInstance> getInstances() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public CloudInstance findInstanceById(@NotNull String id) {
        return null;
    }

    @Nullable
    @Override
    public Integer getAgentPoolId() {
        return null;
    }

    @Nullable
    @Override
    public CloudErrorInfo getErrorInfo() {
        return null;
    }
}
