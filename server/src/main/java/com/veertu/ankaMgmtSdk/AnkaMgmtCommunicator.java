package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.*;
import com.veertu.utils.RoundRobin;
import com.veertu.utils.MetadataKeys;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.*;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;

import static org.apache.http.conn.ssl.SSLConnectionSocketFactory.getDefaultHostnameVerifier;


/**
 * Created by asafgur on 09/05/2017.
 */
public class AnkaMgmtCommunicator {

    protected static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

    protected URL mgmtUrl;
    protected final int timeout = 30000;
    protected final int maxRetries = 10;
    protected boolean skipTLSVerification;
    protected String rootCA;
    protected transient RoundRobin roundRobin;
    protected int maxConnections = 50;

    protected int connectionKeepAliveSeconds = 120;
    protected transient CloseableHttpClient httpClient;
    private static int MinMaxConnections = 5;


    public AnkaMgmtCommunicator(String url) {
        try {
            URL tmpUrl = new URL(url);
            URIBuilder b = new URIBuilder();
            b.setScheme(tmpUrl.getProtocol());
            b.setHost( tmpUrl.getHost());
            b.setPort(tmpUrl.getPort());
            mgmtUrl = b.build().toURL();

        } catch (IOException | URISyntaxException e) {

            e.printStackTrace();
        }
    }

    public AnkaMgmtCommunicator(String mgmtURL, boolean skipTLSVerification) {
        this(mgmtURL);
        this.skipTLSVerification = skipTLSVerification;
    }

    public AnkaMgmtCommunicator(String mgmtUrl, String rootCA) {
        this(mgmtUrl);
        this.rootCA = rootCA;
    }

    public AnkaMgmtCommunicator(String mgmtUrl, boolean skipTLSVerification, String rootCA) {
        this(mgmtUrl);
        this.skipTLSVerification = skipTLSVerification;
        this.rootCA = rootCA;
    }

    public AnkaMgmtCommunicator(List<String> mgmtURLS, boolean skipTLSVerification, String rootCA) {
        this.roundRobin = new RoundRobin(mgmtURLS);
        this.skipTLSVerification = skipTLSVerification;
        this.rootCA = rootCA;
    }

    public int getMaxConections() {
        return maxConnections;
    }

    public void setMaxConections(int maxConections) {
        if (maxConections == 0) { // probably a misconfig
            maxConections = MinMaxConnections;
        }
        this.maxConnections = maxConections;
    }

    public int getConnectionKeepAliveSeconds() {
        return connectionKeepAliveSeconds;
    }

    public void setConnectionKeepAliveSeconds(int connectionKeepAliveSeconds) {
        this.connectionKeepAliveSeconds = connectionKeepAliveSeconds;
    } 

