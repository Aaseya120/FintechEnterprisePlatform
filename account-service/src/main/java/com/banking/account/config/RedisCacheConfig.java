package com.banking.account.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    public static final String CACHE_ACCOUNTS = "accounts";
    public static final String CACHE_ACCOUNT_BALANCES = "accountBalances";
    public static final String CACHE_CUSTOMER_ACCOUNTS = "customerAccounts";

    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
                // Account metadata: 10 minutes TTL
                .withCacheConfiguration(CACHE_ACCOUNTS,
                        defaultCacheConfiguration().entryTtl(Duration.ofMinutes(10)))
                // Frequently polled balance: 30 seconds TTL (ElastiCache tier)
                .withCacheConfiguration(CACHE_ACCOUNT_BALANCES,
                        defaultCacheConfiguration().entryTtl(Duration.ofSeconds(30)))
                // Customer's list of accounts: 5 minutes TTL
                .withCacheConfiguration(CACHE_CUSTOMER_ACCOUNTS,
                        defaultCacheConfiguration().entryTtl(Duration.ofMinutes(5)));
    }
}
