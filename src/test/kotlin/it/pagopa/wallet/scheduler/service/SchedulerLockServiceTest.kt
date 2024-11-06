package it.pagopa.wallet.scheduler.service

import it.pagopa.wallet.scheduler.config.properties.RedisJobLockPolicyConfig
import it.pagopa.wallet.scheduler.exceptions.LockNotAcquiredException
import it.pagopa.wallet.scheduler.exceptions.LockNotReleasedException
import kotlin.test.Test
import org.junit.jupiter.api.BeforeAll
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.redisson.Redisson
import org.redisson.api.RLockReactive
import org.redisson.api.RedissonReactiveClient
import org.redisson.config.Config
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import redis.embedded.RedisServer

class SchedulerLockServiceTest {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val rLockReactive: RLockReactive = mock()

    @Test
    fun `Should acquire lock and release after work`() {
        val jobName = "job-name"

        schedulerLockService
            .acquireJobLock(jobName)
            .doOnSuccess { logger.info("Simulate job work") }
            .then(schedulerLockService.releaseJobLock(jobName))
            .test()
            .expectComplete()
            .verify()
    }

    @Test
    fun `Should throw LockNotAcquiredException when lock is already aquired`() {
        given { rLockReactive.tryLock(any(), any(), any()) }.willReturn(Mono.just(false))

        schedulerLockService
            .acquireJobLock("job-name")
            .test()
            .expectError(LockNotAcquiredException::class.java)
    }

    @Test
    fun `Should throw LockNotReleasedException when fail lock release`() {
        given { rLockReactive.unlock() }.willThrow(RuntimeException::class.java)

        schedulerLockService
            .releaseJobLock("job-name")
            .test()
            .expectError(LockNotReleasedException::class.java)
    }

    companion object {

        private lateinit var redisServer: RedisServer
        private lateinit var redissonClient: RedissonReactiveClient
        private lateinit var redisJobLockPolicyConfig: RedisJobLockPolicyConfig
        private lateinit var schedulerLockService: SchedulerLockService

        @JvmStatic
        @BeforeAll
        fun setUp(): Unit {
            redisServer = RedisServer(9999)
            redisServer.start()
            redissonClient =
                Redisson.create(
                        Config().apply { useSingleServer().setAddress("redis://127.0.0.1:9999") }
                    )
                    .reactive()
            redisJobLockPolicyConfig = RedisJobLockPolicyConfig("keyspace", 20, 2)
            schedulerLockService = SchedulerLockService(redissonClient, redisJobLockPolicyConfig)
        }
    }
}
