package it.pagopa.wallet.scheduler.repositories

import it.pagopa.wallet.scheduler.repositories.redis.ExclusiveLockDocument
import it.pagopa.wallet.scheduler.repositories.redis.ReactiveRedisTemplateWrapper
import org.springframework.data.redis.core.ReactiveRedisTemplate

/** Redis template wrapper instance for handling exclusive lock */
class ReactiveExclusiveLockDocumentWrapper(
    reactiveRedisTemplate: ReactiveRedisTemplate<String, ExclusiveLockDocument>,
    keyspace: String
) : ReactiveRedisTemplateWrapper<ExclusiveLockDocument>(reactiveRedisTemplate, keyspace) {

    override fun getKeyFromEntity(value: ExclusiveLockDocument): String {
        return value.id
    }
}
