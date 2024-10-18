

<tr>
    <th><label>Properties</label></th>
        <td>
          <c:forEach var="ap" items="${propertiesBean.properties}">
                <div><c:out value="${ap.key}"/></div>
          </c:forEach>
      </td>
</tr>

<tr>
    <th><label>Properties Object</label></th>
        <td>
            <div><c:out value="${propertiesBean.properties}"/></div>
      </td>
</tr>