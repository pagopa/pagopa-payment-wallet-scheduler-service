package it.pagopa.wallet.scheduler.client

import it.pagopa.generated.walletservice.api.WalletsApi
import it.pagopa.wallet.scheduler.exceptions.WalletServiceException
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class WalletServiceClient(
    @param:Autowired
    @param:Qualifier("walletServiceWebClient")
    private val walletServiceWebClient: WalletsApi
) {
    fun deactivateContract(walletId: String): Mono<Void> {
        return walletServiceWebClient
            .deactivateContractByWalletId(UUID.fromString(walletId))
            .onErrorMap { error -> WalletServiceException(walletId, error) }
    }
}
