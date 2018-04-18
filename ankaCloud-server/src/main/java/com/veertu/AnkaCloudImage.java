package com.veertu;

import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class AnkaCloudImage implements CloudImage {

    private final String id;
    private final String name;
    private final Collection<String> tags;
    private final AnkaCloudConnector connector;

    public AnkaCloudImage(AnkaCloudConnector connector, String id, String name, Collection<String> tags) {
        this.connector = connector;
        this.id = id;
        this.name = name;
        this.tags = tags;

        // TODO: accept an image with a tag
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

        return this.connector.getImageInstances(this);
    }

    @Nullable
    @Override
    public CloudInstance findInstanceById(@NotNull String id) {

        return this.connector.getInstanceById(id, this);
    }

    @Nullable
    @Override
    public Integer getAgentPoolId() {
        return null;
        // TODO: need to - either get pool id from tc or generate a pool id somehow
    }

    @Nullable
    @Override
    public CloudErrorInfo getErrorInfo() {
        return null;
        // TODO: check if we need this
    }
}
