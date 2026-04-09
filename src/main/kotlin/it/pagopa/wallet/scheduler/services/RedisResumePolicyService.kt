package it.pagopa.wallet.scheduler.services

import it.pagopa.wallet.scheduler.config.properties.RedisResumePolicyConfig
import it.pagopa.wallet.scheduler.repositories.redis.ReactiveResumeTimestampWrapper
import it.pagopa.wallet.scheduler.repositories.redis.ResumeTimestamp
import java.time.Duration
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class RedisResumePolicyService(
    private val reactiveResumeTimestampWrapper: ReactiveResumeTimestampWrapper,
    private val redisResumePolicyConfig: RedisResumePolicyConfig
) : ResumePolicyService {
    private val logger = LoggerFactory.getLogger(RedisResumePolicyService::class.java)

    override fun getResumeTimestamp(target: String): Mono<Instant> {
        return reactiveResumeTimestampWrapper.findById(target).map { it.timestamp }
    }

    override fun saveResumeTimestamp(target: String, timestamp: Instant): Mono<Boolean> {
        logger.info("Saving instant: {} with target: {}", timestamp.toString(), target)
        val resumeTimestamp = ResumeTimestamp(target, timestamp)
        return reactiveResumeTimestampWrapper.save(
            resumeTimestamp,
            Duration.ofMinutes(redisResumePolicyConfig.ttlInMin)
        )
    }
}
