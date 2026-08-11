package com.uptimepulse.application.port;

public interface CachePort {
    void put(String key, Object value, long ttlSeconds);
    Object get(String key);
    void evict(String key);
}
