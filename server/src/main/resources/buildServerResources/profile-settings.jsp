<%@ page import="com.veertu.common.AnkaConstants" %>
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
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean" />
<style>

.hidden {
    display: none;
}

</style>




<c:set var="paramUrl" value="<%=AnkaConstants.CONTROLLER_URL_NAME%>"/>
<tr>
    <th><label for="${paramUrl}">Controller URL: <l:star/></label></th>
    <td>
        <div>
            <props:textProperty name="${paramUrl}" id="controllerURL" className="longField"/>
        </div>
        <span class="controllerURLError error option-error option-error_${paramUrl}" id="error_${paramUrl}"></span>
    </td>
</tr>


<c:set var="paramSkipTLSVerification" value="<%=AnkaConstants.SKIP_TLS_VERIFICATION%>"/>
<tr>
    <th><label for="${paramSkipTLSVerification}">Skip TLS Verification (for self signed certificates): </label></th>
    <td>
        <div>
            <props:checkboxProperty name="${paramSkipTLSVerification}" />
        </div>
        <span class="error option-error option-error_${paramSkipTLSVerification}" id="error_${paramSkipTLSVerification}" uncheckedValue="false" ></span>
    </td>
</tr>

<c:set var="paramAuthMethod" value="<%=AnkaConstants.AUTH_METHOD%>" />
<tr class="auth-config hidden">
    <th><label for="${paramAuthMethod}">Authentication Method: </label></th>
        <td>
            <props:selectProperty name="${paramAuthMethod}" id="authMethodSelect" className="longField">
              <props:option value="cert"><c:out value="mTLS Client Certificate"/></props:option>
              <props:option value=""><c:out value="<Please select authentication method>"/></props:option>
              <props:option value="oidc"><c:out value="Openid Connect"/></props:option>
            </props:selectProperty>
            <span id="error_${paramAuthMethod}" class="error"></span>
      </td>
</tr>

<c:set var="rootCertKey" value="<%=AnkaConstants.ROOT_CA%>"/>
<c:set var="rootCert" value="<%=AnkaConstants.PROP_PREFIX + AnkaConstants.ROOT_CA%>"/>
<tr class="auth-config-cert ">
    <th><label for="${rootCert}">mTLS Root CA Certificate: </label></th>
    <td>
        <div>
            <props:textarea textAreaName="${rootCert}" value="${empty value ? propertiesBean.properties[rootCertKey] : value}" linkTitle="root certificate" cols="50" rows="10" name="${rootCert}"   expanded="false"/>
        </div>
        <span class="error option-error option-error_${rootCert}" id="error_${rootCert}"></span>
    </td>
</tr>

<c:set var="clientCertStringKey" value="<%=AnkaConstants.CERT_STRING%>"/>
<c:set var="clientCertString" value="<%=AnkaConstants.PROP_PREFIX + AnkaConstants.CERT_STRING%>"/>
<tr class="auth-config-cert ">
    <th><label for="${clientCertString}">mTLS Client Certificate: </label></th>
    <td>
        <div>
            <props:textarea textAreaName="${clientCertString}" value="${empty value ? propertiesBean.properties[clientCertStringKey] : value}" linkTitle="certificate" cols="50" rows="10" name="${clientCertString}" className="certs"  expanded="false"/>
        </div>
        <span class="error option-error option-error_${clientCertString}" id="error_${clientCertString}"></span>
    </td>
</tr>

<c:set var="clientCertKeyKey" value="<%=AnkaConstants.CERT_KEY_STRING%>"/>
<c:set var="clientCertKey" value="<%=AnkaConstants.PROP_PREFIX + AnkaConstants.CERT_KEY_STRING%>"/>
<tr class="auth-config-cert ">
    <th><label for="${clientCertKey}">mTLS Client Certificate Private key: </label></th>
    <td>
        <div>
            <props:textarea textAreaName="${clientCertKey}" value="${empty value ? propertiesBean.properties[clientCertKeyKey] : value}" linkTitle="key" cols="50" rows="10" name="${clientCertKey}" className="certs" expanded="false" />

        </div>
        <span class="error option-error option-error_${clientCertKey}" id="error_${clientCertKey}"></span>
    </td>
</tr>

