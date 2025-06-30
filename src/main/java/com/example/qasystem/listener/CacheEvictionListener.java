package com.example.qasystem.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// 新增事件监听类
@Component
@RequiredArgsConstructor
public class CacheEvictionListener {
    private final RedisTemplate redisTemplate;

    @TransactionalEventListener
    public void handleTransactionCommit(CacheEvictionEvent event) {
        redisTemplate.delete(event.getCacheKeys());
    }
}

