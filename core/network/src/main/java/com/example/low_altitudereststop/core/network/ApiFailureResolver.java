package com.example.low_altitudereststop.core.network;

/**
 * API失败信息解析器，将HTTP状态码和异常转换为用户可读的中文提示信息。
 */
public final class ApiFailureResolver {

    private ApiFailureResolver() {
    }

    public static String fromHttp(int code, String fallbackMessage) {
        if (code == 401) {
            return "登录状态已失效，请重新登录";
        }
        if (code == 403) {
            return "当前账号无权执行该操作";
        }
        if (code == 404) {
            return "请求的数据不存在或已被删除";
        }
        if (code >= 500) {
            return "服务暂时不可用，请稍后重试";
        }
        return fallbackMessage == null || fallbackMessage.trim().isEmpty() ? "请求失败，请稍后重试" : fallbackMessage;
    }

    public static String fromThrowable(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().trim().isEmpty()) {
            return "网络连接异常，请检查网络后重试";
        }
        return "网络异常：" + throwable.getMessage();
    }
}
