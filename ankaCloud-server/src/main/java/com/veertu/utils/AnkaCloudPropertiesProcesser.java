package com.veertu.utils;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class AnkaCloudPropertiesProcesser implements PropertiesProcessor {

    @Override
    public Collection<InvalidProperty> process(Map<String, String> properties) {
        return new ArrayList<InvalidProperty>();
    }
}
