package com.veertu.ankaMgmtSdk.exceptions;

public class SaveImageRequestIdMissingException extends AnkaMgmtException {

    public SaveImageRequestIdMissingException(String reqId) {
        super(String.format("Information about save image req id %s is no longer available", reqId));
    }
}