    public List<AnkaVmTemplate> listTemplates() throws AnkaMgmtException {
        List<AnkaVmTemplate> templates = new ArrayList<AnkaVmTemplate>();
        String url = "/api/v1/registry/vm";
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONArray vmsJson = jsonResponse.getJSONArray("body");
                for (Object j: vmsJson) {
                    JSONObject jsonObj = (JSONObject) j;
                    String vmId = jsonObj.getString("id");
                    String name = jsonObj.getString("name");
                    AnkaVmTemplate vm = new AnkaVmTemplate(vmId, name);
                    templates.add(vm);
                }
            }
        } catch (IOException e) {
            return templates;
        }
        return templates;
    }


    public List<String> getTemplateTags(String templateId) throws AnkaMgmtException {
        List<String> tags = new ArrayList<String>();
        String url = String.format("/api/v1/registry/vm?id=%s", templateId);
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONObject templateVm = jsonResponse.getJSONObject("body");
                JSONArray vmsJson = templateVm.getJSONArray("versions");
                for (Object j: vmsJson) {
                    JSONObject jsonObj = (JSONObject) j;
                    String tag = jsonObj.getString("tag");
                    tags.add(tag);
                }
            }
        } catch (IOException e) {
            LOG.error(String.format("Exception trying to access: '%s'", url));
        } catch (org.json.JSONException e) {
            LOG.error(String.format("Exception trying to parse response: '%s'", url));
        }
        return tags;
    }


    public List<NodeGroup> getNodeGroups() throws AnkaMgmtException {
        List<NodeGroup> groups = new ArrayList<>();
        String url = "/api/v1/group";
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResponse = jsonResponse.getString("status");
            if (logicalResponse.equals("OK")) {
                JSONArray groupsJson = jsonResponse.getJSONArray("body");
                for (int i = 0; i < groupsJson.length(); i++) {
                    JSONObject groupJsonObject = groupsJson.getJSONObject(i);
                    NodeGroup nodeGroup = new NodeGroup(groupJsonObject);
                    groups.add(nodeGroup);
                }
            } else {
                throw new AnkaMgmtException(jsonResponse.getString("message"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new AnkaMgmtException(e);
        } catch (JSONException e) {
            return groups;
        }
        return groups;
    }


    public String startVm(String templateId, String tag, String vmNameTemplate, String startUpScript, String groupId, int priority,
                          String name, String externalId) throws AnkaMgmtException {
        String url = "/api/v1/vm";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("vmid", templateId);
        if (tag != null)
            jsonObject.put("tag", tag);
        if (vmNameTemplate != null)
            jsonObject.put("name_template", vmNameTemplate);
        if (startUpScript != null) {
            String b64Script = Base64.encodeBase64String(startUpScript.getBytes());
            jsonObject.put("startup_script", b64Script);
        }
        if (groupId != null) {
            jsonObject.put("group_id", groupId);
        }
        if (priority > 0) {
            jsonObject.put("priority", priority);
        }
        if (name != null) {
            jsonObject.put("name", name);
        }
        if (externalId != null) {
            jsonObject.put("external_id", externalId);
        }
        JSONObject jsonResponse = null;
        try {
            jsonResponse = this.doRequest(RequestMethod.POST, url, jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        String logicalResult = jsonResponse.getString("status");
        if (logicalResult.equals("OK")) {
            JSONArray uuidsJson = jsonResponse.getJSONArray("body");
            if (uuidsJson.length() >= 1 ){
                return uuidsJson.getString(0);
            }
        }
        if (tag != null && !tag.isEmpty()) {
            String message = jsonResponse.getString("message");
            if (message.equals("No such tag "+ tag)) {
                LOG.warn("Tag " + tag + " not found. starting vm with latest tag");
                return startVm(templateId, null, vmNameTemplate, startUpScript, groupId, priority, name, externalId);
            }
        }

        throw new AnkaMgmtException(jsonResponse.getString("message"));
    }

    public AnkaVmInstance showVm(String sessionId) throws AnkaMgmtException {
        String url = String.format("/api/v1/vm?id=%s", sessionId);
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONObject body = jsonResponse.getJSONObject("body");
                return new AnkaVmInstance(sessionId, body);
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean terminateVm(String sessionId) throws AnkaMgmtException {
        String url = "/api/v1/vm";
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", sessionId);
            JSONObject jsonResponse = this.doRequest(RequestMethod.DELETE, url, jsonObject);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                return true;
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public List<AnkaVmInstance> list() throws AnkaMgmtException {
        List<AnkaVmInstance> vms = new ArrayList<>();
        String url = "/api/v1/vm";
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONArray vmsJson = jsonResponse.getJSONArray("body");
                for (int i = 0; i < vmsJson.length(); i++) {
                    JSONObject vmJson = vmsJson.getJSONObject(i);
                    String instanceId = vmJson.getString("instance_id");
                    JSONObject vm = vmJson.getJSONObject("vm");
                    vm.put("instance_id", instanceId);
                    vm.put("cr_time", vm.getString("cr_time"));
                    AnkaVmInstance ankaVmInstance = AnkaVmInstance.makeAnkaVmSessionFromJson(vmJson);
                    vms.add(ankaVmInstance);
                }
            }
            return vms;
        } catch (IOException e) {
            return vms;
        }
    }

    public AnkaCloudStatus status() throws AnkaMgmtException {
        String url = "/api/v1/status";
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url, 5000);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONObject statusJson = jsonResponse.getJSONObject("body");
                return AnkaCloudStatus.fromJson(statusJson);
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public String saveImage(String instanceId, String targetVMId, String newTemplateName, String tag,
                          String description, Boolean suspend, String shutdownScript,
                          Boolean revertBeforePush,
                          String revertTag,
                          Boolean doSuspendTest
    ) throws AnkaMgmtException {
        String url = "/api/v1/image";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", instanceId);
        if (targetVMId != null) {
            jsonObject.put("target_vm_id", targetVMId);
        }
        if (newTemplateName != null) {
            jsonObject.put("new_template_name", newTemplateName);
        }
        jsonObject.put("tag", tag);
        jsonObject.put("description", description);
        jsonObject.put("suspend", suspend);
        if (shutdownScript != null && !shutdownScript.isEmpty()) {
            String b64Script = Base64.encodeBase64String(shutdownScript.getBytes());
            jsonObject.put("script", b64Script);
        }
        if (revertBeforePush) {
            jsonObject.put("revert_before_push", true);
        }
        if (doSuspendTest) {
            jsonObject.put("do_suspend_sanity_test", true);
        }
        if (revertTag != null && !revertTag.isEmpty()) {
            jsonObject.put("revert_tag", revertTag);
        }
        JSONObject jsonResponse = null;
        try {
            jsonResponse = this.doRequest(RequestMethod.POST, url, jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (jsonResponse == null ) {
            throw new AnkaMgmtException("error sending save image request");
        }
        String logicalResult = jsonResponse.optString("status", "fail");
        if (!logicalResult.equals("OK")) {
            throw new AnkaMgmtException(jsonResponse.optString("message", "error saving image"));
        }

        JSONObject bd = jsonResponse.optJSONObject("body");
        return bd.optString("request_id", "");
    }

    public String getSaveImageStatus(String reqId) throws AnkaMgmtException {

        String url = String.format("/api/v1/image?id=%s", reqId);
        JSONObject jsonResponse = null;
        try {
            jsonResponse = this.doRequest(RequestMethod.GET, url);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AnkaMgmtException("error sending save image status request");
        }
        if (jsonResponse == null )
            throw new AnkaMgmtException("error while trying to get save image status");

        String logicalResult = jsonResponse.optString("status", "fail");
        if (!logicalResult.equals("OK")) {
            throw new AnkaMgmtException(jsonResponse.optString("message", "error saving image"));
        }
        try {
            JSONObject bd = jsonResponse.getJSONObject("body");
            if (bd == null) {
                throw new SaveImageRequestIdMissingException(reqId);
            }
            String status = bd.getString("status");
            return status;
        } catch (JSONException e) {
            e.printStackTrace();
            throw new AnkaMgmtException(e.getMessage());
        }
    }

    public void revertRegistryVM(String templateID) throws AnkaMgmtException {
        String url = String.format("/api/v1/registry/revert?id=%s", templateID);
        JSONObject jsonResponse = null;
        try {
            jsonResponse = this.doRequest(RequestMethod.DELETE, url);
        } catch (IOException e) {
            throw new AnkaMgmtException(e);
        }
        String logicalResult = jsonResponse.optString("status", "fail");
        if (!logicalResult.equals("OK")) {
            throw new AnkaMgmtException(jsonResponse.optString("message", "error reverting template " + templateID));
        }

    }

    public List<JSONObject> getImageRequests() throws AnkaMgmtException {
        String url = "/api/v1/image";
        List<JSONObject> imageRequests = new ArrayList<>();
        JSONObject jsonResponse = null;
        try {
            jsonResponse = this.doRequest(RequestMethod.GET, url);

        } catch (AnkaMgmtException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ClientException) {
                throw new AnkaNotFoundException("not found");
            }
        } catch (IOException e) {
            throw new AnkaMgmtException(e);
        }
        String logicalResult = jsonResponse.optString("status", "fail");
        if (!logicalResult.equals("OK")) {
            throw new AnkaMgmtException(jsonResponse.optString("message", "could not get image requests"));
        }
        JSONArray jsonArray = jsonResponse.optJSONArray("body");
        for (int i = 0; i < jsonArray.length(); i++) {
            imageRequests.add(jsonArray.getJSONObject(i));
        }
        return imageRequests;
    }

    public void updateVM(String id, String name, String jenkinsNodeLink, String jobIdentifier) throws AnkaMgmtException {
        String url = String.format("/api/v1/vm?id=%s", id);
        JSONObject jsonResponse = null;
        JSONObject jsonObject = new JSONObject();
        if (jenkinsNodeLink != null) {
            jsonObject.put("external_id", jenkinsNodeLink);
        }
        if (name != null) {
            jsonObject.put("name", name);
        }
        if (jobIdentifier != null && !jobIdentifier.equals("")) {
            HashMap<String, String> metaData = new HashMap<>();
            metaData.put(MetadataKeys.JobIdentifier, jobIdentifier);
            jsonObject.put("metadata", metaData);
        }
        try {
            jsonResponse = this.doRequest(RequestMethod.PUT, url, jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AnkaMgmtException(e);
        }
        String logicalResult = jsonResponse.getString("status");
        if (!logicalResult.equals("OK")) {
            throw new AnkaMgmtException(jsonResponse.optString("message"));
        }

    }

    public Boolean isEnterpriseLicense() throws AnkaMgmtException {
        String url = "/api/v1/status";
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONObject json = jsonResponse.getJSONObject("body");
                if (!json.getString("license").equals("basic")) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<AnkaNode> getNodes() throws AnkaMgmtException {
        List<AnkaNode> nodes = new ArrayList<>();
        String url = "/api/v1/node";
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONArray vmsJson = jsonResponse.getJSONArray("body");
                for (int i = 0; i < vmsJson.length(); i++) {
                    JSONObject nodeJson = vmsJson.getJSONObject(i);
                    AnkaNode ankaNode = AnkaNode.fromJson(nodeJson);
                    nodes.add(ankaNode);
                }
            }
            return nodes;
        } catch (IOException e) {
            return nodes;
        }
    }

    protected void addHeaders(HttpRequestBase request) throws AnkaMgmtException, ClientException {
        return;
    }

    protected enum RequestMethod {
        GET, POST, DELETE, PUT
    }

    protected JSONObject doRequest(RequestMethod method, String path, JSONObject requestBody) throws IOException, AnkaMgmtException {
        return doRequest(method, path, requestBody, timeout);
    }

    private JSONObject doRequest(RequestMethod method, String url) throws IOException, AnkaMgmtException {
        return doRequest(method, url, null, timeout);
    }

    private JSONObject doRequest(RequestMethod method, String url, int reqTimeout) throws IOException, AnkaMgmtException {
        return doRequest(method, url, null, reqTimeout);
    }



    protected JSONObject doRequest(RequestMethod method, String path, JSONObject requestBody, int reqTimeout) throws IOException, AnkaMgmtException {
        int retry = 0;
        CloseableHttpResponse response = null;
        HttpRequestBase request;

        while (true){
            try {
                retry++;

                CloseableHttpClient httpClient = getHttpClient();
                try {
                    String host = "";
                    if (roundRobin != null) {
                        host = roundRobin.next();
                    } else {
                        host = mgmtUrl.toString();
                    }

                    String url = host + path;
                    switch (method) {
                        case POST:
                            HttpPost postRequest = new HttpPost(url);
                            request = setBody(postRequest, requestBody);
                            break;
                        case PUT:
                            HttpPut putRequest = new HttpPut(url);
                            request = setBody(putRequest, requestBody);
                            break;
                        case DELETE:
                            if (requestBody != null) {
                                HttpDeleteWithBody delRequest = new HttpDeleteWithBody(url);
                                request = setBody(delRequest, requestBody);
                            } else {
                                request = new HttpDelete(url);
                            }
                            break;
                        case GET:
                            request = new HttpGet(url);
                            break;
                        default:
                            request = new HttpGet(url);
                            break;
                    }
                    request.setConfig(makeRequestConfig(reqTimeout));
                    this.addHeaders(request);
                    try {
                        long startTime = System.currentTimeMillis();
                        response = httpClient.execute(request);
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        if (roundRobin != null) {
                            roundRobin.update(host, (int) elapsedTime, false);
                        }
                    } catch (HttpHostConnectException | ConnectTimeoutException e) {
                        if (roundRobin != null) {
                            roundRobin.update(host, 0, true);
                        }
                        throw e;
                    }
                    int responseCode = response.getStatusLine().getStatusCode();
                    if (responseCode >= 400) {
                        throw new ClientException(request.getMethod() + " " + request.getURI().toString() + " " + "Bad Request");
                    }
                    if (responseCode == 401) {
                        throw new AnkaUnAuthenticatedRequestException("Authentication Required");
                    }
                    if (responseCode == 403) {
                        throw new AnkaUnauthorizedRequestException("Not authorized to perform this request");
                    }
                    if (responseCode != 200) {
                        LOG.info(String.format("url: %s response: %s", url, response.toString()));
                        return null;
                    }
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(entity.getContent()));
                        StringBuffer result = new StringBuffer();
                        String line = "";
                        while ((line = rd.readLine()) != null) {
                            result.append(line);
                        }
                        rd.close();
                        JSONObject jsonResponse = new JSONObject(result.toString());
                        return jsonResponse;
                    }

                } catch (ConnectionPoolTimeoutException e) {
                    throw e; // keep on retrying
                } catch (HttpHostConnectException | ConnectTimeoutException | ClientException | SSLException | NoRouteToHostException e) {
                    LOG.error(String.format("Got client exception: %s", e.getMessage()));
                    // don't retry on client exception, timeouts or host exceptions
                    throw e;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new AnkaMgmtException(e);
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }
                return null;
            } catch (HttpHostConnectException | ConnectTimeoutException | ClientException | SSLException | NoRouteToHostException e) {
                // don't retry on client exception
                LOG.error(String.format("Got exception: %s %s", e.getClass().getName(), e.getMessage()));

                throw new AnkaMgmtException(e);
            } catch (Exception e) {
                LOG.error(String.format("Got exception: %s %s", e.getClass().getName(), e.getMessage()));

                if (retry < maxRetries) {
                    continue;
                }

                throw new AnkaMgmtException(e);
            }
        }

    }

    protected CloseableHttpClient getHttpClient() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        synchronized (this) {
            if (httpClient == null) {
                httpClient = makeHttpClient();
            }
            return httpClient;
        }
    }

    protected RequestConfig makeRequestConfig(int reqTimeout) {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(reqTimeout);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(reqTimeout);
        requestBuilder.setSocketTimeout(reqTimeout);
        return requestBuilder.build();
    }

    protected CloseableHttpClient makeHttpClient() throws KeyStoreException, NoSuchAlgorithmException,
            KeyManagementException, CertificateException, IOException, UnrecoverableKeyException {

        RequestConfig defaultRequestConfig = makeRequestConfig(timeout);
        HttpClientBuilder builder = HttpClientBuilder.create();
        KeyStore keystore = this.getKeyStore();
        if (rootCA != null) {
            if (keystore == null) {
                keystore = KeyStore.getInstance("JKS");
                keystore.load(null);
            }
            PEMParser reader;
            BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();
            reader = new PEMParser(new StringReader(rootCA));
            X509CertificateHolder holder = (X509CertificateHolder)reader.readObject();
            Certificate certificate = new JcaX509CertificateConverter().setProvider(bouncyCastleProvider).getCertificate(holder);
            keystore.setCertificateEntry("rootCA", certificate);
        }

        SSLContext sslContext = this.getSSLContext(keystore);
        PoolingHttpClientConnectionManager cm = getConnectionManager(sslContext);
        builder.setConnectionManager(cm);
        builder.setSSLContext(sslContext);
        builder.disableAutomaticRetries();
        builder.setMaxConnTotal(maxConnections);
        builder.setMaxConnPerRoute(maxConnections);
        builder.setConnectionTimeToLive(connectionKeepAliveSeconds, TimeUnit.SECONDS);
        CloseableHttpClient httpClient = builder.setDefaultRequestConfig(defaultRequestConfig).build();
        return httpClient;
    }

    protected SSLContext getSSLContext(KeyStore keystore) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {
        return new SSLContextBuilder()
                .loadTrustMaterial(keystore, getTrustStrategy()).build();
    }

    protected KeyStore getKeyStore() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        return null;
    }

    private PoolingHttpClientConnectionManager getConnectionManager(SSLContext sslContext) {
        RegistryBuilder<ConnectionSocketFactory> reg = RegistryBuilder.
                <ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", new SSLConnectionSocketFactory(sslContext, getDefaultHostnameVerifier()));
        return new PoolingHttpClientConnectionManager(reg.build());
    }

    protected TrustStrategy getTrustStrategy() {
        if (skipTLSVerification) {
            return utils.strategyLambda();
        }
        return null;
    }

    protected HttpRequestBase setBody(HttpEntityEnclosingRequestBase request, JSONObject requestBody) throws UnsupportedEncodingException {
        request.setHeader("content-type", "application/json");
        StringEntity body = new StringEntity(requestBody.toString());
        request.setEntity(body);
        return request;
    }

    class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
        public static final String METHOD_NAME = "DELETE";

        public String getMethod() {
            return METHOD_NAME;
        }

        public HttpDeleteWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        public HttpDeleteWithBody(final URI uri) {
            super();
            setURI(uri);
        }

        public HttpDeleteWithBody() {
            super();
        }
    }


}
