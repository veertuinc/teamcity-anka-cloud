package com.veertu;

import java.util.Date;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.diagnostic.Logger;
import com.veertu.ankaMgmtSdk.AnkaVmInstance;
import com.veertu.common.AnkaConstants;

import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.clouds.InstanceStatus;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.AgentDescription;


/**
 * Created by Asaf Gur.
 */

public class AnkaCloudInstance implements CloudInstance {

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

    private final String vmId;
    private final CloudImage image;
    private final Date createdTime;

    public AnkaCloudInstance(String vmId, CloudImage image) {
        this.vmId = vmId;
        this.image = image;
        this.createdTime = new Date();
    }

    public AnkaVmInstance getVm() {
        return ((AnkaCloudImage)image).showInstance(vmId);
    }

    @NotNull
    @Override
    public String getInstanceId() {
        return vmId;
    }

    @NotNull
    @Override
    public String getName() {
        AnkaVmInstance vm = getVm();
        if (vm == null) {
            return "-";
        }
        String name = vm.getName();
        if (name == null || name.equals("")) {
            return "-";
        }
        return name;
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
        return this.createdTime;
    }

    @Nullable
    @Override
    public String getNetworkIdentity() {
        AnkaVmInstance vm = getVm();
        if (vm == null) {
            return null;
        }
        return vm.getVmInfo().getHostIp();
    }

    @NotNull
    @Override
    public InstanceStatus getStatus() {
        AnkaVmInstance vm = getVm();
        if (vm == null) {  // vm does not exist in controller
            return InstanceStatus.STOPPED;
        }
        String state = vm.getSessionState();
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
        AnkaVmInstance vm = getVm();
        if (vm != null && vm.isInError()) {
            String message = vm.getMessage();
            if (message != null && !message.isEmpty()) {
                return new CloudErrorInfo(message);
            }
            return new CloudErrorInfo("VM is in error state");
        }
        return null;
    }

    @Override
    public boolean containsAgent(@NotNull AgentDescription agentDescription) {
        Map<String, String> availableParameters = agentDescription.getAvailableParameters();
        String instanceId = availableParameters.get(AnkaConstants.ENV_INSTANCE_ID_KEY);

        LOG.info(String.format("containsAgent: property instanceId = %s, VM instance id: %s",
                instanceId == null ? "null" : instanceId,
                vmId == null ? "null": vmId));
        if (instanceId == null || vmId == null)
            return false;
        return instanceId.equals(vmId);

    }
}
