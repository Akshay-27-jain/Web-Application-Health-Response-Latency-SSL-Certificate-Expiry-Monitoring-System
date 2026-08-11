package com.uptimepulse.infrastructure.cache;

import com.uptimepulse.application.port.CachePort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Primary
public class InMemoryCacheAdapter implements CachePort {

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    public void put(String key, Object value, long ttlSeconds) {
        cache.put(key, value);
    }

    @Override
    public Object get(String key) {
        return cache.get(key);
    }

    @Override
    public void evict(String key) {
        cache.remove(key);
    }
}
