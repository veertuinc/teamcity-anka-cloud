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



<c:set var="paramImageName" value="<%=AnkaConstants.IMAGE_NAME%>"/>
<tr>
    <th><label for="${paramImageName}">Image Name: <l:star/></label></th>
    <td><props:textProperty name="${paramImageName}" className="longField"/>
    </td>
</tr>

<c:set var="paramImageTag" value="<%=AnkaConstants.IMAGE_TAG%>"/>
<tr>
    <th><label for="${paramImageTag}">Image Tag: <l:star/></label></th>
    <td><props:textProperty name="${paramImageTag}" className="longField"/>
    </td>
</tr>

<c:set var="paramSshUser" value="<%=AnkaConstants.SSH_USER%>"/>
<tr>
    <th><label for="${paramSshUser}">SSH User: <l:star/></label></th>
    <td><props:textProperty name="${paramSshUser}" className="longField"/>
    </td>
</tr>

<c:set var="paramSshPassword" value="<%=AnkaConstants.SSH_PASSWORD%>"/>
<tr>
    <th><label for="${paramSshPassword}">SSH Password: <l:star/></label></th>
    <td><props:textProperty name="${paramSshPassword}" className="longField"/>
    </td>
</tr>

<c:set var="paramAgentPath" value="<%=AnkaConstants.AGENT_PATH%>"/>
<tr>
    <th><label for="${paramAgentPath}">Agent Path: <l:star/></label></th>
    <td><props:textProperty name="${paramAgentPath}" className="longField"/>
    </td>
</tr>
<c:set var="paramAgentPoolId" value="<%=AnkaConstants.AGENT_POOL_ID%>" />
<tr>
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
