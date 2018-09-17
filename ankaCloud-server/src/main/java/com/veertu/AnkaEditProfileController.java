package com.veertu;

import com.veertu.ankaMgmtSdk.AnkaAPI;
import com.veertu.ankaMgmtSdk.AnkaVmTemplate;
import com.veertu.ankaMgmtSdk.NodeGroup;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
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
        AnkaAPI ankaApi = AnkaAPI.getInstance();
        String imageId = request.getParameter("imageId");
        String toGet = request.getParameter("get");
        try {
            if (imageId != null && toGet.equals("tags")) {
                List<String> tags = ankaApi.listTemplateTags(mgmtURL, imageId);
                xmlResponse.addContent(new JSONArray(tags).toString());
            } else if (toGet.equals("images")) {
                xmlResponse.addContent(templatesToJson(ankaApi.listTemplates(mgmtURL)));
            } else if (toGet.equals("groups")) {
                List<NodeGroup> nodeGroups = ankaApi.getNodeGroups(mgmtURL);
                xmlResponse.addContent(groupsToJson(nodeGroups));
            }
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
