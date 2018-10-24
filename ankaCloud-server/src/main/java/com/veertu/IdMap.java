package com.veertu;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IdMap {


    private static final ConcurrentHashMap<String, String> genIdToId = new ConcurrentHashMap<>();

    public static void setGenIdToId(String id, String realId) {
        synchronized (genIdToId) {
            genIdToId.put(id, realId);
        }
    }

    public static String getRealIdFromGenId(String genId) {
        synchronized (genIdToId) {
            return genIdToId.get(genId);
        }
    }

    public static void removeInstanceId(String instanceId) {
        synchronized (genIdToId) {
            for (ConcurrentHashMap.Entry<String, String> entry: genIdToId.entrySet()) {
                if (entry.getValue().equals(instanceId)) {
                    genIdToId.remove(entry.getKey());
                }

            }
        }
    }

}
