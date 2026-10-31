package com.ems.sparkplug.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TsdbService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 从 jdbc:TAOS-RS://host:6041/cloud_db?... 中解析出的库名，缺省 cloud_db */
    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    public void insertFloatMetric(String groupId, String edgeNodeId, String deviceId, String metricName,
                                  long ts, Double valFloat, Integer quality) {
        String subTb = buildSubTable(groupId, edgeNodeId, deviceId, metricName);
        String sql = """
                INSERT INTO %s USING %s
                TAGS (?, ?, ?, ?)
                VALUES (?, ?, NULL, NULL, NULL, ?)
                """.formatted(subTb, stableName());
        try {
            jdbcTemplate.update(sql, groupId, edgeNodeId, deviceId, metricName, ts, valFloat, quality);
        } catch (Exception e) {
            log.error("TD浮点写入失败，测点：{}", metricName, e);
        }
    }

    public void insertIntMetric(String groupId, String edgeNodeId, String deviceId, String metricName,
                                long ts, Long valInt, Integer quality) {
        String subTb = buildSubTable(groupId, edgeNodeId, deviceId, metricName);
        String sql = """
                INSERT INTO %s USING %s
                TAGS (?, ?, ?, ?)
                VALUES (?, NULL, ?, NULL, NULL, ?)
                """.formatted(subTb, stableName());
        try {
            jdbcTemplate.update(sql, groupId, edgeNodeId, deviceId, metricName, ts, valInt, quality);
        } catch (Exception e) {
            log.error("TD整型写入失败，测点：{}", metricName, e);
        }
    }

    public void insertBoolMetric(String groupId, String edgeNodeId, String deviceId, String metricName,
                                 long ts, Boolean valBool, Integer quality) {
        String subTb = buildSubTable(groupId, edgeNodeId, deviceId, metricName);
        String sql = """
                INSERT INTO %s USING %s
                TAGS (?, ?, ?, ?)
                VALUES (?, NULL, NULL, ?, NULL, ?)
                """.formatted(subTb, stableName());
        try {
            jdbcTemplate.update(sql, groupId, edgeNodeId, deviceId, metricName, ts, valBool, quality);
        } catch (Exception e) {
            log.error("TD布尔写入失败，测点：{}", metricName, e);
        }
    }

    public void insertStringMetric(String groupId, String edgeNodeId, String deviceId, String metricName,
                                   long ts, String valStr, Integer quality) {
        String subTb = buildSubTable(groupId, edgeNodeId, deviceId, metricName);
        String sql = """
                INSERT INTO %s USING %s
                TAGS (?, ?, ?, ?)
                VALUES (?, NULL, NULL, NULL, ?, ?)
                """.formatted(subTb, stableName());
        try {
            jdbcTemplate.update(sql, groupId, edgeNodeId, deviceId, metricName, ts, valStr, quality);
        } catch (Exception e) {
            log.error("TD字符串写入失败，测点：{}", metricName, e);
        }
    }

    private String stableName() {
        return dbName() + ".sp_metric_data";
    }

    private String buildSubTable(String groupId, String edgeNodeId, String deviceId, String metricName) {
        String raw = "sub_" + groupId + "_" + edgeNodeId + "_" + deviceId + "_" + metricName;
        // 表名只能用字母数字下划线；测点名里的 / . - 等一律替换
        String safe = raw.replaceAll("[^A-Za-z0-9_]", "_");
        return dbName() + "." + safe;
    }

    private String dbName() {
        // jdbc:TAOS-RS://host:6041/cloud_db?...
        try {
            String afterHost = jdbcUrl.substring(jdbcUrl.indexOf("://") + 3);
            String path = afterHost.contains("?") ? afterHost.substring(0, afterHost.indexOf('?')) : afterHost;
            int slash = path.indexOf('/');
            if (slash >= 0 && slash < path.length() - 1) {
                return path.substring(slash + 1);
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "cloud_db";
    }
}
