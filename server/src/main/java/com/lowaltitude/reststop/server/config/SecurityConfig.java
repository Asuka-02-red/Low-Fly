package com.lowaltitude.reststop.server.config;

import com.lowaltitude.reststop.server.security.AuthTokenFilter;
import com.lowaltitude.reststop.server.security.RequestIdFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security安全配置类。
 * <p>
 * 定义HTTP安全过滤链，配置CSRF禁用、CORS默认策略、无状态会话管理、
 * 路径级权限控制（公开接口、管理员接口、需认证接口），
 * 并注册请求ID过滤器和JWT认证过滤器。
 * </p>
 */
@Configuration
public class SecurityConfig {

    // 从配置文件读取允许的跨域源
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthTokenFilter authTokenFilter, RequestIdFilter requestIdFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                // 替换默认CORS配置为自定义配置
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/admin/weather/realtime", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 自定义CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的跨域源（从配置文件读取）
        configuration.setAllowedOrigins(allowedOrigins);
        // 允许携带凭证（Cookie、Authorization头等）
        configuration.setAllowCredentials(true);
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // 允许的请求头（*表示允许所有）
        configuration.setAllowedHeaders(List.of("*"));
        // 暴露给前端的响应头（如果前端需要获取自定义响应头）
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Request-Id"));
        // 预检请求缓存时间（单位：秒，这里设置为1小时）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有接口应用此配置
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}