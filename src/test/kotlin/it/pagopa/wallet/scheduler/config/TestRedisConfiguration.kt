package it.pagopa.wallet.scheduler.config

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.boot.test.context.TestConfiguration
import redis.embedded.RedisServer

@TestConfiguration
class TestRedisConfiguration {

    private lateinit var redisServer: RedisServer

    init {
        this.redisServer = RedisServer()
    }

    @PostConstruct
    fun postConstruct() {
        redisServer.start()
    }

    @PreDestroy
    fun preDestroy() {
        redisServer.stop()
    }
}
