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


/**
 * Created by Asaf Gur.
 */

public class AnkaCloudInstance implements CloudInstance {


    public AnkaMgmtVm getVm() {
        return vm;
    }

    private final AnkaMgmtVm vm;
    private final CloudImage image;
    private Date createdTime;

    public AnkaCloudInstance(AnkaMgmtVm vm, CloudImage image) {
        this.vm = vm;
        this.image = image;
        this.createdTime = new Date();
    }

    @NotNull
    @Override
    public String getInstanceId() {
        return vm.getId();
    }

    @NotNull
    @Override
    public String getName() {
        String name = vm.getName();
        if (name != null ) {
            return name;
        }
        return "-";

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
            Date created = this.vm.getCreatedTime();
            if (created != null) {
                this.createdTime = created;
            }
            return this.createdTime;
        } catch (AnkaMgmtException e) {
            return this.createdTime;
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
        String state = this.vm.getState();
        switch (state.toLowerCase()) {
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
        Map<String, String> availableParameters = agentDescription.getAvailableParameters();
        return availableParameters.containsKey(AnkaConstants.ENV_PROFILE_ID);

    }
}
