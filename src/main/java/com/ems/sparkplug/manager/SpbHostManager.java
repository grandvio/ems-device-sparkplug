package com.ems.sparkplug.manager;

import org.eclipse.tahu.protobuf.SparkplugBProto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ems.sparkplug.handler.SpbPayloadHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SpbHostManager {
    @Autowired
    private SpbPayloadHandler spbPayloadHandler;

    public void handleMessage(String topic, byte[] payload) {
        try {
            // spBv1.0/<groupId>/<msgType>/<edgeNodeId>/<deviceId>
            String[] topicArr = topic.split("/");
            if (topicArr.length < 4) {
                log.warn("SPB主题格式非法：{}", topic);
                return;
            }
            String groupId = topicArr[1];
            String msgType = topicArr[2];
            String edgeNodeId = topicArr[3];
            String deviceId = topicArr.length >= 5 ? topicArr[4] : null;

            // 直接protobuf反序列化，只有Payload这一个对象
            SparkplugBProto.Payload pbPayload = SparkplugBProto.Payload.parseFrom(payload);
            log.info("收到SPB消息 type={}, group={}, edge={}, device={}", msgType, groupId, edgeNodeId, deviceId);

            spbPayloadHandler.handle(groupId, edgeNodeId, deviceId, msgType, pbPayload);

        } catch (Exception e) {
            log.error("SPB报文解析失败, topic={}", topic, e);
        }
    }
    
}
