package it.pagopa.wallet.scheduler

import com.mongodb.reactivestreams.client.MongoClient
import it.pagopa.wallet.scheduler.config.TestRedisConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.TestPropertySource

@SpringBootTest(classes = [TestRedisConfiguration::class])
@TestPropertySource(locations = ["classpath:application.test.properties"])
class PagopaPaymentWalletSchedulerApplicationTests {
    @MockBean private lateinit var mongoClient: MongoClient

    @Test
    fun contextLoads() {
        // check only if the context is loaded
        Assertions.assertTrue(true)
    }
}