<c:set var="oidcClientId" value="<%=AnkaConstants.OIDC_CLIENT_ID%>"/>
<tr class="auth-config-oidc hidden">
    <th><label for="${oidcClientId}">Openid Client ID</label></th>
    <td>
        <div>
            <props:textProperty className="longField oidcs" name="${oidcClientId}" id="oidcClientId" />
        </div>
        <span class="error option-error option-error_${paramOidcClientId}" id="error_${paramOidcClientId}"></span>
    </td>
</tr>

<c:set var="oidcClientSecret" value="<%=AnkaConstants.OIDC_CLIENT_SECRET%>"/>
<tr class="auth-config-oidc hidden">
    <th><label for="${oidcClientSecret}">Openid Client Secret</label></th>
    <td>
        <div>
            <props:textProperty className="longField oidcs" name="${oidcClientSecret}" id="oidcClientSecret" />
        </div>
        <span class="error option-error option-error_${paramOidcClientSecret}" id="error_${paramOidcClientSecret}"></span>
    </td>
</tr>

<tr class="dependentOnControllerConnection hidden">
    <th><label for="templateSelect">Template Name: <l:star/></label></th>
    <td>
        <select id="templateSelect" class="longField">
                  <option value="">Please select Template</option>
        </select>
    </td>
</tr>

<tr class="dependentOnControllerConnection hidden">
    <th><label for="tagSelect">Template Tag: <l:star/></label></th>
    <td>
        <select id="tagSelect" class="longField">
                  <option value="">Please select Tag</option>
        </select>
    </td>
</tr>

<tr id="nodeGroupTr" class="dependentOnControllerConnection hidden">
    <th><label for="groupSelect">Node Group (optional): </label></th>
    <td id="nodeGroupTd">
        <select id="groupSelect" class="longField">
                <option value="">Select Node Group</option>
        </select>
    </td>
</tr>

<c:set var="paramvCpu" value="<%=AnkaConstants.VCPU_COUNT%>"/>
<tr id="vCpuTr" class="dependentOnControllerConnection hidden">
    <th><label for="vCpuInput">vCPU Override (optional): </label></th>
    <td id="vCpuTd">
        <div>
            <props:textProperty name="${paramvCpu}" id="vCpuInput" className="longField" />
        </div>
        <span>This is used to set the vCPU count for the VM. If blank, the vCPU count will be the number of vCPUs set on the template. (only works when the template VM is stopped)</span>
        <span class="error option-error option-error_${paramvCpu}" id="error_${paramvCpu}"></span>
    </td>
</tr>

<c:set var="paramRam" value="<%=AnkaConstants.RAM_SIZE%>"/>
<tr id="ramTr" class="dependentOnControllerConnection hidden">
    <th><label for="ramInput">RAM Override (optional): </label></th>
    <td id="ramTd">
        <div>
            <props:textProperty name="${paramRam}" id="ramInput" className="longField" />
        </div>
        <span>This is used to set the RAM MB (1GB = 1024MB) for the VM. If blank, the RAM size will be the amount of RAM set on the template. (only works when the template VM is stopped)</span>
        <span class="error option-error option-error_${paramRam}" id="error_${paramRam}"></span>
    </td>
</tr>

<c:set var="paramtemplateName" value="<%=AnkaConstants.TEMPLATE_NAME%>"/>
<tr class="hidden">
    <th><label for="${paramtemplateName}">Template Name</label></th>
    <td>
        <div>
            <props:textProperty className="disabled" name="${paramtemplateName}" id="templateNameInput" disabled="true" />
        </div>
        <span class="error option-error option-error_${paramTemplateName}" id="error_${paramTemplateName}"></span>
    </td>
</tr>

<c:set var="paramTemplateId" value="<%=AnkaConstants.IMAGE_ID%>"/>
<tr class="hidden">
    <th><label for="${paramTemplateId}">Template Id</label></th>
    <td>
        <props:textProperty name="${paramTemplateId}" id="templateIdInput" disabled="true" />
    </td>
</tr>

<c:set var="paramtemplateTag" value="<%=AnkaConstants.TEMPLATE_TAG%>"/>
<tr class="hidden">
    <th><label for="${paramtemplateTag}">Template Tag</label></th>
    <td>
        <div>
            <props:textProperty className="disabled" name="${paramtemplateTag}" id="templateTagInput" disabled="true" />
        </div>
        <span class="error option-error option-error_${paramtemplateTag}" id="error_${paramtemplateTag}"></span>
    </td>
