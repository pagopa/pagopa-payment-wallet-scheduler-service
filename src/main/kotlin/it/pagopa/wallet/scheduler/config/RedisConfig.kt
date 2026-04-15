package it.pagopa.wallet.scheduler.config

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.pagopa.wallet.scheduler.config.properties.RedisJobLockPolicyConfig
import it.pagopa.wallet.scheduler.config.properties.RedisResumePolicyConfig
import it.pagopa.wallet.scheduler.repositories.redis.ExclusiveLockDocument
import it.pagopa.wallet.scheduler.repositories.redis.ReactiveExclusiveLockDocumentWrapper
import it.pagopa.wallet.scheduler.repositories.redis.ReactiveResumeTimestampWrapper
import it.pagopa.wallet.scheduler.repositories.redis.ResumeTimestamp
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig(
    private val redisJobLockPolicyConfig: RedisJobLockPolicyConfig,
    private val redisResumePolicyConfig: RedisResumePolicyConfig
) {

    @Bean
    fun resumeTimestampWrapper(
        reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory
    ): ReactiveResumeTimestampWrapper {
        val objectMapper =
            jacksonObjectMapper().apply {
                findAndRegisterModules()
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }

        // serializer
        val keySer = StringRedisSerializer()
        val valueSer = Jackson2JsonRedisSerializer(objectMapper, ResumeTimestamp::class.java)

        // serialization context
        val ctx =
            RedisSerializationContext.newSerializationContext<String, ResumeTimestamp>(keySer)
                .key(keySer)
                .value(valueSer)
                .hashKey(keySer)
                .hashValue(valueSer)
                .build()

        // reactive template
        val reactiveTemplate = ReactiveRedisTemplate(reactiveRedisConnectionFactory, ctx)

        return ReactiveResumeTimestampWrapper(reactiveTemplate, redisResumePolicyConfig.keyspace)
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
