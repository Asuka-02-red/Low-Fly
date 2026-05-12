package com.lowaltitude.reststop.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 低空驿站服务端启动类。
 * <p>
 * Spring Boot 应用入口，通过 {@link SpringBootApplication} 注解
 * 启用自动配置、组件扫描和配置类加载，启动整个后端服务。
 * </p>
 */
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
