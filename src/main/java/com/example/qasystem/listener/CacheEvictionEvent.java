package com.example.qasystem.listener;

import org.springframework.context.ApplicationEvent;

public class CacheEvictionEvent extends ApplicationEvent {
    private final String[] cacheKeys;

    public CacheEvictionEvent(String... cacheKeys) {
        super(CacheEvictionEvent.class);
        this.cacheKeys = cacheKeys;
    }

    public String[] getCacheKeys() {
        return cacheKeys;
    }
}