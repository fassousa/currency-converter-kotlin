package com.fintech.currencyconverter.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import java.math.BigDecimal

/**
 * Regression test for the BigDecimal ClassCastException in CurrencyApiGateway.
 *
 * Root cause: Spring's @Cacheable stores the BigDecimal return value of getRate() in Redis.
 * Without type metadata in the serialized JSON, Jackson deserializes numeric values as Double
 * on cache hit. The Spring CGLIB proxy then throws:
 *   ClassCastException: class java.lang.Double cannot be cast to class java.math.BigDecimal
 *
 * Fix: CacheConfig activates default typing (ObjectMapper.DefaultTyping.EVERYTHING) so that
 * every cached value carries its Java class name, allowing faithful round-trip deserialization.
 */
class CacheConfigSerializerTest {

    private val rawMapper = ObjectMapper()

    private fun typedSerializer(): GenericJackson2JsonRedisSerializer {
        val cacheMapper = rawMapper.copy().activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Any::class.java)
                .build(),
            ObjectMapper.DefaultTyping.EVERYTHING,
        )
        return GenericJackson2JsonRedisSerializer(cacheMapper)
    }

    @Test
    fun `BigDecimal round-trips through Redis serializer without ClassCastException`() {
        val serializer = typedSerializer()
        val original = BigDecimal("1.084700000000000")

        val bytes = serializer.serialize(original)
        val deserialized = serializer.deserialize(bytes)

        assertThat(deserialized).isInstanceOf(BigDecimal::class.java)
        assertThat(deserialized as BigDecimal).isEqualByComparingTo(original)
    }

    @Test
    fun `untyped serializer reproduces the original Double ClassCastException bug`() {
        // Demonstrates the original bug: without type info embedded in the JSON,
        // Jackson cannot infer BigDecimal from a plain numeric value and defaults to Double.
        val serializer = GenericJackson2JsonRedisSerializer(rawMapper)
        val original = BigDecimal("1.084700000000000")

        val bytes = serializer.serialize(original)
        val deserialized = serializer.deserialize(bytes)

        // A plain JSON number deserializes to Double — attempting to cast it to BigDecimal
        // inside the @Cacheable CGLIB proxy throws ClassCastException in production.
        // Note: use java.lang.Double::class.java (boxed), not Double::class.java (primitive).
        assertThat(deserialized).isNotInstanceOf(BigDecimal::class.java)
        assertThat(deserialized).isInstanceOf(java.lang.Double::class.java)
    }
}
