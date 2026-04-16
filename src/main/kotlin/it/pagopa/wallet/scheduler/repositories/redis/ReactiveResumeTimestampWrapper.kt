package it.pagopa.wallet.scheduler.repositories.redis

import org.springframework.data.redis.core.ReactiveRedisTemplate

/** Redis template wrapper instance for handling resume timestamps */
class ReactiveResumeTimestampWrapper(
    reactiveRedisTemplate: ReactiveRedisTemplate<String, ResumeTimestamp>,
    keyspace: String
) : ReactiveRedisTemplateWrapper<ResumeTimestamp>(reactiveRedisTemplate, keyspace) {

    override fun getKeyFromEntity(value: ResumeTimestamp): String {
        return value.id
    }
}
