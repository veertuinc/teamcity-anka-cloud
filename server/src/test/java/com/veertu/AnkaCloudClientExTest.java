package com.veertu;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import jetbrains.buildServer.clouds.CanStartNewInstanceResult;
import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudImage;
import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.clouds.InstanceStatus;
import jetbrains.buildServer.serverSide.AgentDescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnkaCloudClientExTest {

    @Test
    void stoppedInstanceDoesNotConsumeTheLimit() {
        AnkaCloudClientEx client = new AnkaCloudClientEx(1);
        CanStartNewInstanceResult stoppedOnly = client.canStartNewInstanceWithDetails(
            image(instance("gone", InstanceStatus.STOPPED)));
        assertTrue(stoppedOnly.isPositive());

        CanStartNewInstanceResult runningBesideStopped = client.canStartNewInstanceWithDetails(
            image(
                instance("live", InstanceStatus.RUNNING),
                instance("gone", InstanceStatus.STOPPED)));
        assertFalse(runningBesideStopped.isPositive());
        assertEquals("Max instances limit reached", runningBesideStopped.getReason());
    }

    @Test
    void stoppingInstanceConsumesTheLimit() {
        AnkaCloudClientEx client = new AnkaCloudClientEx(1);
        CanStartNewInstanceResult result = client.canStartNewInstanceWithDetails(
            image(instance("going", InstanceStatus.STOPPING)));
        assertFalse(result.isPositive());
        assertEquals("Max instances limit reached", result.getReason());
    }

    private static CloudImage image(CloudInstance... instances) {
        List<CloudInstance> listed = List.of(instances);
        return new CloudImage() {
            @Override
            public String getId() {
                return "image";
            }

            @Override
            public String getName() {
                return "image";
            }

            @Override
            public Collection<? extends CloudInstance> getInstances() {
                return listed;
            }

            @Override
            public CloudInstance findInstanceById(String id) {
                return null;
            }

            @Override
            public Integer getAgentPoolId() {
                return null;
            }

            @Override
            public CloudErrorInfo getErrorInfo() {
                return null;
            }
        };
    }

    private static CloudInstance instance(String id, InstanceStatus status) {
        return new CloudInstance() {
            @Override
            public String getInstanceId() {
                return id;
            }

            @Override
            public String getName() {
                return id;
            }

            @Override
            public String getImageId() {
                return "image";
            }

            @Override
            public CloudImage getImage() {
                return null;
            }

            @Override
            public Date getStartedTime() {
                return new Date(0);
            }

            @Override
            public String getNetworkIdentity() {
                return null;
            }

            @Override
            public InstanceStatus getStatus() {
                return status;
            }

            @Override
            public CloudErrorInfo getErrorInfo() {
                return null;
            }

            @Override
            public boolean containsAgent(AgentDescription agentDescription) {
                return false;
            }
        };
    }
}
