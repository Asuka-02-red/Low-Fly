package com.example.low_altitudereststop.core.network;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * 模拟数据拦截器，在演示模式下拦截HTTP请求并返回本地构造的模拟JSON响应，
 * 用于无后端环境下的功能演示和开发调试。
 */
public class MockInterceptor implements Interceptor {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static volatile boolean enabled = true;

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (!enabled) {
            return chain.proceed(chain.request());
        }
        String path = chain.request().url().encodedPath();
        if (shouldBypassMock(path)) {
            return chain.proceed(chain.request());
        }
        String body = readRequestBody(chain.request().body());
        String jsonResponse = getMockJsonResponse(path, body);

        return new Response.Builder()
                .code(200)
                .message("OK")
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .body(ResponseBody.create(jsonResponse, JSON))
                .addHeader("content-type", "application/json")
                .build();
    }

    private boolean shouldBypassMock(String path) {
        return path.contains("admin/weather/realtime")
                || path.contains("qweather.com");
    }

    private String getMockJsonResponse(String path, String requestBody) {
        if (path.contains("auth/login") || path.contains("auth/register") || path.contains("auth/refresh")) {
            boolean isEnterprise = requestBody.contains("enterprise")
                    || requestBody.contains("ENTERPRISE")
                    || requestBody.contains("企业");
            String role = isEnterprise ? "ENTERPRISE" : "PILOT";
            String username = isEnterprise ? "企业测试账号" : "飞手测试账号";
            String realName = isEnterprise ? "企业调度员" : "陈伶";
            String companyName = isEnterprise ? "低空驿站企业中心" : "";
            String token = isEnterprise ? "mock_enterprise_token" : "mock_pilot_token";
            return "{\"code\":200, \"message\":\"成功\", \"data\":{\"token\":\"" + token + "\", \"refreshToken\":\"mock_refresh\", "
                    + "\"userInfo\":{\"id\":1,\"username\":\"" + username + "\",\"role\":\"" + role + "\",\"realName\":\"" + realName + "\",\"companyName\":\"" + companyName + "\"}}}";
        }
        if (path.contains("users/me")) {
            return "{\"code\":200, \"message\":\"成功\", \"data\":{\"id\":1, \"username\":\"飞手测试账号\", \"role\":\"PILOT\", \"realName\":\"陈伶\", \"companyName\":\"\"}}";
        }
        if (path.endsWith("tasks") || path.contains("tasks?")) {
            return "{\"code\":200, \"message\":\"成功\", \"data\":[" +
                    "{\"id\":1, \"title\":\"高压线巡检任务\", \"taskType\":\"巡检\", \"location\":\"重庆市江北区\", \"deadline\":\"2026-05-01\", \"status\":\"招募中\", \"budget\":5000}," +
                    "{\"id\":2, \"title\":\"园区全景测绘\", \"taskType\":\"测绘\", \"location\":\"重庆市渝北区\", \"deadline\":\"2026-05-10\", \"status\":\"进行中\", \"budget\":8000}" +
                    "]}";
        }
        if (path.matches(".*tasks/\\d+$")) {
            return "{\"code\":200, \"message\":\"成功\", \"data\":{" +
                    "\"id\":1, \"title\":\"高压线巡检任务\", \"taskType\":\"巡检\", \"location\":\"重庆市江北区\", " +
                    "\"deadline\":\"2026-05-01\", \"status\":\"招募中\", \"budget\":5000, " +
                    "\"description\":\"需要沿江北区高压线进行全线巡检，确保无障碍物。\", " +
                    "\"ownerName\":\"李四企业\", \"latitude\":29.56376, \"longitude\":106.55046" +
                    "}}";
        }
        if (path.endsWith("orders") || path.contains("orders?")) {
            return "{\"code\":200, \"message\":\"成功\", \"data\":[" +
                    "{\"id\":7001, \"orderNo\":\"ORD-20260423-001\", \"taskId\":101, \"amount\":2680, \"status\":\"待支付\"}," +
                    "{\"id\":7002, \"orderNo\":\"ORD-20260423-002\", \"taskId\":102, \"amount\":4200, \"status\":\"已支付\"}," +
                    "{\"id\":7003, \"orderNo\":\"ORD-20260423-003\", \"taskId\":103, \"amount\":3200, \"status\":\"待确认\"}," +
                    "{\"id\":7004, \"orderNo\":\"ORD-20260423-004\", \"taskId\":104, \"amount\":980, \"status\":\"待支付\"}" +
                    "]}";
        }
        if (path.contains("orders/pay") || path.contains("orders/") && path.contains("/pay")) {
            return "{\"code\":200, \"message\":\"成功\", \"data\":{\"paymentStatus\":\"已支付\", \"paymentChannel\":\"模拟支付\"}}";
        }
        if (path.matches(".*orders/\\d+$")) {
            return "{\"code\":200, \"message\":\"成功\", \"data\":{" +
                    "\"id\":7001, \"orderNo\":\"ORD-20260423-001\", \"taskId\":101, \"amount\":2680, \"status\":\"待支付\", " +
                    "\"taskTitle\":\"两江新区智慧园区日常巡检\", \"taskType\":\"巡检\", \"location\":\"重庆两江新区数字园区\", " +
                    "\"pilotName\":\"刘一飞\", \"enterpriseName\":\"云巡科技\", \"contactName\":\"苏调度\", \"contactPhone\":\"023-6718-9001\", " +
                    "\"paymentStatus\":\"待支付\", \"paymentChannel\":\"企业对公\", \"createdAt\":\"2026-04-23 06:00\", \"appointmentTime\":\"2026-04-23 22:00\", " +
                    "\"remark\":\"对应园区巡检项目首单，建议在起飞前 30 分钟完成支付确认。\"" +
                    "}}";
        }
        if (path.contains("compliance/no-fly-zones")) {
            return "{\"code\":200, \"message\":\"成功\", \"data\":[" +
                    "{\"id\":1, \"name\":\"江北机场禁飞区\", \"centerLat\":29.7196, \"centerLng\":106.6416, \"radius\":5000, \"level\":\"红区\"}," +
                    "{\"id\":2, \"name\":\"渝中区重要机关禁飞区\", \"centerLat\":29.5583, \"centerLng\":106.5694, \"radius\":2000, \"level\":\"红区\"}" +
                    "]}";
        }
        if (path.contains("training/courses/manage") && !path.contains("/enroll") && !path.contains("/publish")) {
            if (path.matches(".*manage/\\d+$")) {
                return "{\"code\":200, \"message\":\"成功\", \"data\":{" +
                        "\"id\":1, \"title\":\"无人机高级飞行培训\", \"summary\":\"提升复杂环境下的飞行技巧\", \"learningMode\":\"OFFLINE\", " +
                        "\"seatTotal\":50, \"seatAvailable\":20, \"browseCount\":100, \"enrollCount\":30, \"price\":1999, \"status\":\"OPEN\"" +
                        "}}";
            }
            return "{\"code\":200, \"message\":\"成功\", \"data\":[" +
                    "{\"id\":1, \"title\":\"无人机高级飞行培训\", \"summary\":\"提升复杂环境下的飞行技巧\", \"learningMode\":\"OFFLINE\", \"seatTotal\":50, \"seatAvailable\":20, \"browseCount\":100, \"enrollCount\":30, \"price\":1999, \"status\":\"OPEN\"}," +
                    "{\"id\":2, \"title\":\"植保机操作入门\", \"summary\":\"农业植保机的基础操作与维护\", \"learningMode\":\"ARTICLE\", \"seatTotal\":999, \"seatAvailable\":999, \"browseCount\":500, \"enrollCount\":200, \"price\":0, \"status\":\"OPEN\"}" +
                    "]}";
        }
        if (path.contains("training/courses")) {
            if (path.contains("/enroll")) {
                return "{\"code\":200, \"message\":\"成功\", \"data\":{\"enrollmentNo\":\"ENR20260500001\", \"status\":\"ENROLLED\"}}";
            }
            if (path.contains("/publish")) {
                return "{\"code\":200, \"message\":\"成功\", \"data\":null}";
            }
            if (path.matches(".*courses/\\d+$")) {
                return "{\"code\":200, \"message\":\"成功\", \"data\":{" +
                        "\"id\":1, \"title\":\"无人机高级飞行培训\", \"summary\":\"提升复杂环境下的飞行技巧\", \"content\":\"课程内容包括：\\n1. 夜航技巧\\n2. 复杂气象飞行\\n3. 应急处理\", " +
                        "\"learningMode\":\"OFFLINE\", \"institutionName\":\"低空驿站飞行学院\", \"seatTotal\":50, \"seatAvailable\":20, " +
                        "\"browseCount\":100, \"enrollCount\":30, \"price\":1999, \"status\":\"OPEN\", " +
                        "\"enrolled\":false, \"enrollmentNo\":null, \"enrollmentStatus\":null" +
                        "}}";
            }
            return "{\"code\":200, \"message\":\"成功\", \"data\":[" +
                    "{\"id\":1, \"title\":\"无人机高级飞行培训\", \"summary\":\"提升复杂环境下的飞行技巧\", \"learningMode\":\"OFFLINE\", \"institutionName\":\"低空驿站飞行学院\", \"seatAvailable\":20, \"browseCount\":100, \"enrollCount\":30, \"status\":\"OPEN\", \"price\":1999}," +
                    "{\"id\":2, \"title\":\"植保机操作入门\", \"summary\":\"农业植保机的基础操作与维护\", \"learningMode\":\"ARTICLE\", \"institutionName\":\"低空驿站培训中心\", \"seatAvailable\":999, \"browseCount\":500, \"enrollCount\":200, \"status\":\"OPEN\", \"price\":0}" +
                    "]}";
        }
        if (path.contains("messages/conversations")) {
            if (path.contains("/messages") && !path.endsWith("conversations")) {
                return "{\"code\":200, \"message\":\"成功\", \"data\":[]}";
            }
            return "{\"code\":200, \"message\":\"成功\", \"data\":[]}";
        }
        if (path.contains("message/readReceipt") || path.contains("messages/readReceipt")) {
            return "{\"code\":200, \"message\":\"成功\", \"data\":null}";
        }
        if (path.contains("compliance/flight-applications")) {
            return "{\"code\":200, \"message\":\"成功\", \"data\":[]}";
        }
        if (path.contains("feedback/tickets")) {
            return "{\"code\":200, \"message\":\"成功\", \"data\":[]}";
        }
        if (path.contains("risk/alerts")) {
            return "{\"code\":200, \"message\":\"成功\", \"data\":[]}";
        }
        if (path.contains("user/pilot") || path.contains("enterprise/")) {
            return "{\"code\":200, \"message\":\"成功\", \"data\":{}}";
        }
        // 默认通用成功返回
        return "{\"code\":200, \"message\":\"成功\", \"data\":null}";
    }

    private String readRequestBody(RequestBody body) throws IOException {
        if (body == null) {
            return "";
        }
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return buffer.readUtf8();
    }
}
