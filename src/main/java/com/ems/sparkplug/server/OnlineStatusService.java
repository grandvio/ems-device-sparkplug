package com.ems.sparkplug.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

/**
 * 把 Sparkplug 上下线写回 ems-admin 业务库。
 * 网关按 group_id + edge_node_id 匹配，设备按所属网关下的 device_code 匹配。
 */
@Slf4j
@Service
public class OnlineStatusService {

    private static final String GATEWAY_ONLINE = """
            UPDATE ems_gateway
            SET online_status = 'ONLINE', last_birth_at = ?, updated_at = ?
            WHERE group_id = ? AND edge_node_id = ? AND deleted = 0
            """;

    private static final String GATEWAY_OFFLINE = """
            UPDATE ems_gateway
            SET online_status = 'OFFLINE', last_death_at = ?, updated_at = ?
            WHERE group_id = ? AND edge_node_id = ? AND deleted = 0
            """;

    /** 边缘节点离线时，其下已上线设备一并离线，未激活设备保持原状 */
    private static final String DEVICES_OFFLINE_BY_GATEWAY = """
            UPDATE ems_device d
            JOIN ems_gateway g ON d.gateway_id = g.id AND g.deleted = 0
            SET d.status = 'OFFLINE', d.updated_at = ?
            WHERE g.group_id = ? AND g.edge_node_id = ? AND d.deleted = 0 AND d.status = 'ONLINE'
            """;

    /** 设备能上报 Birth，说明边缘节点会话已经在线；不覆盖 NBIRTH 写入的 last_birth_at */
    private static final String GATEWAY_TOUCH_ONLINE = """
            UPDATE ems_gateway
            SET online_status = 'ONLINE', updated_at = ?
            WHERE group_id = ? AND edge_node_id = ? AND deleted = 0 AND online_status <> 'ONLINE'
            """;

    private static final String DEVICE_ONLINE = """
            UPDATE ems_device d
            JOIN ems_gateway g ON d.gateway_id = g.id AND g.deleted = 0
            SET d.status = 'ONLINE', d.updated_at = ?
            WHERE g.group_id = ? AND g.edge_node_id = ? AND d.device_code = ? AND d.deleted = 0
            """;

    private static final String DEVICE_OFFLINE = """
            UPDATE ems_device d
            JOIN ems_gateway g ON d.gateway_id = g.id AND g.deleted = 0
            SET d.status = 'OFFLINE', d.updated_at = ?
            WHERE g.group_id = ? AND g.edge_node_id = ? AND d.device_code = ? AND d.deleted = 0
            """;

    @Autowired
    @Qualifier("mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbcTemplate;

    public void gatewayOnline(String groupId, String edgeNodeId, long epochMillis) {
        Timestamp now = timestamp(epochMillis);
        int rows = update("网关上线", GATEWAY_ONLINE, now, now, groupId, edgeNodeId);
        if (rows == 0) {
            log.warn("【在线状态】未找到网关，无法置为在线 group={}, edge={}", groupId, edgeNodeId);
        } else if (rows > 0) {
            log.info("【在线状态】网关上线 group={}, edge={}", groupId, edgeNodeId);
        }
    }

    public void gatewayOffline(String groupId, String edgeNodeId, long epochMillis) {
        Timestamp now = timestamp(epochMillis);
        int rows = update("网关离线", GATEWAY_OFFLINE, now, now, groupId, edgeNodeId);
        int devices = update("网关下设备离线", DEVICES_OFFLINE_BY_GATEWAY, now, groupId, edgeNodeId);
        if (rows == 0) {
            log.warn("【在线状态】未找到网关，无法置为离线 group={}, edge={}", groupId, edgeNodeId);
        } else if (rows > 0) {
            log.info("【在线状态】网关离线 group={}, edge={}, 连带离线设备={}", groupId, edgeNodeId, Math.max(devices, 0));
        }
    }

    public void deviceOnline(String groupId, String edgeNodeId, String deviceId, long epochMillis) {
        if (deviceId == null || deviceId.isBlank()) {
            log.warn("【在线状态】设备上线缺少 deviceId，group={}, edge={}", groupId, edgeNodeId);
            return;
        }
        Timestamp now = timestamp(epochMillis);
        update("网关随设备上线", GATEWAY_TOUCH_ONLINE, now, groupId, edgeNodeId);
        int rows = update("设备上线", DEVICE_ONLINE, now, groupId, edgeNodeId, deviceId);
        if (rows == 0) {
            log.warn("【在线状态】未找到设备，无法置为在线 group={}, edge={}, device={}", groupId, edgeNodeId, deviceId);
        } else if (rows > 0) {
            log.info("【在线状态】设备上线 group={}, edge={}, device={}", groupId, edgeNodeId, deviceId);
        }
    }

    public void deviceOffline(String groupId, String edgeNodeId, String deviceId, long epochMillis) {
        if (deviceId == null || deviceId.isBlank()) {
            log.warn("【在线状态】设备离线缺少 deviceId，group={}, edge={}", groupId, edgeNodeId);
            return;
        }
        int rows = update("设备离线", DEVICE_OFFLINE, timestamp(epochMillis), groupId, edgeNodeId, deviceId);
        if (rows == 0) {
            log.warn("【在线状态】未找到设备，无法置为离线 group={}, edge={}, device={}", groupId, edgeNodeId, deviceId);
        } else if (rows > 0) {
            log.info("【在线状态】设备离线 group={}, edge={}, device={}", groupId, edgeNodeId, deviceId);
        }
    }

    private int update(String action, String sql, Object... args) {
        try {
            return mysqlJdbcTemplate.update(sql, args);
        } catch (Exception ex) {
            log.error("【在线状态】{}写库失败", action, ex);
            return -1;
        }
    }

    private Timestamp timestamp(long epochMillis) {
        long millis = epochMillis > 0 ? epochMillis : System.currentTimeMillis();
        return new Timestamp(millis);
    }
}
