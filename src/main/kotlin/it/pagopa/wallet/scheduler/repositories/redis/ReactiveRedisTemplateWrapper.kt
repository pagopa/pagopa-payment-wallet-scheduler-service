package it.pagopa.wallet.scheduler.repositories.redis

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
) {
    /**
     * Save the input entity into Redis.
     *
     * @param value the entity to be saved
     * @param ttl the TTL for the entity to be saved. This parameter overrides the default TTL value
     * @return a [Mono] emitting `true` if the key was set, `false` otherwise
     */
    fun save(value: V, ttl: Duration): Mono<Boolean> {
        return reactiveRedisTemplate
            .opsForValue()
            .set(compoundKeyWithKeyspace(getKeyFromEntity(value)), value!!, ttl)
    }

    /**
     * Save key to hold the string value if key is absent (SET with NX).
     *
     * @param value the entity to be saved
     * @return a [Mono] emitting `true` if the key did not exist and was set, `false` otherwise
     */
    fun saveIfAbsent(value: V, customTtl: Duration): Mono<Boolean> {
        return reactiveRedisTemplate
            .opsForValue()
            .setIfAbsent(compoundKeyWithKeyspace(getKeyFromEntity(value)), value!!, customTtl)
    }

    /**
     * Get the Redis key from the input entity
     *
     * @param value the entity value from which retrieve the Redis key
     * @return the key associated to the input entity
     */
    protected abstract fun getKeyFromEntity(value: V): String

    /**
     * Retrieve entity for the given key
     *
     * @param key the key of the entity to be found
     * @return a [Mono] emitting the value if present; empty if not found
     */
    fun findById(key: String): Mono<V> {
        return reactiveRedisTemplate.opsForValue()[compoundKeyWithKeyspace(key)]
    }

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
