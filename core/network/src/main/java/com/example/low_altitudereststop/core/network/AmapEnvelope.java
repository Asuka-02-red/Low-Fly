package com.example.low_altitudereststop.core.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class AmapEnvelope {
    public String status;
    public String info;
    public JsonObject regeocode;
    public JsonArray lives;
    public JsonArray casts;
}
