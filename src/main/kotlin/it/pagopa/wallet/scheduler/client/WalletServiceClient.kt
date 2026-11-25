package it.pagopa.wallet.scheduler.client

import it.pagopa.generated.walletservice.api.WalletsApi
import it.pagopa.wallet.scheduler.exceptions.WalletServiceException
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Component
class WalletServiceClient(
    @param:Autowired
    @param:Qualifier("walletServiceWebClient")
    private val walletServiceWebClient: WalletsApi
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun deactivateContract(walletId: String): Mono<Void> {
        return walletServiceWebClient
            .deactivateContractByWalletId(UUID.fromString(walletId))
            .doOnError { error ->
                val reason =
                    if (error is WebClientResponseException) "HTTP ${error.statusCode.value()}"
                    else error.javaClass.simpleName
                logger.warn("Contract deactivation failed for wallet $walletId: $reason")
            }
            .onErrorMap { error -> WalletServiceException(walletId, error) }
    }
}
