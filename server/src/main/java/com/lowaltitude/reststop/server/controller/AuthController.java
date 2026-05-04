package com.lowaltitude.reststop.server.controller;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.ApiResponse;
import com.lowaltitude.reststop.server.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<ApiDtos.AuthPayload> login(@Valid @RequestBody ApiDtos.LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<ApiDtos.AuthPayload> register(@Valid @RequestBody ApiDtos.RegisterRequest request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<ApiDtos.AuthPayload> refresh(@Valid @RequestBody ApiDtos.RefreshTokenRequest request) {
        return ApiResponse.success("刷新成功", authService.refresh(request));
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("服务正常", "ok");
    }
}
