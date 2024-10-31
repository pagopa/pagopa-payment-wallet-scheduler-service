package it.pagopa.wallet.scheduler.repositories

import it.pagopa.wallet.scheduler.WalletTestUtils
import it.pagopa.wallet.scheduler.WalletTestUtils.WALLET_CREATED_STATUS
import it.pagopa.wallet.scheduler.config.WalletSearchConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.test.StepVerifier
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

@DataMongoTest
@ExtendWith(SpringExtension::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class WalletRepositoryTest {

    @Autowired lateinit var walletRepository: WalletRepository
    @Autowired lateinit var walletSearchConfig: WalletSearchConfig

    @BeforeEach
    fun setup() {
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
        walletRepository.save(WalletTestUtils.walletDocument("CREATED")).block()
    }

    @Test
    fun `Should return wallets limited results`() {
        val startDate: String = Instant.now().minus(10, ChronoUnit.MINUTES).toString()
        val endDate: String = Instant.now().plus(10, ChronoUnit.MINUTES).toString()

        StepVerifier.create( walletRepository.findByCreationDateBetweenAndStatusOrderByUpdateDateAsc(startDate, endDate, WALLET_CREATED_STATUS, walletSearchConfig.limit).collectList())
            .consumeNextWith { list ->
                assertEquals(list.size, 30)
            }
            .verifyComplete()
    }
}