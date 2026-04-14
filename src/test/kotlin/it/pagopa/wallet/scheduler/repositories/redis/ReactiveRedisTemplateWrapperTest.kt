package it.pagopa.wallet.scheduler.repositories.redis

import java.time.Duration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class ReactiveRedisTemplateWrapperTest {
    private lateinit var reactiveRedisTemplate: ReactiveRedisTemplate<String, String>
    private lateinit var valueOperations: ReactiveValueOperations<String, String>

    private lateinit var wrapper: TestRedisWrapper

    private val keyspace = "test-space"

    // concrete implementation of the abstract class for testing
    class TestRedisWrapper(template: ReactiveRedisTemplate<String, String>, keyspace: String) :
        ReactiveRedisTemplateWrapper<String>(template, keyspace) {
        override fun getKeyFromEntity(value: String): String {
            return "key-$value"
        }
    }

    @BeforeEach
    fun setUp() {
        reactiveRedisTemplate = mock()
        valueOperations = mock()

        whenever(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations)

        wrapper = TestRedisWrapper(reactiveRedisTemplate, keyspace)
    }

    @Test
    fun `save should prepend keyspace and return true on success`() {
        // Arrange
        val entity = "myEntity"
        val ttl = Duration.ofMinutes(5)
        val expectedCompoundKey = "$keyspace:key-$entity"

        whenever(valueOperations.set(eq(expectedCompoundKey), eq(entity), eq(ttl)))
            .thenReturn(Mono.just(true))

        // Act
        val resultMono = wrapper.save(entity, ttl)

        // Assert
        StepVerifier.create(resultMono).expectNext(true).verifyComplete()

        verify(valueOperations).set(expectedCompoundKey, entity, ttl)
    }

    @Test
    fun `saveIfAbsent should prepend keyspace and return true if set`() {
        // Arrange
        val entity = "myEntity"
        val ttl = Duration.ofMinutes(5)
        val expectedCompoundKey = "$keyspace:key-$entity"

        whenever(valueOperations.setIfAbsent(eq(expectedCompoundKey), eq(entity), eq(ttl)))
            .thenReturn(Mono.just(true))

        // Act
        val resultMono = wrapper.saveIfAbsent(entity, ttl)

        // Assert
        StepVerifier.create(resultMono).expectNext(true).verifyComplete()

        verify(valueOperations).setIfAbsent(expectedCompoundKey, entity, ttl)
    }

    @Test
    fun `findById should prepend keyspace and return entity if found`() {
        // Arrange
        val rawKey = "123"
        val expectedCompoundKey = "$keyspace:$rawKey"
        val expectedEntity = "foundEntity"

        whenever(valueOperations.get(eq(expectedCompoundKey))).thenReturn(Mono.just(expectedEntity))

        // Act
        val resultMono = wrapper.findById(rawKey)

        // Assert
        StepVerifier.create(resultMono).expectNext(expectedEntity).verifyComplete()

        verify(valueOperations).get(expectedCompoundKey)
    }

    @Test
    fun `findById should complete empty if entity not found`() {
        // Arrange
        val rawKey = "123"
        val expectedCompoundKey = "$keyspace:$rawKey"

        whenever(valueOperations.get(eq(expectedCompoundKey))).thenReturn(Mono.empty())

        // Act
        val resultMono = wrapper.findById(rawKey)

        // Assert
        StepVerifier.create(resultMono).verifyComplete()
    }

    @Test
    fun `deleteById should return true if deleted count is greater than 0`() {
        // Arrange
        val rawKey = "123"
        val expectedCompoundKey = "$keyspace:$rawKey"

        // Spring Data Redis returns the number of deleted keys
        whenever(reactiveRedisTemplate.delete(eq(expectedCompoundKey))).thenReturn(Mono.just(1L))

        // Act
        val resultMono = wrapper.deleteById(rawKey)

        // Assert
        StepVerifier.create(resultMono).expectNext(true).verifyComplete()

        verify(reactiveRedisTemplate).delete(expectedCompoundKey)
    }

    @Test
    fun `deleteById should return false if deleted count is 0`() {
        // Arrange
        val rawKey = "123"
        val expectedCompoundKey = "$keyspace:$rawKey"

        whenever(reactiveRedisTemplate.delete(eq(expectedCompoundKey))).thenReturn(Mono.just(0L))

        // Act
        val resultMono = wrapper.deleteById(rawKey)

        // Assert
        StepVerifier.create(resultMono).expectNext(false).verifyComplete()
    }
}
