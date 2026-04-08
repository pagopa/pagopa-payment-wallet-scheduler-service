package it.pagopa.wallet.scheduler.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import it.pagopa.wallet.documents.wallets.ExclusiveLockDocument
import it.pagopa.wallet.scheduler.config.properties.RedisJobLockPolicyConfig
import it.pagopa.wallet.scheduler.repositories.ReactiveExclusiveLockDocumentWrapper
import java.time.Instant
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig(private val redisJobLockPolicyConfig: RedisJobLockPolicyConfig) {

    @Bean
    fun reactiveRedisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory
    ): ReactiveRedisTemplate<String, Instant> {
        val objectMapper =
            ObjectMapper().apply {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }

        val keySerializer = StringRedisSerializer()
        val valueSerializer = Jackson2JsonRedisSerializer(objectMapper, Instant::class.java)

        val serializationContext =
            RedisSerializationContext.newSerializationContext<String, Instant>(keySerializer)
                .value(valueSerializer)
                .build()

        return ReactiveRedisTemplate(connectionFactory, serializationContext)
    }

    @Bean
    fun exclusiveLockDocumentWrapper(
        reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory
    ): ReactiveExclusiveLockDocumentWrapper {
        // serializer
        val keySer = StringRedisSerializer()
        val valueSer = Jackson2JsonRedisSerializer(ExclusiveLockDocument::class.java)

        // serialization context
        val ctx =
            RedisSerializationContext.newSerializationContext<String, ExclusiveLockDocument>(keySer)
                .key(keySer)
                .value(valueSer)
                .hashKey(keySer)
                .hashValue(valueSer)
                .build()

        // reactive template
        val reactiveTemplate = ReactiveRedisTemplate(reactiveRedisConnectionFactory, ctx)

        return ReactiveExclusiveLockDocumentWrapper(
            reactiveTemplate,
            redisJobLockPolicyConfig.keyspace
        )
    }
}
