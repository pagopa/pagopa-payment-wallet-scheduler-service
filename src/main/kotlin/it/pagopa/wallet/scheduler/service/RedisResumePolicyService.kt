package it.pagopa.wallet.scheduler.service

import it.pagopa.wallet.scheduler.config.properties.RedisResumePolicyConfig
import java.time.Duration
import java.time.Instant
import java.util.NoSuchElementException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RedisResumePolicyService(
    @Autowired private val redisTemplate: TimestampRedisTemplate,
    @Autowired private val redisResumePolicyConfig: RedisResumePolicyConfig
) : ResumePolicyService {
    private val logger = LoggerFactory.getLogger(RedisResumePolicyService::class.java)

    override fun getResumeTimestamp(target: String): Instant {
        return redisTemplate
            .findByKeyspaceAndTarget(redisResumePolicyConfig.keyspace, target)
            .orElseThrow { NoSuchElementException("No resume timestamp found for target: $target") }
    }

    override fun saveResumeTimestamp(target: String, timestamp: Instant) {
        logger.debug("Saving instant: {}", timestamp.toString())
        redisTemplate.save(
            redisResumePolicyConfig.keyspace,
            target,
            timestamp,
            Duration.ofMinutes(redisResumePolicyConfig.ttlInMin)
        )
    }
}
