package com.example.low_altitudereststop.core.network;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

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
            return "{\"code\":200, \"message\":\"成功\", \"data\":{\"id\":1, \"username\":\"陈伶\", \"role\":\"PILOT\"}}";
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
                    "{\"id\":1, \"orderNo\":\"ORD20260422001\", \"taskId\":1, \"amount\":5000, \"status\":\"待支付\"}," +
                    "{\"id\":2, \"orderNo\":\"ORD20260422002\", \"taskId\":2, \"amount\":8000, \"status\":\"已支付\"}" +
                    "]}";
        }
        if (path.matches(".*orders/\\d+$")) {
            return "{\"code\":200, \"message\":\"成功\", \"data\":{" +
                    "\"id\":1, \"orderNo\":\"ORD20260422001\", \"taskId\":1, \"amount\":5000, \"status\":\"待支付\", " +
                    "\"taskTitle\":\"高压线巡检任务\", \"taskType\":\"巡检\", \"location\":\"重庆市江北区\", " +
                    "\"pilotName\":\"陈伶\", \"enterpriseName\":\"李四企业\", \"contactName\":\"李四\", \"contactPhone\":\"13800138000\", " +
                    "\"paymentStatus\":\"未支付\", \"paymentChannel\":\"无\", \"createdAt\":\"2026-04-22 10:00:00\"" +
                    "}}";
        }
        if (path.contains("compliance/no-fly-zones")) {
            return "{\"code\":200, \"message\":\"成功\", \"data\":[" +
                    "{\"id\":1, \"name\":\"江北机场禁飞区\", \"centerLat\":29.7196, \"centerLng\":106.6416, \"radius\":5000, \"level\":\"红区\"}," +
                    "{\"id\":2, \"name\":\"渝中区重要机关禁飞区\", \"centerLat\":29.5583, \"centerLng\":106.5694, \"radius\":2000, \"level\":\"红区\"}" +
                    "]}";
        }
        if (path.contains("training/courses/manage") || path.contains("training/courses")) {
            if (path.matches(".*courses/\\d+$") && !path.contains("manage")) {
                return "{\"code\":200, \"message\":\"成功\", \"data\":{" +
                        "\"id\":1, \"title\":\"无人机高级飞行培训\", \"summary\":\"提升复杂环境下的飞行技巧\", \"content\":\"课程内容包括：\\n1. 夜航技巧\\n2. 复杂气象飞行\\n3. 应急处理\", " +
                        "\"learningMode\":\"线下报名\", \"institutionName\":\"低空驿站飞行学院\", \"seatTotal\":50, \"seatAvailable\":20, " +
                        "\"browseCount\":100, \"enrollCount\":30, \"price\":1999, \"status\":\"报名中\"" +
                        "}}";
            }
            return "{\"code\":200, \"message\":\"成功\", \"data\":[" +
                    "{\"id\":1, \"title\":\"无人机高级飞行培训\", \"summary\":\"提升复杂环境下的飞行技巧\", \"learningMode\":\"线下报名\", \"institutionName\":\"低空驿站飞行学院\", \"seatAvailable\":20, \"browseCount\":100, \"enrollCount\":30, \"status\":\"报名中\", \"price\":1999}," +
                    "{\"id\":2, \"title\":\"植保机操作入门\", \"summary\":\"农业植保机的基础操作与维护\", \"learningMode\":\"文章学习\", \"institutionName\":\"低空驿站培训中心\", \"seatAvailable\":999, \"browseCount\":500, \"enrollCount\":200, \"status\":\"报名中\", \"price\":0}" +
                    "]}";
        }
        if (path.contains("messages/conversations")) {
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
