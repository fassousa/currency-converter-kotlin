package com.fintech.currencyconverter.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
@ConditionalOnProperty(name = ["spring.cache.type"], havingValue = "redis", matchIfMissing = false)
class CacheConfig {

    @Bean
    fun cacheManager(
        redisConnectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper,
    ): RedisCacheManager {
        // Copy the application ObjectMapper and enable default typing so that
        // numeric types like BigDecimal are stored with their Java type metadata.
        // Without this, Jackson deserializes every JSON number as Double on cache
        // hit, causing a ClassCastException inside the @Cacheable AOP proxy.
        val cacheMapper = objectMapper.copy().activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Any::class.java)
                .build(),
            ObjectMapper.DefaultTyping.EVERYTHING,
        )
        val serializer = GenericJackson2JsonRedisSerializer(cacheMapper)
        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(CACHE_TTL_HOURS))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()),
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer),
            )
            .disableCachingNullValues()

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration(
                "exchangeRates",
                defaultConfig.entryTtl(Duration.ofHours(CACHE_TTL_HOURS)),
            )
            .build()
    }

    companion object {
        private const val CACHE_TTL_HOURS = 24L
    }
}

