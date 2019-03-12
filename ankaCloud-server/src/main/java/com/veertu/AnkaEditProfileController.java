package com.veertu;

import com.veertu.ankaMgmtSdk.AnkaAPI;
import com.veertu.ankaMgmtSdk.AnkaVmTemplate;
import com.veertu.ankaMgmtSdk.AuthType;
import com.veertu.ankaMgmtSdk.NodeGroup;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.AnkaUnAuthenticatedRequestException;
import com.veertu.ankaMgmtSdk.exceptions.AnkaUnauthorizedRequestException;
import com.veertu.common.AnkaConstants;
import jetbrains.buildServer.BuildProject;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.controllers.BasePropertiesBean;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.agentPools.AgentPoolManager;
import jetbrains.buildServer.serverSide.agentPools.AgentPool;

import jetbrains.buildServer.serverSide.agentPools.AgentPoolUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Created by Asaf Gur.
 */

public class AnkaEditProfileController extends BaseFormXmlController {

    private final PluginDescriptor pluginDescriptor;
    private final AgentPoolManager agentPoolManager;
    private final String PROP_PREFIX = "prop:";

    public AnkaEditProfileController(@NotNull final SBuildServer server,
                                     @NotNull final PluginDescriptor pluginDescriptor,
                                     @NotNull final WebControllerManager manager,
                                     @NotNull final AgentPoolManager agentPoolManager){
        super(server);
        this.pluginDescriptor = pluginDescriptor;
        this.agentPoolManager = agentPoolManager;
        String pluginResourcesPath = pluginDescriptor.getPluginResourcesPath(AnkaConstants.PROFILE_SETTING_HTML);
        manager.registerController(pluginResourcesPath, this);
    }

    @Override
    protected ModelAndView doGet(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
        ModelAndView modelAndView = new ModelAndView(pluginDescriptor.getPluginResourcesPath(AnkaConstants.PROFILE_SETTINGS_JSP));
        final String projectId = request.getParameter("projectId");
        final List<AgentPool> pools = new ArrayList<>();
        if (!BuildProject.ROOT_PROJECT_ID.equals(projectId)){
            pools.add(AgentPoolUtil.DUMMY_PROJECT_POOL);
        }
        pools.addAll(agentPoolManager.getProjectOwnedAgentPools(projectId));
        modelAndView.getModel().put("agentPools", pools);
        modelAndView.getModel().put("pluginResourcePath", pluginDescriptor.getPluginResourcesPath(AnkaConstants.PROFILE_SETTING_HTML));
        return modelAndView;
    }

    @Override
    protected void doPost(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Element xmlResponse) {
        final BasePropertiesBean propsBean = new BasePropertiesBean(null);
        String mgmtURL = request.getParameter(PROP_PREFIX+ AnkaConstants.CONTROLLER_URL_NAME);
        if (mgmtURL == null) {
            response.setStatus(400);
            return;
        }
        try {
            AnkaAPI ankaApi;

            String authMethod = request.getParameter(PROP_PREFIX+ AnkaConstants.AUTH_METHOD);
            String clientCert = request.getParameter(PROP_PREFIX + AnkaConstants.CERT_STRING);
            String clientCertKey = request.getParameter(PROP_PREFIX + AnkaConstants.CERT_KEY_STRING);
            String clientId = request.getParameter(PROP_PREFIX + AnkaConstants.OIDC_CLIENT_ID);
            String clientSecret = request.getParameter(PROP_PREFIX + AnkaConstants.OIDC_CLIENT_SECRET);
            String skipTLSVerificationString = request.getParameter(PROP_PREFIX + AnkaConstants.SKIP_TLS_VERIFICATION);
            boolean skipTLSVerification = false;
            if (skipTLSVerificationString != null && skipTLSVerificationString.equals("true")) {
                skipTLSVerification = true;
            }
            String rootCA = null;
            String rootCAParam =  request.getParameter(PROP_PREFIX + AnkaConstants.ROOT_CA);
            if (!rootCAParam.isEmpty()) {
                rootCA = rootCAParam;
            }
            if (authMethod.equals("cert") && clientCert != null && !clientCert.isEmpty()) {
                ankaApi = new AnkaAPI(mgmtURL, skipTLSVerification, clientCert, clientCertKey, AuthType.CERTIFICATE, rootCA);
            } else if(authMethod.equals("oidc") && clientId != null && !clientId.isEmpty()) {
                ankaApi = new AnkaAPI(mgmtURL, skipTLSVerification, clientId, clientSecret, AuthType.OPENID_CONNECT, rootCA);
            } else {
                ankaApi = new AnkaAPI(mgmtURL,  skipTLSVerification, rootCA);
            }

            String imageId = request.getParameter("imageId");
            String toGet = request.getParameter("get");

            if (imageId != null && toGet.equals("tags")) {
                List<String> tags = ankaApi.listTemplateTags(imageId);
                xmlResponse.addContent(new JSONArray(tags).toString());
            } else if (toGet.equals("images")) {
                xmlResponse.addContent(templatesToJson(ankaApi.listTemplates()));
            } else if (toGet.equals("groups")) {
                List<NodeGroup> nodeGroups = ankaApi.getNodeGroups();
                xmlResponse.addContent(groupsToJson(nodeGroups));
            }
        } catch (AnkaUnAuthenticatedRequestException e) {
            response.setStatus(401);
        } catch (AnkaUnauthorizedRequestException e) {
            response.setStatus(403);
        } catch (AnkaMgmtException e) {
            response.setStatus(400);
        }


    }

    private Element templatesAsElements(List<AnkaVmTemplate> ankaVmTemplates) {
        Element element = new Element("templates");
        for (AnkaVmTemplate template : ankaVmTemplates) {
            Element templateElement = new Element("template");
            templateElement.setAttribute("id", template.getId());
            templateElement.setAttribute("name", template.getName());
            element.addContent(templateElement);
        }
        return element;
    }

    private String templatesToJson(List<AnkaVmTemplate> ankaVmTemplates) {
        JSONArray arr = new JSONArray();
        for (AnkaVmTemplate template : ankaVmTemplates) {
            JSONObject templateJson = new JSONObject();
            templateJson.put("id", template.getId());
            templateJson.put("name", template.getName());
            arr.put(templateJson);
        }
        return arr.toString();
    }

    private String groupsToJson(List<NodeGroup> nodeGroups) {
        JSONArray arr = new JSONArray();
        for (NodeGroup group : nodeGroups) {
            JSONObject templateJson = new JSONObject();
            templateJson.put("id", group.getId());
            templateJson.put("name", group.getName());
            arr.put(templateJson);
        }
        return arr.toString();
    }
}
