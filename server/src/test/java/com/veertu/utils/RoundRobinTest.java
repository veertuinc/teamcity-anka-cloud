package com.veertu.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundRobinTest {

    @Test
    void singleUrlAlwaysReturned() {
        RoundRobin roundRobin = new RoundRobin(Arrays.asList("https://a.example"));

        for (int i = 0; i < 5; i++) {
            assertEquals("https://a.example", roundRobin.next());
        }
    }

    @Test
    void equalWeightsDistributeAcrossUrls() {
        RoundRobin roundRobin = new RoundRobin(Arrays.asList(
            "https://a.example",
            "https://b.example"
        ));

        Set<String> seen = new HashSet<>();
        // urlMap length is startWeight * number of endpoints when weights are equal
        int samples = RoundRobin.startWeight * 2;
        for (int i = 0; i < samples; i++) {
            seen.add(roundRobin.next());
        }

        assertTrue(seen.contains("https://a.example"));
        assertTrue(seen.contains("https://b.example"));
        assertEquals(2, seen.size());
    }

    @Test
    void updateWithFailureStillReturnsKnownUrl() {
        RoundRobin roundRobin = new RoundRobin(Arrays.asList(
            "https://a.example",
            "https://b.example"
        ));

        roundRobin.update("https://a.example", 0, true);

        String next = roundRobin.next();
        assertTrue(next.equals("https://a.example") || next.equals("https://b.example"));
    }
}
