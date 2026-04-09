package it.pagopa.wallet.services

import it.pagopa.wallet.scheduler.config.properties.RedisResumePolicyConfig
import it.pagopa.wallet.scheduler.repositories.ReactiveResumeTimestampWrapper
import it.pagopa.wallet.scheduler.repositories.redis.ResumeTimestamp
import it.pagopa.wallet.scheduler.services.RedisResumePolicyService
import it.pagopa.wallet.scheduler.services.ResumePolicyService
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class RedisResumePolicyServiceTest {
    private val reactiveResumeTimestampWrapper: ReactiveResumeTimestampWrapper = mock()
    private val redisResumePolicyConfig: RedisResumePolicyConfig = mock()
    private lateinit var redisResumePolicyService: ResumePolicyService

    @BeforeEach
    fun initEventStream() {
        redisResumePolicyService =
            RedisResumePolicyService(reactiveResumeTimestampWrapper, redisResumePolicyConfig)
    }

    @Test
    fun `redis resume policy will get resume timestamp in case of cache hit`() {
        val expected = Instant.now()
        val resumeTimestamp = ResumeTimestamp("target_test", expected)

        whenever(reactiveResumeTimestampWrapper.findById(anyOrNull()))
            .thenReturn(Mono.just(resumeTimestamp))

        StepVerifier.create(redisResumePolicyService.getResumeTimestamp("target_test"))
            .expectNext(expected)
            .verifyComplete()
    }

    @Test
    fun `redis resume policy will save resume timestamp`() {
        val expected: Instant = Instant.now()
        given(reactiveResumeTimestampWrapper.save(anyOrNull(), anyOrNull()))
            .willReturn(Mono.just(true))

        redisResumePolicyService.saveResumeTimestamp("target_test", expected)

        verify(reactiveResumeTimestampWrapper, times(1)).save(anyOrNull(), anyOrNull())
    }
}
