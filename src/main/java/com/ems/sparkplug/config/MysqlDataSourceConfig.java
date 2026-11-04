package com.ems.sparkplug.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Configuration
public class MysqlDataSourceConfig {

    @Bean(name = "mysqlDataSource")
    public DataSource mysqlDataSource(
            @Value("${mysql.url}") String url,
            @Value("${mysql.username}") String username,
            @Value("${mysql.password}") String password) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("mysql-online");
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(0);
        // 业务库暂时不可用时，测点写入仍继续
        dataSource.setInitializationFailTimeout(-1);
        return dataSource;
    }

    @Bean(name = "mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate(@Qualifier("mysqlDataSource") DataSource mysqlDataSource) {
        return new JdbcTemplate(mysqlDataSource);
    }

    @Bean
    public ApplicationRunner mysqlConnectionLogger(@Qualifier("mysqlDataSource") DataSource mysqlDataSource,
                                                   @Value("${mysql.url}") String url) {
        return args -> {
            try (Connection connection = mysqlDataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
                log.info("【MySQL连接成功】url={}", stripCredentials(url));
            } catch (SQLException ex) {
                log.error("【MySQL连接失败】url={}", stripCredentials(url), ex);
            }
        };
    }

    private String stripCredentials(String url) {
        int queryIndex = url.indexOf('?');
        return queryIndex >= 0 ? url.substring(0, queryIndex) : url;
    }
}