</tr>

<c:set var="paramVmNameTemplate" value="<%=AnkaConstants.VM_NAME_TEMPLATE%>"/>
<tr class="dependentOnControllerConnection hidden">
    <th><label for="${paramVmNameTemplate}">VM Name Template</label></th>
    <td>
        <div>
            <props:textProperty name="${paramVmNameTemplate}" id="vmNameTemplateInput" className="longField" />
        </div>
        <span>This is used to set the eventual VM name. By default, the VM and Agent is mgmtManaged-\$ts (ts is convereted to a timestamp). See <a href="https://docs.veertu.com/anka/anka-build-cloud/working-with-controller-and-api/#start-vm-instances" target="_blank">the API documentation and the Start VM's <i>name_template</i> parameter</a> for more information about other variables you can use. <b>Note: Agent names cannot be longer than 80 characters or they will not function properly.</b></span>
        <span class="error option-error option-error_${paramVmNameTemplate}" id="error_${paramVmNameTemplate}"></span>
    </td>
</tr>

<c:set var="paramGroupId" value="<%=AnkaConstants.GROUP_ID%>"/>
<tr class="hidden">
    <th><label for="${paramGroupId}">Node Group </label></th>
    <td>
        <div>
            <props:textProperty className="disabled" name="${paramGroupId}" id="groupIdInput" disabled="true" />
        </div>
        <span class="groupError error option-error option-error_${paramGroupId}" id="error_${paramGroupId}"></span>
    </td>
</tr>


<c:set var="paramSshUser" value="<%=AnkaConstants.SSH_USER%>"/>
<tr class="dependentOnControllerConnection hidden">
    <th><label for="${paramSshUser}">SSH User: <l:star/></label></th>
    <td>
        <div>
            <props:textProperty name="${paramSshUser}" className="longField"/>
        </div>
        <span class="error option-error option-error_${paramSshUser}" id="error_${paramSshUser}"></span>
    </td>
</tr>

<c:set var="paramSshPassword" value="<%=AnkaConstants.SSH_PASSWORD%>"/>
<tr class="dependentOnControllerConnection hidden">
    <th><label for="${paramSshPassword}">SSH Password: <l:star/></label></th>
    <td>
        <div>
            <props:passwordProperty name="${paramSshPassword}" className="longField"/>
        </div>
        <span class="error option-error option-error_${paramSshPassword}" id="error_${paramSshPassword}"></span>
    </td>
</tr>

<c:set var="paramSshForwardingPort" value="<%=AnkaConstants.SSH_FORWARDING_PORT%>"/>
<tr class="dependentOnControllerConnection hidden">
    <th><label for="${paramSshForwardingPort}">Forwarding SSH Port: </label></th>
    <td>
        <div>
            <props:textProperty name="${paramSshForwardingPort}" className="longField"/>
        </div>
        <span>Leave blank for 22. This is the port in the VM that's being forwarded (not the host level port)</span>
        <span class="error option-error option-error_${paramSshForwardingPort}" id="error_${paramSshForwardingPort}"></span>
    </td>
</tr>


<c:set var="paramAgentPath" value="<%=AnkaConstants.AGENT_PATH%>"/>
<tr class="dependentOnControllerConnection hidden">
    <th><label for="${paramAgentPath}">Agent Path: <l:star/></label></th>
    <td>
        <div>
            <props:textProperty name="${paramAgentPath}" className="longField"/>
        </div>
        <span>If the path does not exist in the VM, we will download the buildAgent from your Teamcity instance (Server Url) and install it for you.</span>
        <span class="error option-error option-error_${paramAgentPath}" id="error_${paramAgentPath}"></span>
    </td>
</tr>

<c:set var="paramMaxInstances" value="<%=AnkaConstants.MAX_INSTANCES%>"/>
<tr class="dependentOnControllerConnection hidden">
    <th><label for="${paramMaxInstances}">Max Instances: </label></th>
    <td>
        <div>
            <props:textProperty name="${paramMaxInstances}" className="longField"/>
        </div>
        <span>Leave this empty for unlimited instances</span>
        <span class="error option-error option-error_${paramMaxInstances}" id="error_${paramMaxInstances}"></span>
    </td>
</tr>


