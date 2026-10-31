package com.ems.sparkplug.config;


import lombok.Data;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ems.sparkplug.listener.MqttMessageListener;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@ConfigurationProperties(prefix = "mqtt")
@Data
public class MqttClientConfig {

    private String broker;
    private String clientId;
    private String username;
    private String password;
    private String subscribeTopic;
    private int qos;
    private boolean automaticReconnect;
    private int keepAlive;

    @Autowired
    private MqttMessageListener mqttMessageListener;

    @Bean
    public MqttClient mqttClient() throws MqttException {
        // MemoryPersistence：内存持久化，重启丢失；生产可以换成MqttDefaultFilePersistence
        MemoryPersistence persistence = new MemoryPersistence();
        MqttClient mqttClient = new MqttClient(broker, clientId, persistence);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setCleanSession(true);
        options.setAutomaticReconnect(automaticReconnect);
        options.setKeepAliveInterval(keepAlive);
        // 连接超时，单位秒
        options.setConnectionTimeout(10);

        // 设置消息回调
        mqttClient.setCallback(mqttMessageListener);

        // 连接
        mqttClient.connect(options);
        // 订阅
        mqttClient.subscribe(subscribeTopic, qos);

        return mqttClient;
    }
    
}
