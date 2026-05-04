package com.lowaltitude.reststop.server.common;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * <p>
 * 统一捕获并处理控制器层抛出的各类异常，包括业务异常（BizException）、
 * 参数校验异常（MethodArgumentNotValidException）及其他未知异常，
 * 将其转换为统一的ApiResponse格式返回给前端。
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException exception) {
        return new ApiResponse<>(exception.getCode(), exception.getMessage(), null, System.currentTimeMillis());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ":" + error.getDefaultMessage())
                .orElse("请求参数错误");
        return new ApiResponse<>(400, message, null, System.currentTimeMillis());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception exception) {
        return new ApiResponse<>(500, exception.getMessage(), null, System.currentTimeMillis());
    }
}
