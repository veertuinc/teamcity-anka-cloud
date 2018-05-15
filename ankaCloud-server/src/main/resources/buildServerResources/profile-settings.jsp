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
    <th><label for="${paramUrl}">Controller Host: <l:star/></label></th>
    <td>
        <div>
            <props:textProperty name="${paramUrl}" id="hostName" className="longField"/>
        </div>
        <span class="error option-error option-error_${paramUrl}" id="error_${paramUrl}"></span>
    </td>
</tr>

<c:set var="paramPort" value="<%=AnkaConstants.PORT%>"/>
<tr>
    <th><label for="${paramPort}">Controller Port: <l:star/></label></th>
    <td>
        <div>
            <props:textProperty name="${paramPort}" id="hostPort" className="longField"/>
        </div>
        <span class="error option-error option-error_${paramPort}" id="error_${paramPort}"></span>
    </td>
</tr>

<tr class="dialog hidden">
    <th><label for="imageSelect">Image Name: <l:star/></label></th>
    <td>
        <select id="imageSelect" class="longField">
                  <option value="">Please select Image</option>
        </select>
    </td>
</tr>

<tr class="dialog hidden">
    <th><label for="tagSelect">Image Tag: <l:star/></label></th>
    <td>
        <select id="tagSelect" class="longField">
                  <option value="">Please select Tag</option>
        </select>
    </td>
</tr>

<c:set var="paramImageName" value="<%=AnkaConstants.IMAGE_NAME%>"/>
<tr class="hidden">
    <th><label for="${paramImageName}">Image Name</label></th>
    <td>
        <div>
            <props:textProperty className="disabled" name="${paramImageName}" id="imageNameInput" disabled="true" />
        </div>
        <span class="error option-error option-error_${paramImageId}" id="error_${paramImageId}"></span>
    </td>
</tr>

<c:set var="paramImageId" value="<%=AnkaConstants.IMAGE_ID%>"/>
<tr class="hidden">
    <th><label for="${paramImageId}">Image Id</label></th>
    <td>
        <props:textProperty name="${paramImageId}" id="imageIdInput" disabled="true" />
    </td>
</tr>

<c:set var="paramImageTag" value="<%=AnkaConstants.IMAGE_TAG%>"/>
<tr class="hidden">
    <th><label for="${paramImageTag}">Image Tag</label></th>
    <td>
        <div>
            <props:textProperty className="disabled" name="${paramImageTag}" id="imageTagInput" disabled="true" />
        </div>
        <span class="error option-error option-error_${paramImageTag}" id="error_${paramImageTag}"></span>
    </td>
</tr>


<c:set var="paramSshUser" value="<%=AnkaConstants.SSH_USER%>"/>
<tr class="dialog hidden">
    <th><label for="${paramSshUser}">SSH User: <l:star/></label></th>
    <td>
        <div>
            <props:textProperty name="${paramSshUser}" className="longField"/>
        </div>
        <span class="error option-error option-error_${paramSshUser}" id="error_${paramSshUser}"></span>
    </td>
</tr>

<c:set var="paramSshPassword" value="<%=AnkaConstants.SSH_PASSWORD%>"/>
<tr class="dialog hidden">
    <th><label for="${paramSshPassword}">SSH Password: <l:star/></label></th>
    <td>
        <div>
            <props:passwordProperty name="${paramSshPassword}" className="longField"/>
        </div>
        <span class="error option-error option-error_${paramSshPassword}" id="error_${paramSshPassword}"></span>
    </td>
</tr>

<c:set var="paramAgentPath" value="<%=AnkaConstants.AGENT_PATH%>"/>
<tr class="dialog hidden">
    <th><label for="${paramAgentPath}">Agent Path: <l:star/></label></th>
    <td>
        <div>
            <props:textProperty name="${paramAgentPath}" className="longField"/>
        </div>
        <span class="error option-error option-error_${paramAgentPath}" id="error_${paramAgentPath}"></span>
    </td>
</tr>

<c:set var="paramMaxInstances" value="<%=AnkaConstants.MAX_INSTANCES%>"/>
<tr class="dialog hidden">
    <th><label for="${paramMaxInstances}">Max Instances: </label></th>
    <td>
        <div>
            <props:textProperty name="${paramMaxInstances}" className="longField"/>
        </div>
        <span>Leave this empty for unlimited intances</span>
        <span class="error option-error option-error_${paramMaxInstances}" id="error_${paramMaxInstances}"></span>
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


    function getImages() {
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
                            if (template.id === $j("#imageIdInput").val()) {
                                newTemplate.prop("selected", true);
                            }
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
                    "<c:url value="${pluginResourcePath}"/>"+"?get=tags&imageId=" + $j("#imageSelect option:selected").val(),
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
                                tagSelect.append($j('<option value="">Latest</option>'));
                                for (var k = 0; k < tags.length; k++) {
                                    var tag = $j('<option value="' + tags[k] + '">' + tags[k] + '</option>');
                                    if (tags[k] ===  $j("#imageTagInput").val()) {
                                        tag.prop("selected", true);
                                    }
                                    tagSelect.append(tag);
                                }
                             updateInputs();
                            }
                    });

     }

     function updateInputs() {

        var imageId = $j("#imageSelect option:selected").val();
        var tag = $j("#tagSelect").val();
        if (imageId.length > 0) {
            $j("#imageNameInput").val($j("#imageSelect option:selected").text());
            $j("#imageIdInput").val(imageId);
            $j("#imageTagInput").val(tag);
        }
     }

         var portField = $j("#hostPort");
         var hostField = $j("#hostName");
         hostField.on("change", getImages);
         portField.on("change", getImages);
         var imageSelect = $j("#imageSelect");
         imageSelect.on("change", function() {
            getTags();
            updateInputs();
         });

         var tagSelect = $j("#tagSelect");
         tagSelect.on("change", updateInputs);


         if (hostField.val().length > 0 && portField.val().length > 0) {
            getImages();
         }

         $j(".disabled").on("data-attribute-change", function() {
            $j(".disabled").prop("disabled", true);

         });

         $j(".error").on("change",  function() {
            $j(".disabled").prop("disabled", true);
         });

</script>
