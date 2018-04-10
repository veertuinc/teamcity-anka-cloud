package com.veertu;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.utils.AnkaConstants;
import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.clouds.InstanceStatus;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import jetbrains.buildServer.serverSide.AgentDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Map;

public class AnkaCloudInsatnce implements CloudInstance {


    public AnkaMgmtVm getVm() {
        return vm;
    }

    private final AnkaMgmtVm vm;
    private final CloudImage image;

    public AnkaCloudInsatnce(AnkaMgmtVm vm, CloudImage image) {
        this.vm = vm;
        this.image = image;
    }

    @NotNull
    @Override
    public String getInstanceId() {
        return vm.getId();
    }

    @NotNull
    @Override
    public String getName() {
        return vm.getName();
    }

    @NotNull
    @Override
    public String getImageId() {
        return image.getId();
    }

    @NotNull
    @Override
    public CloudImage getImage() {
        return this.image;
    }

    @NotNull
    @Override
    public Date getStartedTime() {
        try {
            return this.vm.getCreatedTime();
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Nullable
    @Override
    public String getNetworkIdentity() {
        return this.vm.getConnectionIp();
    }

    @NotNull
    @Override
    public InstanceStatus getStatus() {
        switch (this.vm.getState().toLowerCase()) {
            case "scheduling":
                return InstanceStatus.SCHEDULED_TO_START;
            case "starting":
                return InstanceStatus.STARTING;
            case "started":
                return InstanceStatus.RUNNING;
            case "terminating":
                return InstanceStatus.STOPPING;
            case "terminated":
                return InstanceStatus.STOPPED;
            case "error":
                return InstanceStatus.ERROR;
            default:
                return InstanceStatus.UNKNOWN;
        }
    }

    @Nullable
    @Override
    public CloudErrorInfo getErrorInfo() {
        return null;
    }

    @Override
    public boolean containsAgent(@NotNull AgentDescription agentDescription){
        final Map<String, String> configParams = agentDescription.getConfigurationParameters();
        return getInstanceId().equals(configParams.get(AnkaConstants.INSTANCE_ID));
    }
}
