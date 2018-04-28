<%@ page import="com.veertu.utils.AnkaConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include.jsp" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>
<jsp:useBean id="agentPools" scope="request" type="java.util.Collection<jetbrains.buildServer.serverSide.agentPools.AgentPool>"/>

<style>

.hidden {
    display: none;
}

</style>


<c:set var="paramUrl" value="<%=AnkaConstants.HOST_NAME%>"/>
<tr>
    <th><label for="${paramUrl}">Anka Cloud Host: <l:star/></label></th>
    <td><props:textProperty name="${paramUrl}" className="longField"/>
    </td>
</tr>

<c:set var="paramPort" value="<%=AnkaConstants.PORT%>"/>
<tr>
    <th><label for="${paramPort}">Port: <l:star/></label></th>
    <td><props:textProperty name="${paramPort}" className="longField"/>
    </td>
</tr>
<tr>
    <td>
        <forms:button id="getTemplatesBtn" >Check connection / Fetch parameter values</forms:button>
    </td>
</tr>

<c:set var="paramImageName" value="<%=AnkaConstants.IMAGE_ID%>"/>

<tr class="dialog hidden">
    <th><label for="${paramImageName}">Image Name: <l:star/></label></th>
    <td>
        <props:selectProperty id="imageSelect" name="${paramImageName}" className="longField">
                  <props:option value=""><c:out value="<Please select Image>"/></props:option>
        </props:selectProperty>
    </td>
</tr>



<c:set var="paramImageTag" value="<%=AnkaConstants.IMAGE_TAG%>"/>
<tr class="dialog hidden">
    <th><label for="${paramImageTag}">Image Tag: <l:star/></label></th>
    <td>
        <props:selectProperty id="tagSelect" name="${paramImageTag}" className="longField">
                  <props:option value=""><c:out value="<Please select Tag>"/></props:option>
        </props:selectProperty>
    </td>
</tr>

<c:set var="paramSshUser" value="<%=AnkaConstants.SSH_USER%>"/>
<tr class="dialog hidden">
    <th><label for="${paramSshUser}">SSH User: <l:star/></label></th>
    <td><props:textProperty name="${paramSshUser}" className="longField"/>
    </td>
</tr>

<c:set var="paramSshPassword" value="<%=AnkaConstants.SSH_PASSWORD%>"/>
<tr class="dialog hidden">
    <th><label for="${paramSshPassword}">SSH Password: <l:star/></label></th>
    <td><props:textProperty name="${paramSshPassword}" className="longField"/>
    </td>
</tr>

<c:set var="paramAgentPath" value="<%=AnkaConstants.AGENT_PATH%>"/>
<tr class="dialog hidden">
    <th><label for="${paramAgentPath}">Agent Path: <l:star/></label></th>
    <td><props:textProperty name="${paramAgentPath}" className="longField"/>
    </td>
</tr>

<c:set var="paramMaxInstances" value="<%=AnkaConstants.MAX_INSTANCES%>"/>
<tr class="dialog hidden">
    <th><label for="${paramMaxInstances}">Max Instances: <l:star/></label></th>
    <td><props:textProperty name="${paramMaxInstances}" className="longField"/>
    </td>
</tr>

<c:set var="paramAgentPoolId" value="<%=AnkaConstants.AGENT_POOL_ID%>" />
<tr class="dialog hidden">
    <th><label for="${paramAgentPoolId}">Agent pool:</label></th>
        <td>
            <props:selectProperty name="${paramAgentPoolId}" className="longField">
              <props:option value=""><c:out value="<Please select agent pool>"/></props:option>
                  <c:forEach var="ap" items="${agentPools}">
                    <props:option value="${ap.agentPoolId}"><c:out value="${ap.name}"/></props:option>
                  </c:forEach>
            </props:selectProperty>
            <span id="error_${paramAgentPoolId}" class="error"></span>
      </td>
</tr>


<script type="text/javascript">


    function getImages(e) {
        e.preventDefault();
        if (!BS) BS = {};
        // inspired by vmware plugin
        BS.ajaxRequest(
            "<c:url value="${pluginResourcePath}"/>"+"?get=images",
                {
                    parameters: BS.Clouds.Admin.CreateProfileForm.serializeParameters(),
                    onFailure: function (response) {
                            console.log(response);
                            // might want to show some error message
                    },
                    onSuccess: function (response) {
                        var xmlDoc = $j(response.responseXML);
                        var wrapper = xmlDoc.find( "response" );
                        templates = JSON.parse(wrapper.text());
                        var imageSelect = $j("#imageSelect");
                        imageSelect.empty();
                        for (var k = 0; k < templates.length; k++) {
                            var template = templates[k];
                            var newTemplate = $j('<option value="' + template.id + '">' + template.name + '</option>');

                            imageSelect.append(newTemplate);
                        }
                        getTags();
                        $j(".dialog").removeClass("hidden");
                    }
            });
     }

     function getTags() {
        if (!BS) BS = {};
                // inspired by vmware plugin
                BS.ajaxRequest(
                    "<c:url value="${pluginResourcePath}"/>"+"?get=tags",
                        {
                            parameters: BS.Clouds.Admin.CreateProfileForm.serializeParameters(),
                            onFailure: function (response) {
                                    console.log(response);
                                    // might want to show some error message
                            },
                            onSuccess: function (response) {
                                var xmlDoc = $j(response.responseXML);
                                var wrapper = xmlDoc.find( "response" );
                                var tags = JSON.parse(wrapper.text()).reverse();
                                var tagSelect = $j("#tagSelect");
                                tagSelect.empty();
                                for (var k = 0; k < tags.length; k++) {
                                    var tag = $j('<option value="' + tags[k] + '">' + tags[k] + '</option>');

                                    tagSelect.append(tag);
                                }


                            }
                    });

     }

         var connectBtn = $j("#getTemplatesBtn");
         connectBtn.on("click", getImages);
         var imageSelect = $j("#imageSelect");
         imageSelect.on("change", getTags);
         if (imageSelect.find("option").length > 1) {
            $j(".dialog").removeClass("hidden");

         }

</script>
