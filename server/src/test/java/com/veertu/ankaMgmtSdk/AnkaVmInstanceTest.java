package com.veertu.ankaMgmtSdk;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnkaVmInstanceTest {

    @Test
    void parsesRequiredFieldsAndOptionalVmInfo() {
        JSONObject vm = new JSONObject()
            .put("instance_state", "Started")
            .put("vmid", "template-1")
            .put("external_id", "ext-9")
            .put("message", "ok")
            .put("vminfo", new JSONObject()
                .put("uuid", "vm-uuid")
                .put("name", "build-agent")
                .put("status", "running")
                .put("ip", "10.0.0.5")
                .put("host_ip", "10.0.0.1"));

        AnkaVmInstance instance = new AnkaVmInstance("inst-1", vm);

        assertEquals("inst-1", instance.getId());
        assertEquals("Started", instance.getSessionState());
        assertEquals("template-1", instance.getTemplateId());
        assertEquals("ext-9", instance.getExternalId());
        assertEquals("ok", instance.getMessage());
        assertEquals("build-agent", instance.getName());
        assertEquals("vm-uuid", instance.getVmInfo().getUuid());
        assertTrue(instance.isStarted());
        assertFalse(instance.isPulling());
    }

    @Test
    void makeAnkaVmSessionFromJsonUsesNestedVmObject() {
        JSONObject payload = new JSONObject()
            .put("instance_id", "inst-2")
            .put("vm", new JSONObject()
                .put("instance_state", "Scheduling")
                .put("vmid", "template-2"));

        AnkaVmInstance instance = AnkaVmInstance.makeAnkaVmSessionFromJson(payload);

        assertEquals("inst-2", instance.getId());
        assertEquals("Scheduling", instance.getSessionState());
        assertTrue(instance.isScheduling());
        assertTrue(instance.isSchedulingOrPulling());
        assertNull(instance.getVmInfo());
    }

    @Test
    void stateHelpersCoverTerminalAndErrorPaths() {
        assertTrue(instanceWithState("Terminated").isTerminatingOrTerminated());
        assertTrue(instanceWithState("Terminating").isTerminatingOrTerminated());
        assertFalse(instanceWithState("Started").isTerminatingOrTerminated());

        assertTrue(instanceWithState("pulling").isPulling());
        assertTrue(instanceWithState("Pulling").isSchedulingOrPulling());
        assertTrue(instanceWithState("error").isInError());
        assertTrue(instanceWithState("pushing").isPushing());
    }

    private static AnkaVmInstance instanceWithState(String state) {
        JSONObject vm = new JSONObject()
            .put("instance_state", state)
            .put("vmid", "template");
        return new AnkaVmInstance("id", vm);
    }
}
