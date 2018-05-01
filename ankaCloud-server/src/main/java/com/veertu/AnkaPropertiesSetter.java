package com.veertu;

import java.util.Map;


/**
 * Created by Asaf Gur.
 */

public interface AnkaPropertiesSetter {

    public void setProperties(Map<String, String> properties) throws AnkaUnreachableInstanceException;

}
