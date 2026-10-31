package com.ems.sparkplug.handler;

import org.eclipse.tahu.protobuf.SparkplugBProto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ems.sparkplug.constant.SpbTopicConstant;
import com.ems.sparkplug.manager.SpbAliasCache;
import com.ems.sparkplug.model.SpbMetricDto;
import com.ems.sparkplug.server.TsdbService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SpbPayloadHandler {
    @Autowired
    private SpbAliasCache aliasCache;
    @Autowired
    private TsdbService tsdbService;


    public void handle(String groupId, String edgeNodeId, String deviceId, String msgType, SparkplugBProto.Payload payload) {
        switch (msgType) {
            case SpbTopicConstant.MSG_TYPE_NBIRTH:
            case SpbTopicConstant.MSG_TYPE_DBIRTH:
                handleBirth(groupId, edgeNodeId, deviceId, payload);
                break;
            case SpbTopicConstant.MSG_TYPE_DDATA:
                handleDdata(groupId, edgeNodeId, deviceId, payload);
                break;
            case SpbTopicConstant.MSG_TYPE_NDEATH:
            case SpbTopicConstant.MSG_TYPE_DDEATH:
                handleDeath(groupId, edgeNodeId, deviceId, payload);
                break;
            default:
                log.debug("暂不处理消息类型：{}", msgType);
        }
    }

    /**
     * Birth：缓存 alias -> metricName
     */
    private void handleBirth(String groupId, String edgeNodeId, String deviceId, SparkplugBProto.Payload payload) {
        log.info("收到Birth报文 group={}, edge={}, device={}", groupId, edgeNodeId, deviceId);
        aliasCache.clearDevice(groupId, edgeNodeId, deviceId);
        for (SparkplugBProto.Payload.Metric metric : payload.getMetricsList()) {
            Long alias = metric.hasAlias() ? metric.getAlias() : null;
            String name = metric.hasName() ? metric.getName() : null;
            if (alias != null && name != null) {
                aliasCache.put(groupId, edgeNodeId, deviceId, alias, name);
            }
            log.info("Birth metric -> alias:{}, name:{}", alias, name);
        }
    }

    /**
     * DDATA：解析测点，写入TD
     */
    private void handleDdata(String groupId, String edgeNodeId, String deviceId, SparkplugBProto.Payload payload) {
        long ts = payload.hasTimestamp() ? payload.getTimestamp() : System.currentTimeMillis();
        for (SparkplugBProto.Payload.Metric metric : payload.getMetricsList()) {
            SpbMetricDto dto = new SpbMetricDto();
            dto.setGroupId(groupId);
            dto.setEdgeNodeId(edgeNodeId);
            dto.setDeviceId(deviceId);
            dto.setTs(ts);
            // 质量码  暂时质量码字段
            // int quality = metric.hasQuality() ? metric.getQuality().getQuality() : 0;
            int quality = 0;
            dto.setQuality(quality);

            // 优先name，没有就用alias查缓存
            String metricName = metric.hasName() ? metric.getName() : null;
            if (metricName == null && metric.hasAlias()) {
                metricName = aliasCache.getMetricName(groupId, edgeNodeId, deviceId, metric.getAlias());
            }
            if (metricName == null) {
                log.warn("DDATA测点无法解析名称，alias={}，跳过", metric.getAlias());
                continue;
            }
            dto.setMetricName(metricName);

            // 读取protobuf的oneof value
            Object val = readMetricValue(metric);
            dto.setValue(val);
            log.info("DDATA测点：name={}, value={}, ts={}, quality={}", dto.getMetricName(), val, dto.getTs(), dto.getQuality());

            // 写入TD
            if (val instanceof Double || val instanceof Float) {
                tsdbService.insertFloatMetric(groupId, edgeNodeId, deviceId, metricName, ts, ((Number) val).doubleValue(), quality);
            } else if (val instanceof Long || val instanceof Integer) {
                tsdbService.insertIntMetric(groupId, edgeNodeId, deviceId, metricName, ts, ((Number) val).longValue(), quality);
            } else if (val instanceof Boolean) {
                tsdbService.insertBoolMetric(groupId, edgeNodeId, deviceId, metricName, ts, (Boolean) val, quality);
            } else if (val instanceof String) {
                tsdbService.insertStringMetric(groupId, edgeNodeId, deviceId, metricName, ts, (String) val, quality);
            }
        }
    }

    /**
     * Death：清理alias缓存
     */
    private void handleDeath(String groupId, String edgeNodeId, String deviceId, SparkplugBProto.Payload payload) {
        log.warn("收到Death，设备离线 group={}, edge={}, device={}", groupId, edgeNodeId, deviceId);
        aliasCache.clearDevice(groupId, edgeNodeId, deviceId);
    }

    /**
     * 解析protobuf oneof 各种类型的值
     */
    private Object readMetricValue(SparkplugBProto.Payload.Metric metric) {
        if (metric.hasDoubleValue()) {
            return metric.getDoubleValue();
        } else if (metric.hasFloatValue()) {
            return metric.getFloatValue();
        } else if (metric.hasIntValue()) {
            return metric.getIntValue();
        } else if (metric.hasBooleanValue()) {
            return metric.getBooleanValue();
        } else if (metric.hasStringValue()) {
            return metric.getStringValue();
        }
        // 其他类型（bytes、uint等）你后续按需扩展
        return null;
    }
    
}
