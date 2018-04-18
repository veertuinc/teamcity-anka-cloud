<%@ page import="com.veertu.utils.AnkaConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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