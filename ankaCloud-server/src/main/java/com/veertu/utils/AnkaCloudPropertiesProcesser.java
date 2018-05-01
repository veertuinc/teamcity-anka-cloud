package com.veertu.utils;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;

import java.util.*;

public class AnkaCloudPropertiesProcesser implements PropertiesProcessor {

    @Override
    public Collection<InvalidProperty> process(Map<String, String> properties) {
        ArrayList<InvalidProperty> invalidProperties = new ArrayList<>();

        List<String> notEmpties = Arrays.asList(AnkaConstants.HOST_NAME, AnkaConstants.PORT, AnkaConstants.AGENT_PATH, AnkaConstants.IMAGE_ID
        , AnkaConstants.SSH_USER, AnkaConstants.SSH_PASSWORD);
        for (String key: notEmpties) {
            String value = properties.get(key);
            if (isNullOrEmpty(value)) {
                invalidProperties.add(new InvalidProperty(key, "Must not be empty"));
            }
        }
        String hostPort = properties.get(AnkaConstants.PORT);
        if (!isNullOrEmpty(hostPort) && !hasNumericValue(hostPort)) {
            invalidProperties.add(new InvalidProperty(AnkaConstants.PORT, "Must be a number"));
        }
        String maxInstances = properties.get(AnkaConstants.MAX_INSTANCES);
        if (!isNullOrEmpty(maxInstances) && !hasNumericValue(maxInstances)) {
            invalidProperties.add(new InvalidProperty(AnkaConstants.MAX_INSTANCES, "Must be a number"));
        }

        return invalidProperties;
    }

    private boolean hasNumericValue(String number) {
        return number.matches("^\\d+$");
    }

    private boolean isNullOrEmpty(String s) {
        if (s == null) {
            return true;
        }
        if (s.length() <= 0) {
            return true;
        }
        return false;
    }
}
