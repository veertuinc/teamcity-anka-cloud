package com.veertu.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.veertu.common.AnkaConstants;

import jetbrains.buildServer.serverSide.InvalidProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnkaCloudPropertiesProcesserTest {

    private final AnkaCloudPropertiesProcesser processor = new AnkaCloudPropertiesProcesser();

    @Test
    void validPropertiesProduceNoErrors() {
        Collection<InvalidProperty> invalid = processor.process(validProperties());
        assertTrue(invalid.isEmpty());
    }

    @Test
    void missingRequiredFieldsAreReported() {
        Map<String, String> properties = new HashMap<>();
        Collection<InvalidProperty> invalid = processor.process(properties);

        assertEquals(5, invalid.size());
        assertTrue(propertyKeys(invalid).contains(AnkaConstants.CONTROLLER_URL_NAME));
        assertTrue(propertyKeys(invalid).contains(AnkaConstants.AGENT_PATH));
        assertTrue(propertyKeys(invalid).contains(AnkaConstants.IMAGE_ID));
        assertTrue(propertyKeys(invalid).contains(AnkaConstants.SSH_USER));
        assertTrue(propertyKeys(invalid).contains(AnkaConstants.SSH_PASSWORD));
    }

    @Test
    void nonNumericMaxInstancesIsInvalid() {
        Map<String, String> properties = validProperties();
        properties.put(AnkaConstants.MAX_INSTANCES, "many");

        Collection<InvalidProperty> invalid = processor.process(properties);

        assertEquals(1, invalid.size());
        assertEquals(AnkaConstants.MAX_INSTANCES, invalid.iterator().next().getPropertyName());
    }

    @Test
    void numericMaxInstancesIsAccepted() {
        Map<String, String> properties = validProperties();
        properties.put(AnkaConstants.MAX_INSTANCES, "3");

        assertTrue(processor.process(properties).isEmpty());
    }

    private static Map<String, String> validProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put(AnkaConstants.CONTROLLER_URL_NAME, "https://controller.example");
        properties.put(AnkaConstants.AGENT_PATH, "/Users/teamcity/buildAgent");
        properties.put(AnkaConstants.IMAGE_ID, "template-id");
        properties.put(AnkaConstants.SSH_USER, "anka");
        properties.put(AnkaConstants.SSH_PASSWORD, "secret");
        return properties;
    }

    private static Collection<String> propertyKeys(Collection<InvalidProperty> invalid) {
        return invalid.stream().map(InvalidProperty::getPropertyName).collect(Collectors.toSet());
    }
}
