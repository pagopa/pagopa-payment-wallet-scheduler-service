package it.pagopa.wallet.scheduler.service

import it.pagopa.wallet.scheduler.config.properties.RedisJobLockPolicyConfig
import it.pagopa.wallet.scheduler.exceptions.LockNotAcquiredException
import it.pagopa.wallet.scheduler.exceptions.LockNotReleasedException
import java.util.concurrent.TimeUnit
import org.redisson.api.RedissonReactiveClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class SchedulerLockService(
    @Autowired private val redissonClient: RedissonReactiveClient,
    @Autowired private val redisJobLockPolicyConfig: RedisJobLockPolicyConfig
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun acquireJobLock(jobName: String): Mono<Void> {
        logger.info("Trying to acquire lock for job: {}", jobName)
        return redissonClient
            .getLock(redisJobLockPolicyConfig.getLockByTarget(jobName))
            .tryLock(
                redisJobLockPolicyConfig.waitTimeSec,
                redisJobLockPolicyConfig.ttlSec,
                TimeUnit.SECONDS
            )
            .filter { it == true } // only lock acquired
            .switchIfEmpty { Mono.error(LockNotAcquiredException(jobName)) }
            .doOnSuccess { logger.info("Lock acquired for job: {}", jobName) }
            .onErrorResume { Mono.error(LockNotAcquiredException(jobName, it)) }
            .then()
    }

    fun releaseJobLock(jobName: String): Mono<Void> {
        logger.info("Trying to release lock for job: {}", jobName)
        return redissonClient
            .getLock(redisJobLockPolicyConfig.getLockByTarget(jobName))
            .unlock()
            .doOnSuccess { logger.info("Lock released for job: {}", jobName) }
            .onErrorResume { Mono.error(LockNotReleasedException(jobName, it)) }
    }
}
