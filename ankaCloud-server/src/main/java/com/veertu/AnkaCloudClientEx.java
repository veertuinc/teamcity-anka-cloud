package com.veertu;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import jetbrains.buildServer.clouds.*;
import com.veertu.ankaMgmtSdk.AnkaVmFactory;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import jetbrains.buildServer.serverSide.AgentDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class AnkaCloudClientEx implements CloudClientEx {

    private final String cloudHost;
    private final String cloudPort;

    public AnkaCloudClientEx(String cloudHost,String cloudPort) {
        this.cloudHost = cloudHost;
        this.cloudPort = cloudPort;
    }

    @NotNull
    @Override
    public CloudInstance startNewInstance(@NotNull CloudImage cloudImage, @NotNull CloudInstanceUserData userData) throws QuotaException {
        String imageId = cloudImage.getId();
        AnkaVmFactory vmFactory = AnkaVmFactory.getInstance();
        String tag = userData.getAgentConfigurationParameter("tag");
        try {
            AnkaMgmtVm vm = vmFactory.makeAnkaVm(this.cloudHost, this.cloudPort, imageId, tag, null, 22);
            return new AnkaCloudInsatnce(vm, cloudImage);

        } catch (AnkaMgmtException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void restartInstance(@NotNull CloudInstance cloudInstance) {

    }

    @Override
    public void terminateInstance(@NotNull CloudInstance cloudInstance) {
        AnkaCloudInsatnce instance = (AnkaCloudInsatnce)cloudInstance;
        AnkaMgmtVm vm = instance.getVm();
        vm.terminate();
    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Nullable
    @Override
    public CloudImage findImageById(@NotNull String s) throws CloudException {
        return null;
    }

    @Nullable
    @Override
    public CloudInstance findInstanceByAgent(@NotNull AgentDescription agentDescription) {
        return null;
    }

    @NotNull
    @Override
    public Collection<? extends CloudImage> getImages() throws CloudException {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public CloudErrorInfo getErrorInfo() {
        return null;
    }

    @Override
    public boolean canStartNewInstance(@NotNull CloudImage cloudImage) {
        return true;
    }

    @Nullable
    @Override
    public String generateAgentName(@NotNull AgentDescription agentDescription) {
        return null;
    }
}