<c:set var="paramPriority" value="<%=AnkaConstants.PRIORITY%>"/>
<tr class="dependentOnControllerConnection hidden">
    <th><label for="${paramPriority}">Priority: </label></th>
    <td>
        <div>
            <props:textProperty name="${paramPriority}" className="longField"/>
        </div>
        <span>Set priority for this image, lower is more urgent. (Enterprise version)</span>

        <span class="error option-error option-error_${paramPriority}" id="error_${paramPriority}"></span>
    </td>
</tr>

<c:set var="paramAgentPoolId" value="<%=AnkaConstants.AGENT_POOL_ID%>" />
<tr class="dependentOnControllerConnection hidden">
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

    function showAuthMethods() {
        var method = $j("#authMethodSelect").val();
        let certConfig = $j(".auth-config-cert");
        let oidcConfig = $j(".auth-config-oidc");
        console.log('method: ', method);
        if (method === 'cert') {
             certConfig.removeClass("hidden");
             oidcConfig.addClass("hidden");
        } else if (method == "oidc") {
             certConfig.addClass("hidden");
             oidcConfig.removeClass("hidden");
        }
    }

    function showAuth() {
        //$j(".auth-config").removeClass("hidden");
        $j(".auth-config-cert").removeClass("hidden");
    }

    function getTemplates() {
        if (!BS) BS = {};
        // inspired by vmware plugin
        BS.ajaxRequest(
            "<c:url value="${pluginResourcePath}"/>"+"?get=templates",
            {
                parameters: BS.Clouds.Admin.CreateProfileForm.serializeParameters(),
                onFailure: function (response) {
                    $j(".controllerURLError").text('');
                    $j(".controllerURLError").append('Error: Controller communication cannot be established.');
                    $j(".submitButton").prop("disabled", true);
                    console.log(response);
                    if (response.status === 401) {
                        showAuth();
                    }
                    // might want to show some error message
                },
                onSuccess: function (response) {
                    var xmlDoc = $j(response.responseXML);
                    var wrapper = xmlDoc.find( "response" );
                    templates = JSON.parse(wrapper.text());
                    var templateSelect = $j("#templateSelect");
                    templateSelect.empty();
                    for (var k = 0; k < templates.length; k++) {
                        var template = templates[k];
                        var newTemplate = $j('<option value="' + template.id + '">' + template.name + '</option>');
                        if (template.id === $j("#templateIdInput").val()) {
                            newTemplate.prop("selected", true);
                        }
                        templateSelect.append(newTemplate);
                    }
                    getTags();
                    $j(".dependentOnControllerConnection").removeClass("hidden");
                    $j(".submitButton").prop("disabled", false);
                    $j(".controllerURLError").text('');
                }
            }
        )
     };

    function getTags() {
        if (!BS) BS = {};
        // inspired by vmware plugin
        BS.ajaxRequest(
            "<c:url value="${pluginResourcePath}"/>"+"?get=tags&templateId=" + $j("#templateSelect option:selected").val(),
            {
                parameters: BS.Clouds.Admin.CreateProfileForm.serializeParameters(),
                onFailure: function (response) {
                    console.log(response);
                    if (response.status === 401) {
                        showAuth();
                    }
                    // might want to show some error message
                },
                onSuccess: function (response) {
                    var xmlDoc = $j(response.responseXML);
                    var wrapper = xmlDoc.find( "response" );
                    var tags = JSON.parse(wrapper.text()).reverse();
                    var tagSelect = $j("#tagSelect");
                    tagSelect.empty();
                    if (tags[0] !== "No tags found in registry") {
                        tagSelect.append($j('<option value="">Latest</option>'));
                    }
                    for (var k = 0; k < tags.length; k++) {
                        var tag = $j('<option value="' + tags[k] + '">' + tags[k] + '</option>');
                        if (tags[k] ===  $j("#templateTagInput").val()) {
                            tag.prop("selected", true);
                        }
                        tagSelect.append(tag);
                    }
                    updateInputs();
                }
            }
        )
    };

    function getGroups() {
        if (!BS) BS = {};
        BS.ajaxRequest(
            "<c:url value="${pluginResourcePath}"/>"+"?get=groups",
            {
                parameters: BS.Clouds.Admin.CreateProfileForm.serializeParameters(),
                onFailure: function (response) {
                    console.log('getGroups onFailure', response);
                    if (response.code === 401) {
                        showAuth();
                    }
                    // might want to show some error message
                },
                onSuccess: function (response) {
                    var xmlDoc = $j(response.responseXML);
                    var wrapper = xmlDoc.find( "response" );
                    try {
                        var groups = JSON.parse(wrapper.text()).reverse();
                        $j("#groupSelect").removeClass("hidden");
                        $j("#nodeGroupTd span").remove();
                        var groupSelect = $j("#groupSelect");
                        groupSelect.empty();
                        groupSelect.append($j('<option value="">(none; target all nodes)</option>'));
                        for (var k = 0; k < groups.length; k++) {
                            var group = $j('<option value="' + groups[k].id + '">' + groups[k].name + '</option>');
                            if (groups[k].id ===  $j("#groupIdInput").val()) {
                                group.prop("selected", true);
                            }
                            groupSelect.append(group);
                        }
                    } catch (e) {
                        $j("#groupSelect").addClass("hidden");
                        $j("#nodeGroupTd").append($j('<span style="color: gray;">Requires Enterprise License.</span>'));
                    }
                }
            }
        )
    };


    function updateInputs() {
        var imageId = $j("#templateSelect option:selected").val();
        var tag = $j("#tagSelect").val();
        if (imageId.length > 0) {
            $j("#templateNameInput").val($j("#templateSelect option:selected").text());
            $j("#templateIdInput").val(imageId);
            $j("#templateTagInput").val(tag);
        }
    }

    var contUrl = $j("#controllerURL");
    var debounceTimeout;
    contUrl.on("change", function() {
        clearTimeout(debounceTimeout);
        debounceTimeout = setTimeout(function() {
            getTemplates();
            getGroups();
        }, 300); // Adjust the debounce delay as needed
    });

    var certs = $j(".certs");
    var oidcs = $j(".oidcs");
    certs.on("change", function() {
        clearTimeout(debounceTimeout);
        debounceTimeout = setTimeout(function() {
            getTemplates();
            getGroups();
        }, 300); // Adjust the debounce delay as needed
    });
    oidcs.on("change", function() {
        clearTimeout(debounceTimeout);
        debounceTimeout = setTimeout(function() {
            getTemplates();
            getGroups();
        }, 300); // Adjust the debounce delay as needed
    });

    var templateSelect = $j("#templateSelect");
    templateSelect.on("change", function() {
        getTags();
        updateInputs();
    });

    var tagSelect = $j("#tagSelect");
    tagSelect.on("change", updateInputs);

    var groupSelect = $j("#groupSelect");
    groupSelect.on("change", function() {
        var groupSelectValue = $j("#groupSelect").val();
        var groupIdInput = $j("#groupIdInput");
        groupIdInput.val(groupSelectValue);
    });

    var vCpuInput = $j("#vCpuInput");
    vCpuInput.on("change", function() {
        var vCpuValue = vCpuInput.val();
        if (isNaN(vCpuValue) || vCpuValue.indexOf('.') !== -1 || vCpuValue < 0) {
            vCpuInput.val('');
            $j("#error_${paramvCpu}").text('Please enter a valid integer for vCPU count.');
            $j(".submitButton").prop("disabled", true);
        } else {
            $j("#error_${paramvCpu}").text('');
            $j(".submitButton").prop("disabled", false);
        }
    });

    var ramInput = $j("#ramInput");
    ramInput.on("change", function() {
        var ramValue = ramInput.val();
        if (isNaN(ramValue) || ramValue.indexOf('.') !== -1 || ramValue < 0) {
            ramInput.val('');
            $j("#error_${paramRam}").text('Please enter a valid integer for RAM size.');
            $j(".submitButton").prop("disabled", true);
        } else {
            $j("#error_${paramRam}").text('');
            $j(".submitButton").prop("disabled", false);
        }
    });

    if (contUrl.val().length > 0) {
        getTemplates();
        getGroups();
    }

    $j(".disabled").on("data-attribute-change", function() {
        $j(".disabled").prop("disabled", true);
    });

    $j(".error").on("change",  function() {
        $j(".disabled").prop("disabled", true);
    });
    //var authSelect = $j("#authMethodSelect");
    //authSelect.on("change", showAuthMethods);
    //if (authSelect.val() != null && authSelect.val().length > 0) {
    //showAuthMethods();
    //showAuth();
    //}



</script>
