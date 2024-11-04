package it.pagopa.wallet.scheduler.services

import it.pagopa.wallet.scheduler.config.WalletSearchConfig
import it.pagopa.wallet.scheduler.documents.Wallet
import it.pagopa.wallet.scheduler.exceptions.WalletInvalidRangeException
import it.pagopa.wallet.scheduler.repositories.WalletRepository
import java.time.Instant
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class WalletService(
    @Autowired private val walletRepository: WalletRepository,
    @Autowired private val walletSearchConfig: WalletSearchConfig
) {
    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun getWalletsForCdcIngestion(startDate: Instant, endDate: Instant): Flux<Wallet> {
        logger.info(
            "Search wallets for CDC ingestion. From [{}] to [{}] - Status [{}] - Limit [{}]",
            startDate,
            endDate,
            walletSearchConfig.status,
            walletSearchConfig.limit
        )

        // check if is valid date range
        if (endDate.isBefore(startDate))
            return Flux.error(WalletInvalidRangeException(startDate, endDate))

        return walletRepository.findByCreationDateBetweenAndStatusOrderByUpdateDateAsc(
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            status = walletSearchConfig.status,
            limit = walletSearchConfig.limit
        )
    }
}
