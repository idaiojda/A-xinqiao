package com.example.xinqiaobackend.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(ObjectProvider<RedisConnectionFactory> factoryProvider) {
        RedisConnectionFactory factory = factoryProvider.getIfAvailable();
        if (factory != null) {
            return RedisCacheManager.builder(factory).build();
        }
        return new ConcurrentMapCacheManager("posts");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(ObjectProvider<RedisConnectionFactory> factoryProvider) {
        RedisConnectionFactory factory = factoryProvider.getIfAvailable();
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        if (factory != null) {
            container.setConnectionFactory(factory);
        }
        return container;
    }
}
