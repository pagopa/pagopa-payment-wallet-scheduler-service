package it.pagopa.wallet.scheduler.repositories

import it.pagopa.wallet.scheduler.WalletTestUtils
import it.pagopa.wallet.scheduler.WalletTestUtils.WALLET_CREATED_STATUS
import it.pagopa.wallet.scheduler.config.WalletSearchConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
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

    @Test
    fun `Should return wallets limited results`() {
        val startDate: String = Instant.now().minus(10, ChronoUnit.MINUTES).toString()
        val endDate: String = Instant.now().plus(10, ChronoUnit.MINUTES).toString()

        val saveMono = Flux.merge(
            walletRepository.save(WalletTestUtils.walletDocument("CREATED")),
            walletRepository.save(WalletTestUtils.walletDocument("CREATED"))
        ).last()

        val walletsMono = saveMono.flatMap {
            walletRepository.findByCreationDateBetweenAndStatusOrderByUpdateDateAsc(
                startDate,
                endDate,
                WALLET_CREATED_STATUS,
                walletSearchConfig.limit
            ).collectList()
        }

        StepVerifier.create( walletsMono )
            .assertNext { list ->
                assertEquals(2, list.size)
            }
            .verifyComplete()
    }
}