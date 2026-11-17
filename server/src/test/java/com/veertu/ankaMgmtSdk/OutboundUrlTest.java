package com.veertu.ankaMgmtSdk;

import java.net.URI;

import org.junit.jupiter.api.Test;

import com.veertu.ankaMgmtSdk.exceptions.ClientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboundUrlTest {

    @Test
    void allowsPrivateControllerUrl() throws ClientException {
        URI uri = OutboundUrl.require("http://10.1.2.3:8090/config/v1/auth");

        assertEquals("10.1.2.3", uri.getHost());
        assertEquals(8090, uri.getPort());
        assertEquals("/config/v1/auth", uri.getPath());
    }

    @Test
    void allowsLocalhostProvider() throws ClientException {
        URI uri = OutboundUrl.require("https://localhost/realms/anka/.well-known/openid-configuration");

        assertEquals("localhost", uri.getHost());
    }

    @Test
    void rejectsLinkLocalMetadataAddress() {
        assertThrows(ClientException.class,
                () -> OutboundUrl.require("http://169.254.169.254/latest/meta-data"));
    }

    @Test
    void rejectsDecimalEncodedMetadataAddress() {
        byte[] raw = new byte[] {(byte) 169, (byte) 254, (byte) 169, (byte) 254};
        long value = 0;
        for (byte part : raw) {
            value = (value << 8) | (part & 0xff);
        }

        String url = "http://" + value + "/latest/meta-data";
        assertThrows(ClientException.class, () -> OutboundUrl.require(url));
    }

    @Test
    void rejectsMetadataHostName() {
        assertThrows(ClientException.class,
                () -> OutboundUrl.require("http://metadata.google.internal/computeMetadata/v1/"));
    }

    @Test
    void rejectsAlibabaMetadataAddress() {
        assertThrows(ClientException.class,
                () -> OutboundUrl.require("http://100.100.100.200/latest/meta-data"));
    }

    @Test
    void rejectsLinkLocalAndAwsIpv6Metadata() {
        assertThrows(ClientException.class, () -> OutboundUrl.require("http://[fe80::1]/"));
        assertThrows(ClientException.class, () -> OutboundUrl.require("http://[fd00:ec2::254]/latest/"));
    }

    @Test
    void rejectsUserInfoAndNonHttpScheme() {
        assertThrows(ClientException.class,
                () -> OutboundUrl.require("https://user:secret@idp.example/token"));
        assertThrows(ClientException.class,
                () -> OutboundUrl.require("file:///etc/passwd"));
    }
}
