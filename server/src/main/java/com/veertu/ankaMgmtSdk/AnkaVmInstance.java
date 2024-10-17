package com.veertu.ankaMgmtSdk;

import org.json.JSONObject;

/**
 * Created by asafgur on 17/05/2017.
 */
public class AnkaVmInstance extends AnkaVMRepresentation {

    private final String sessionState;
    private final String templateId;
    private AnkaVmInfo vmInfo;
    private String message;
    private String externalId;

    public AnkaVmInstance(String id, JSONObject jsonObject) {
        this.id = id;
        this.sessionState = jsonObject.getString("instance_state");
        this.templateId = jsonObject.getString("vmid");
        if (jsonObject.has("vminfo")) {
            JSONObject ankaVmInfo = jsonObject.getJSONObject("vminfo");
            this.vmInfo = new AnkaVmInfo(ankaVmInfo);
        }
        if (jsonObject.has("message")) {
            this.message = jsonObject.getString("message");
        }
        if (jsonObject.has("external_id")) {
            this.externalId = jsonObject.getString("external_id");
        }
    }

    public static AnkaVmInstance makeAnkaVmSessionFromJson(JSONObject jsonObject) {
        String instance_id = jsonObject.getString("instance_id");
        JSONObject vm = jsonObject.getJSONObject("vm");
        return new AnkaVmInstance(instance_id, vm);
    }

    @Override
    public String getName() {
        if (this.vmInfo != null) {
            return vmInfo.getName();
        }
        return this.name;
    }

    public String getTemplateId() { return templateId; }

    public String getExternalId() { return externalId; }

    public String getSessionState() {
        return sessionState;
    }

    public AnkaVmInfo getVmInfo() {
        return vmInfo;
    }

    public String getMessage() {
        return this.message;
    }

    public boolean isTerminatingOrTerminated() {
        switch (sessionState) {
            case "Terminated":
            case "Terminating":
                return true;
        }
        return false;
    }

    public boolean isStarted() {
        return sessionState.equalsIgnoreCase("Started");
    }

    public boolean isPulling() {
        return sessionState.equalsIgnoreCase("pulling");
    }

    public boolean isSchedulingOrPulling() {
        switch (sessionState) {
            case "Pulling":
            case "Scheduling":
                return true;
        }
        return false;
    }

    public boolean isScheduling() {
        return sessionState.equalsIgnoreCase("Scheduling");
    }

    public boolean isInError() {
        return sessionState.equalsIgnoreCase("error");
    }

    public boolean isPushing() {
        return sessionState.equalsIgnoreCase("pushing");
    }

}


