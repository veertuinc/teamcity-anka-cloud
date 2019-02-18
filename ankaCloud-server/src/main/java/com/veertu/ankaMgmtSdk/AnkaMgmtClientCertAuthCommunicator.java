package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.AnkaUnAuthenticatedRequestException;
import com.veertu.ankaMgmtSdk.exceptions.AnkaUnauthorizedRequestException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;

public class AnkaMgmtClientCertAuthCommunicator extends AnkaMgmtCommunicator {

    protected ClientCertAuthenticator authenticator;

    public AnkaMgmtClientCertAuthCommunicator(String mgmtUrl, String clientCert, String clientCertKey) {
        super(mgmtUrl);
        this.authenticator = new ClientCertAuthenticator(clientCert, clientCertKey);
    }


    protected JSONObject doRequest(RequestMethod method, String url, JSONObject requestBody) throws IOException, AnkaMgmtException {
        int retry = 0;
        while (true){
            try {
                retry++;
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

                HttpRequestBase request;
                try {
                    switch (method) {
                        case POST:
                            HttpPost postRequest = new HttpPost(url);
                            request = setBody(postRequest, requestBody);
                            break;
                        case DELETE:
                            HttpDeleteWithBody delRequest = new HttpDeleteWithBody(url);
                            request = setBody(delRequest, requestBody);
                            break;
                        case GET:
                            request = new HttpGet(url);
                            break;
                        default:
                            request = new HttpGet(url);
                            break;
                    }

                    HttpResponse response = httpClient.execute(request);
                    int responseCode = response.getStatusLine().getStatusCode();
                    if (responseCode == 401) {
                        throw new AnkaUnAuthenticatedRequestException("Authentication Required");
                    }
                    if (responseCode == 403) {
                        throw new AnkaUnauthorizedRequestException("Not authorized to perform this request");
                    }

                    if (responseCode != 200) {
                        LOG.error(String.format("url: %s response: %s", url, response.toString()));
                        return null;
                    }
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        BufferedReader rd = new BufferedReader(
                                new InputStreamReader(entity.getContent()));
                        StringBuffer result = new StringBuffer();
                        String line = "";
                        while ((line = rd.readLine()) != null) {
                            result.append(line);
                        }
                        JSONObject jsonResponse = new JSONObject(result.toString());
                        return jsonResponse;
                    }

                } catch (HttpHostConnectException | ConnectTimeoutException e) {
                    throw new AnkaMgmtException(e);
                } catch (SSLException e) {
                    throw e;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new AnkaMgmtException(e);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new AnkaMgmtException(e);
                } finally {
                    httpClient.close();
                }
                return null;
            } catch (Exception e) {
                if (retry >= maxRetries) {
                    continue;
                }
                throw new AnkaMgmtException(e);
            }
        }

    }
}
