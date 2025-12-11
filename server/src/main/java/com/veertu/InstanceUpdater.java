package com.veertu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.intellij.openapi.diagnostic.Logger;

import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;

/**
 * Created by Asaf Gur.
 */

public class InstanceUpdater {

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

    private final long delay;
    private final Collection<AnkaCloudClientEx> clients;
    private final ExecutorServices executors;

    public InstanceUpdater(ExecutorServices executors) {
        this.delay = 5000;
        this.clients = new ArrayList<>();
        this.executors = executors;
        executors.getNormalExecutorService().scheduleWithFixedDelay(this::populateInstances, delay, delay, TimeUnit.MILLISECONDS);
    }

    public void registerClient(AnkaCloudClientEx client) {
        this.clients.add(client);
    }

    public void unRegisterClient(AnkaCloudClientEx client) {
        this.clients.remove(client);
    }

    private void populateInstances() {
        LOG.info("[InstanceUpdater:populateInstances] Scheduled update running");
        for (AnkaCloudClientEx client : clients) {
            Collection<? extends CloudImage> images = client.getImages();
            for (CloudImage image: images) {
                AnkaCloudImage ankaImage = (AnkaCloudImage)image;
                LOG.info(String.format("[InstanceUpdater:populateInstances] Updating image=%s", ankaImage.getTemplateId()));
                ankaImage.populateInstances();
            }
        }
    }

    public void executeTaskInBackground(Runnable r) {
        executors.getNormalExecutorService().schedule(r, 0, TimeUnit.MILLISECONDS);
    }
}
