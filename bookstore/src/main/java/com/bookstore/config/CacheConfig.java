package com.bookstore.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // 用 JDK 原生序列化器 —— 对泛型/多态最友好，不会出现 LinkedHashMap → Result 的 ClassCastException
        JdkSerializationRedisSerializer valueSerializer = new JdkSerializationRedisSerializer();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .withCacheConfiguration("home",     config.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("book",     config.entryTtl(Duration.ofMinutes(60)))
                .withCacheConfiguration("shop",     config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("ranking",  config.entryTtl(Duration.ofMinutes(5)))
                .build();
    }
}
