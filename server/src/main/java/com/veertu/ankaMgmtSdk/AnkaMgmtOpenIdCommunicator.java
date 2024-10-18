package com.veertu.ankaMgmtSdk;

import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpRequestBase;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.ClientException;

public class AnkaMgmtOpenIdCommunicator extends AnkaMgmtCommunicator {

    private final OpenIdConnectAuthenticator authenticator;

    public AnkaMgmtOpenIdCommunicator(String mgmtUrl, boolean skipTLSVerification, String client, String key, String rootCA) {
        super(mgmtUrl, skipTLSVerification, rootCA);
        authenticator = new OpenIdConnectAuthenticator(mgmtUrl, client, key);
    }

    public AnkaMgmtOpenIdCommunicator(List<String> mgmtURLS, boolean skipTLSVerification, String client, String key, String rootCA) {
        super(mgmtURLS, skipTLSVerification, rootCA);
        authenticator = new OpenIdConnectAuthenticator(mgmtURLS.get(0), client, key);
    }

    private String sanitizeHeaderValue(String value) {
        return value.replaceAll("[\\r\\n]", "");
    }

    @Override
    protected void addHeaders(HttpRequestBase request) throws AnkaMgmtException, ClientException {
        NameValuePair authHeader = this.authenticator.getAuthorization();
        String sanitizedHeaderValue = sanitizeHeaderValue(authHeader.getValue());
        request.setHeader(authHeader.getName(), sanitizedHeaderValue);
    }
}
