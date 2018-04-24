package com.veertu;

import com.veertu.utils.AnkaConstants;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.agentPools.AgentPoolManager;
import jetbrains.buildServer.serverSide.agentPools.AgentPool;

import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AnkaEditProfileController extends BaseFormXmlController {

    private final PluginDescriptor pluginDescriptor;
    private final AgentPoolManager agentPoolManager;

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
        return modelAndView;
    }

    @Override
    protected void doPost(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Element xmlResponse) {

    }
}
