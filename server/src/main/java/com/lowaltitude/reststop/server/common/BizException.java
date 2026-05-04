package com.lowaltitude.reststop.server.common;

/**
 * 业务异常类。
 * <p>
 * 用于在业务逻辑中抛出携带错误码和错误信息的运行时异常，
 * 由全局异常处理器捕获并转换为统一的API响应格式。
 * </p>
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
