package com.example.low_altitudereststop.core.network;

public class ApiEnvelope<T> {
    public int code;
    public String message;
    public T data;
    public long timestamp;
}

