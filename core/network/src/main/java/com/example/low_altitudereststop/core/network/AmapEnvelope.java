package com.example.low_altitudereststop.core.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * 高德地图API响应信封模型，封装地理编码和天气等接口的返回数据结构。
 */
public class AmapEnvelope {
    public String status;
    public String info;
    public JsonObject regeocode;
    public JsonArray lives;
    public JsonArray casts;
}
