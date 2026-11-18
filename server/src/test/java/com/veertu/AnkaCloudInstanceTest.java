package com.veertu;

import com.veertu.ankaMgmtSdk.AnkaVmInstance;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class AnkaCloudInstanceTest {

    @Test
    void networkIdentityIsNullWhileVmInfoIsMissing() {
        AnkaVmInstance vm = new AnkaVmInstance("inst-1", new JSONObject()
            .put("instance_state", "Scheduling")
            .put("vmid", "template-1"));
        AnkaCloudInstance instance = new AnkaCloudInstance("inst-1", null) {
            @Override
            public AnkaVmInstance getVm() {
                return vm;
            }
        };

        assertNull(instance.getNetworkIdentity());
    }
}
