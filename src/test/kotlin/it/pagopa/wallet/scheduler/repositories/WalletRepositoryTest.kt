package it.pagopa.wallet.scheduler.repositories

import it.pagopa.wallet.scheduler.WalletTestUtils
import it.pagopa.wallet.scheduler.WalletTestUtils.WALLET_CREATED_STATUS
import it.pagopa.wallet.scheduler.config.WalletSearchConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.domain.Limit
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals


@DataMongoTest
@ExtendWith(SpringExtension::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class WalletRepositoryTest {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Autowired lateinit var walletRepository: WalletRepository
    @Autowired lateinit var walletSearchConfig: WalletSearchConfig

    @Test
    fun `Should return wallets limited results`() {
        val startDate: String = Instant.now().minus(10, ChronoUnit.DAYS).toString()
        val endDate: String = Instant.now().plus(10, ChronoUnit.DAYS).toString()

        //save 5 elements on db
        val saveMono = Flux.range(1, 4).flatMap {
            walletRepository.save(WalletTestUtils.walletDocument(WALLET_CREATED_STATUS))
        }.last()

        val walletsMono = saveMono.flatMap {
            walletRepository.findByCreationDateBetweenAndStatusOrderByUpdateDateAsc(
                startDate = startDate,
                endDate = endDate,
                status = WALLET_CREATED_STATUS,
                limit = walletSearchConfig.limit
            ).collectList()
        }

        StepVerifier.create( walletsMono )
            .assertNext { list ->
                assertEquals(walletSearchConfig.limit, list.size)
            }
            .verifyComplete()
    }
}