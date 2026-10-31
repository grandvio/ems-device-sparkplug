package com.ems.sparkplug.config;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Configuration
public class TdengineDataSourceConfig {

    @Bean(initMethod = "init")
    @ConfigurationProperties(prefix = "spring.datasource.druid")
    public DruidDataSource tdengineDataSource(
            @Value("${spring.datasource.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.url}") String url) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        return dataSource;
    }

    @Bean
    public ApplicationRunner tdengineConnectionLogger(DruidDataSource dataSource) {
        return args -> verifyConnection(dataSource);
    }

    private void verifyConnection(DruidDataSource dataSource) throws SQLException {
        String url = stripCredentials(dataSource.getUrl());
        String validationQuery = dataSource.getValidationQuery();
        if (validationQuery == null || validationQuery.isBlank()) {
            validationQuery = "SELECT 1";
        }
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(validationQuery);
            log.info("【TDengine连接成功】url={}", url);
        } catch (SQLException ex) {
            log.error("【TDengine连接失败】url={}", url, ex);
            throw ex;
        }
    }

    private String stripCredentials(String url) {
        int queryIndex = url.indexOf('?');
        return queryIndex >= 0 ? url.substring(0, queryIndex) : url;
    }
}
