package it.pagopa.wallet.scheduler.service

import it.pagopa.wallet.scheduler.config.properties.RedisJobLockPolicyConfig
import it.pagopa.wallet.scheduler.exceptions.LockNotAcquiredException
import it.pagopa.wallet.scheduler.exceptions.LockNotReleasedException
import it.pagopa.wallet.scheduler.exceptions.SemNotAcquiredException
import it.pagopa.wallet.scheduler.exceptions.SemNotReleasedException
import java.util.concurrent.TimeUnit
import org.redisson.api.RedissonReactiveClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class SchedulerLockService(
    @Autowired private val redissonClient: RedissonReactiveClient,
    @Autowired private val redisJobLockPolicyConfig: RedisJobLockPolicyConfig
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun acquireJobLock(jobName: String): Mono<Void> {
        logger.info("Trying to acquire lock for job: {}", jobName)
        return redissonClient
            .getLock(redisJobLockPolicyConfig.getLockNameByJob(jobName))
            .tryLock(
                redisJobLockPolicyConfig.waitTimeSec,
                redisJobLockPolicyConfig.ttlSec,
                TimeUnit.SECONDS
            )
            .filter { it == true } // only lock acquired
            .doOnSuccess { logger.info("Lock acquired for job: {}", jobName) }
            .onErrorMap {
                logger.error("Lock acquiring error for job: {}", jobName, it)
                LockNotAcquiredException(jobName, it)
            }
            .switchIfEmpty(Mono.error(LockNotAcquiredException(jobName)))
            .then()
    }

    fun releaseJobLock(jobName: String): Mono<Void> {
        logger.info("Trying to release lock for job: {}", jobName)
        return redissonClient
            .getLock(redisJobLockPolicyConfig.getLockNameByJob(jobName))
            .unlock()
            .doOnSuccess { logger.info("Lock released for job: {}", jobName) }
            .onErrorMap {
                logger.error("Lock releasing error for job: {}", jobName, it)
                LockNotReleasedException(jobName, it)
            }
    }

    fun acquireJobSemaphore(jobName: String): Mono<String> {
        logger.info("Trying to acquire semaphore for job: {}", jobName)
        val semaphore =
            redissonClient.getPermitExpirableSemaphore(
                redisJobLockPolicyConfig.getSemNameByJob(jobName)
            )
        return semaphore
            .trySetPermits(1)
            .flatMap {
                semaphore.tryAcquire(
                    redisJobLockPolicyConfig.waitTimeSec,
                    redisJobLockPolicyConfig.ttlSec,
                    TimeUnit.SECONDS
                )
            }
            .doOnSuccess { logger.info("Semaphore [{}] acquired for job: {}", it, jobName) }
            .onErrorMap {
                logger.error("Semaphore acquiring error for job: {}", jobName, it)
                SemNotAcquiredException(jobName, it)
            }
            .switchIfEmpty(Mono.error(SemNotAcquiredException(jobName)))
    }

    fun releaseJobSemaphore(jobName: String, semaphoreId: String): Mono<Void> {
        logger.info("Trying to release semaphore for job: {}", jobName)
        return redissonClient
            .getPermitExpirableSemaphore(redisJobLockPolicyConfig.getSemNameByJob(jobName))
            .release(semaphoreId)
            .doOnSuccess { logger.info("Semaphore released for job: {}", jobName) }
            .onErrorMap {
                logger.error("Semaphore releasing error for job: {}", jobName, it)
                SemNotReleasedException(jobName, it)
            }
    }
}
