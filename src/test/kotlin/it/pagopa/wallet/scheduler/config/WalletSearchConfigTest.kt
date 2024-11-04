package it.pagopa.wallet.scheduler.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(locations = ["classpath:application.test.properties"])
@EnableConfigurationProperties(value = [WalletSearchConfig::class])
class WalletSearchConfigTest {

    @Autowired private lateinit var walletSearchConfig: WalletSearchConfig

    @Test
    fun configLoads() {
        // check if config is loaded
        Assertions.assertEquals(walletSearchConfig.status, "VALIDATED")
        Assertions.assertEquals(walletSearchConfig.limit, 10)
    }
}
