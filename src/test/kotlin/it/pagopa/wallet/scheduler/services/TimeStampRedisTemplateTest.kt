package it.pagopa.wallet.services

import it.pagopa.wallet.scheduler.services.TimestampRedisTemplate
import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class ReactiveTimestampRedisTemplateTest {

    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Instant> = mock()
    private val valueOps: ReactiveValueOperations<String, Instant> = mock()
    private lateinit var timestampRedisTemplate: TimestampRedisTemplate

    @BeforeEach
    fun setup() {
        timestampRedisTemplate = TimestampRedisTemplate(reactiveRedisTemplate)
    }

    @Test
    fun `time stamp redis template saves instant`() {
        given { reactiveRedisTemplate.opsForValue() }.willReturn(valueOps)
        doNothing().`when`(valueOps).set(anyOrNull(), anyOrNull(), any<Duration>())

        timestampRedisTemplate.save("keyspace", "target", Instant.now(), Duration.ofSeconds(0))
        verify(valueOps, times(1)).set(anyOrNull(), anyOrNull(), any<Duration>())
        verify(reactiveRedisTemplate, times(1)).opsForValue()
    }

    @Test
    fun `time stamp redis template gets instant`() {
        val now = Instant.now()
        given { reactiveRedisTemplate.opsForValue() }.willReturn(valueOps)
        given { valueOps.get(anyOrNull()) }.willReturn(Mono.just(now))

        val resumeTimestamp = timestampRedisTemplate.findByKeyspaceAndTarget("keyspace", "target")

        StepVerifier.create(resumeTimestamp).expectNext(now).verifyComplete()
        verify(valueOps, times(1)).get(anyOrNull())
        verify(reactiveRedisTemplate, times(1)).opsForValue()
    }
}
