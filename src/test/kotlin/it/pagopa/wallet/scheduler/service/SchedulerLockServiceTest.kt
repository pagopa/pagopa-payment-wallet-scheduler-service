package it.pagopa.wallet.scheduler.service

import it.pagopa.wallet.scheduler.config.properties.RedisJobLockPolicyConfig
import kotlin.test.Test
import org.junit.jupiter.api.BeforeAll
import org.redisson.Redisson
import org.redisson.api.RedissonReactiveClient
import org.redisson.config.Config
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.test.StepVerifier
import redis.embedded.RedisServer

class SchedulerLockServiceTest {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun `Should acquire lock and release after work`() {
        val jobName = "job-name"
        val result =
            schedulerLockService
                .acquireJobLock(jobName)
                .doOnSuccess { logger.info("Simulate job work") }
                .then(schedulerLockService.releaseJobLock(jobName))

        StepVerifier.create(result).expectComplete().verify()
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
