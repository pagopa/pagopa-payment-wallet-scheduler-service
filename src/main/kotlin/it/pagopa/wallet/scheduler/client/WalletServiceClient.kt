package it.pagopa.wallet.scheduler.client

import it.pagopa.generated.walletservice.api.WalletsApi
import it.pagopa.wallet.scheduler.exceptions.ContractDeactivationBadGatewayException
import it.pagopa.wallet.scheduler.exceptions.WalletServiceException
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
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
            .doOnError(WebClientResponseException::class.java) {
                logger.error(
                    "Error communicating with wallet-service on contract deactivation. Response status: ${it.statusCode}"
                )
            }
            .onErrorMap { error ->
                if (
                    error is WebClientResponseException &&
                        error.statusCode == HttpStatus.BAD_GATEWAY
                ) {
                    ContractDeactivationBadGatewayException(walletId, error)
                } else {
                    WalletServiceException(walletId, error)
                }
            }
    }
}
