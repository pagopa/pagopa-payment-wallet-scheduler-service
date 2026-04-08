package it.pagopa.wallet.scheduler.repositories

import java.time.Duration
import java.util.function.Function
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

/**
 * This class is a [ReactiveRedisTemplate] wrapper class, used to centralize commons
 * ReactiveRedisTemplate operations
 *
 * @param V the ReactiveRedisTemplate value type
 */
abstract class ReactiveRedisTemplateWrapper<V>(
    val reactiveRedisTemplate: ReactiveRedisTemplate<String, V>,
    private val keyspace: String,
    private val ttl: Duration
) {
    /**
     * Save key to hold the string value if key is absent (SET with NX).
     *
     * @param value the entity to be saved
     * @return a [Mono] emitting `true` if the key did not exist and was set, `false` otherwise
     */
    fun saveIfAbsent(value: V, customTtl: Duration): Mono<Boolean> {
        return reactiveRedisTemplate
            .opsForValue()
            .setIfAbsent("$keyspace:${getKeyFromEntity(value)}", value!!, customTtl)
    }

    /**
     * Get the Redis key from the input entity
     *
     * @param value the entity value from which retrieve the Redis key
     * @return the key associated to the input entity
     */
    protected abstract fun getKeyFromEntity(value: V): String

    /**
     * Delete the entity for the given key
     *
     * @param key the entity key to be deleted
     * @return a [Mono] emitting `true` if a key was removed, `false` otherwise
     */
    fun deleteById(key: String): Mono<Boolean> {
        return reactiveRedisTemplate
            .delete(compoundKeyWithKeyspace(key))
            .map<Boolean>(Function { deletedCount: Long? -> deletedCount!! > 0 })
    }

    private fun compoundKeyWithKeyspace(key: String): String {
        return "$keyspace:$key"
    }
}
