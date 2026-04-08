package it.pagopa.wallet.scheduler.repositories

import it.pagopa.wallet.documents.wallets.ExclusiveLockDocument
import java.time.Duration
import org.springframework.data.redis.core.ReactiveRedisTemplate

/** Redis template wrapper instance for handling exclusive lock */
class ReactiveExclusiveLockDocumentWrapper(
    reactiveRedisTemplate: ReactiveRedisTemplate<String, ExclusiveLockDocument>,
    keyspace: String,
    ttl: Duration
) : ReactiveRedisTemplateWrapper<ExclusiveLockDocument>(reactiveRedisTemplate, keyspace, ttl) {

    override fun getKeyFromEntity(value: ExclusiveLockDocument): String {
        return value.id
    }
}
