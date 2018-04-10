package com.veertu.ankaMgmtSdk;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * Created by asafgur on 17/05/2017.
 */
public class AnkaVmSession extends AnkaVMRepresentation {

    private final String sessionState;
    private final String vmId;
    private final Date created;
    private AnkaVmInfo vmInfo;

    public AnkaVmSession(String id, JSONObject jsonObject) {
        this.id = id;
        this.sessionState = jsonObject.getString("instance_state");
        String dateString = jsonObject.getString("cr_time");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-ddTHH:mm:ss.nz", Locale.ENGLISH);
        LocalDate ldate = LocalDate.parse(dateString, formatter);
        this.created = new Date(ldate.toEpochDay());
        this.vmId = jsonObject.getString("vmid");
        if (jsonObject.has("vminfo")) {
            JSONObject ankaVmInfo = jsonObject.getJSONObject("vminfo");
            this.vmInfo = new AnkaVmInfo(ankaVmInfo);
        }
    }


    public String getSessionState() {
        return sessionState;
    }

    public String getVmId() {
        return vmId;
    }

    public AnkaVmInfo getVmInfo() {
        return vmInfo;
    }

    public Date getCreated() {
        return created;
    }
}


