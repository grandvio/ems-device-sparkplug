package com.ems.sparkplug.manager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SpbAliasCache {
    private final Map<String, String> aliasMap = new ConcurrentHashMap<>();

    private String buildKey(String groupId, String edgeNodeId, String deviceId, Long alias) {
        return String.format("%s|%s|%s|%d", groupId, edgeNodeId, deviceId, alias);
    }

    public void put(String groupId, String edgeNodeId, String deviceId, Long alias, String metricName) {
        if (alias == null || metricName == null) return;
        String key = buildKey(groupId, edgeNodeId, deviceId, alias);
        aliasMap.put(key, metricName);
        log.info("alias缓存: alias={}, name={}", alias, metricName);
    }

    public String getMetricName(String groupId, String edgeNodeId, String deviceId, Long alias) {
        if (alias == null) return null;
        return aliasMap.get(buildKey(groupId, edgeNodeId, deviceId, alias));
    }

    public void clearDevice(String groupId, String edgeNodeId, String deviceId) {
        aliasMap.keySet().removeIf(k -> k.startsWith(groupId + "|" + edgeNodeId + "|" + deviceId + "|"));
    }
    
}
