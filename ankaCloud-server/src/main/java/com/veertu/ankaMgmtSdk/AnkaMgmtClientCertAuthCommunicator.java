package com.veertu.ankaMgmtSdk;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

public class AnkaMgmtClientCertAuthCommunicator extends AnkaMgmtCommunicator {

    protected ClientCertAuthenticator authenticator;

    public AnkaMgmtClientCertAuthCommunicator(String mgmtUrl, String clientCert, String clientCertKey) {
        super(mgmtUrl);
        this.authenticator = new ClientCertAuthenticator(clientCert, clientCertKey);
    }



    protected CloseableHttpClient makeHttpClient() throws CertificateException, NoSuchAlgorithmException,
            KeyStoreException, IOException, UnrecoverableKeyException, KeyManagementException {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(timeout);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(timeout);
        HttpClientBuilder builder = HttpClientBuilder.create();

        KeyStore keyStore = this.authenticator.getKeyStore();
        // allow self-signed certs
        SSLContext sslContext = new SSLContextBuilder()
                .loadKeyMaterial(keyStore, authenticator.getPemPassword().toCharArray())
                .loadTrustMaterial(keyStore, (certificate, authType) -> true).build();
        builder.setSslcontext(sslContext);
        //builder.setSSLHostnameVerifier(new NoopHostnameVerifier());

        builder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER));
        CloseableHttpClient httpClient = builder.setDefaultRequestConfig(requestBuilder.build()).build();
        return httpClient;
    }
}
