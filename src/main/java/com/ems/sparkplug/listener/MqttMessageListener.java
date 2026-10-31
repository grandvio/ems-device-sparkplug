package com.ems.sparkplug.listener;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ems.sparkplug.manager.SpbHostManager;

@Slf4j
@Component
public class MqttMessageListener implements MqttCallbackExtended {
    
    @Autowired
    private SpbHostManager spbHostManager;

    /**
     * 连接完成。首次连接 reconnect=false，自动重连成功 reconnect=true
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
            log.info("【MQTT重连成功】broker={}", serverURI);
        } else {
            log.info("【MQTT连接成功】broker={}", serverURI);
        }
    }
    /**
     * 收到消息回调
     * 【当前阶段】只打印日志；后续这里把 topic + payload byte[] 交给 SpbHostManager
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        byte[] payload = message.getPayload();
        log.info("【MQTT收到消息】topic={}, payload长度={} bytes", topic, payload.length);
        // ========== 后面打通Tahu之后，在这里加一行转发代码 ==========
        spbHostManager.handleMessage(topic, payload);
    }

    /**
     * 消息发送完成（publish发布消息才会触发，我们现在只订阅，暂时用不上）
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    /**
     * 连接断开
     */
    @Override
    public void connectionLost(Throwable cause) {
        log.error("【MQTT连接断开】", cause);
    }
    
}
