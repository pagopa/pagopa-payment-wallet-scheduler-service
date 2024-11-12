package it.pagopa.wallet.scheduler.config

import com.mongodb.reactivestreams.client.MongoClient
import de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration
import it.pagopa.wallet.scheduler.config.properties.RedisJobLockPolicyConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonReactiveClient
import org.redisson.spring.starter.RedissonAutoConfigurationV2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(locations = ["classpath:application.test.properties"])
@EnableConfigurationProperties(value = [WalletSearchConfig::class, RedisJobLockPolicyConfig::class])
@EnableAutoConfiguration(
    exclude = [EmbeddedMongoAutoConfiguration::class, RedissonAutoConfigurationV2::class]
)
class WalletSchedulerConfigsTest {
    @MockBean private lateinit var mongoClient: MongoClient
    @MockBean private lateinit var redissonReactiveClient: RedissonReactiveClient

    @Autowired private lateinit var walletSearchConfig: WalletSearchConfig
    @Autowired private lateinit var redisJobLockPolicyConfig: RedisJobLockPolicyConfig

    @Test
    fun configLoads() {
        // check if config is loaded
        Assertions.assertEquals(walletSearchConfig.status, "VALIDATED")
        Assertions.assertEquals(walletSearchConfig.limit, 10)
        Assertions.assertEquals(redisJobLockPolicyConfig.keyspace, "keyspace")
        Assertions.assertEquals(redisJobLockPolicyConfig.ttlMs, 20000)
        Assertions.assertEquals(redisJobLockPolicyConfig.waitTimeMs, 2000)
    }
}
