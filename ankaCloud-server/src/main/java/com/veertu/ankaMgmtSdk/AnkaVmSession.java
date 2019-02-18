package com.veertu.ankaMgmtSdk;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss.nx");
        org.joda.time.format.DateTimeFormatter formatter = DateTimeFormat.forPattern("YYYY-MM-dd'T'HH:mm:ss.SSSSSSSSSZ");
        DateTime dt = formatter.parseDateTime(dateString);
        this.created = dt.toDate();
//        int i = dateString.lastIndexOf(':');
//        StringBuilder sb = new StringBuilder(dateString);
//        sb.deleteCharAt(i);
//        dateString = sb.toString();
//        LocalDateTime ldate = LocalDateTime.from(formatter.parse(dateString));
//        this.created = new Date(ldate.toLocalDate().toEpochDay());
        this.vmId = jsonObject.getString("vmid");
        if (jsonObject.has("vminfo")) {
            JSONObject ankaVmInfo = jsonObject.getJSONObject("vminfo");
            this.vmInfo = new AnkaVmInfo(ankaVmInfo);
        }
    }

    public static AnkaVmSession makeAnkaVmSessionFromJson(JSONObject jsonObject) {
        String instance_id = jsonObject.getString("instance_id");
        JSONObject vm = jsonObject.getJSONObject("vm");
        return new AnkaVmSession(instance_id, vm);
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


