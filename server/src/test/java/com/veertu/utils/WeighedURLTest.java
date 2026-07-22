package com.veertu.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeighedURLTest {

    @Test
    void defaultsMatchRoundRobinStartWeight() {
        WeighedURL weighedURL = new WeighedURL("https://controller.example");

        assertEquals("https://controller.example", weighedURL.getUrl());
        assertEquals(RoundRobin.startWeight, weighedURL.getWeight());
        assertEquals(1, weighedURL.getLatency());
        assertFalse(weighedURL.isFailed());
    }

    @Test
    void settersUpdateState() {
        WeighedURL weighedURL = new WeighedURL("https://controller.example");

        weighedURL.setWeight(10);
        weighedURL.setLatency(25);
        weighedURL.setFailed(true);

        assertEquals(10, weighedURL.getWeight());
        assertEquals(25, weighedURL.getLatency());
        assertTrue(weighedURL.isFailed());
    }
}
