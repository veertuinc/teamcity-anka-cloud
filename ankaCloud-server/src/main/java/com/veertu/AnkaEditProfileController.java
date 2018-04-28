package com.veertu;

import com.veertu.ankaMgmtSdk.AnkaAPI;
import com.veertu.ankaMgmtSdk.AnkaVmTemplate;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.utils.AnkaConstants;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.controllers.BasePropertiesBean;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.agentPools.AgentPoolManager;
import jetbrains.buildServer.serverSide.agentPools.AgentPool;

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
        String projectId = request.getParameter("projectId");
        List<AgentPool> pools = new ArrayList<>();
        Set<Integer> projectPools = agentPoolManager.getProjectPools(projectId);
        for (Integer poolId: projectPools) {
            pools.add(agentPoolManager.findAgentPoolById(poolId));
        }
        pools.addAll(agentPoolManager.getAllAgentPools());
        modelAndView.getModel().put("agentPools", pools);
        modelAndView.getModel().put("pluginResourcePath", pluginDescriptor.getPluginResourcesPath(AnkaConstants.PROFILE_SETTING_HTML));
        return modelAndView;
    }

    @Override
    protected void doPost(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Element xmlResponse) {
        final BasePropertiesBean propsBean = new BasePropertiesBean(null);
        String host = request.getParameter(PROP_PREFIX+ AnkaConstants.HOST_NAME);
        String port = request.getParameter(PROP_PREFIX + AnkaConstants.PORT);
        AnkaAPI ankaApi = AnkaAPI.getInstance();
        String imageId = request.getParameter(PROP_PREFIX + AnkaConstants.IMAGE_ID);
        String toGet = request.getParameter("get");
        try {
            if (imageId != null && toGet.equals("tags")) {
                List<String> tags = ankaApi.listTemplateTags(host, port, imageId);
                xmlResponse.addContent(new JSONArray(tags).toString());
            } else {
                xmlResponse.addContent(templatesToJson(ankaApi.listTemplates(host, port)));
            }
        } catch (AnkaMgmtException e) {
            // do something, like return error msg
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
}
