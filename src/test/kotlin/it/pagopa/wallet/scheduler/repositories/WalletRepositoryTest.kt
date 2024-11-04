package it.pagopa.wallet.scheduler.repositories

import it.pagopa.wallet.scheduler.WalletTestUtils
import it.pagopa.wallet.scheduler.WalletTestUtils.WALLET_VALIDATED_STATUS
import it.pagopa.wallet.scheduler.config.MongoConfiguration
import it.pagopa.wallet.scheduler.config.WalletSearchConfig
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

@DataMongoTest
@ExtendWith(SpringExtension::class)
@Import(MongoConfiguration::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class WalletRepositoryTest {

    @Autowired lateinit var walletRepository: WalletRepository
    @Autowired lateinit var walletSearchConfig: WalletSearchConfig

    @Test
    fun `Should return wallets limited results`() {
        val startDate: String = Instant.now().minus(10, ChronoUnit.DAYS).toString()
        val endDate: String = Instant.now().plus(10, ChronoUnit.DAYS).toString()

        // save more then limit elements on db
        val saveMono =
            Flux.range(1, walletSearchConfig.limit + 1)
                .flatMap {
                    walletRepository.save(WalletTestUtils.walletDocument(WALLET_VALIDATED_STATUS))
                }
                .last()

        val walletsMono =
            saveMono.flatMap {
                walletRepository
                    .findByCreationDateBetweenAndStatusOrderByUpdateDateAsc(
                        startDate = startDate,
                        endDate = endDate,
                        status = WALLET_VALIDATED_STATUS,
                        limit = walletSearchConfig.limit
                    )
                    .collectList()
            }

        StepVerifier.create(walletsMono)
            .assertNext { list -> assertEquals(walletSearchConfig.limit, list.size) }
            .verifyComplete()
    }
}
