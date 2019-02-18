package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

import java.io.IOException;
import java.util.Date;

/**
 * Created by asafgur on 18/05/2017.
 */
public interface AnkaMgmtVm {

    String waitForBoot() throws InterruptedException, IOException, AnkaMgmtException;
    String getId();
    String getName();
    String getConnectionIp();
    String getState();
    int getConnectionPort();
    void terminate();
    boolean isRunning();
    String getInfo();
    Date getCreatedTime() throws AnkaMgmtException;
    String getImageId();

}
