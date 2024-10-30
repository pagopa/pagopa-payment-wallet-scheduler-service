package it.pagopa.wallet.scheduler.repositories

import it.pagopa.wallet.scheduler.documents.Wallet
import org.springframework.data.domain.Limit
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface WalletRepository : ReactiveCrudRepository<Wallet, String> {

    fun findByCreationDateBetweenAndStatusOrderByUpdateDateAsc(
        startDate: String,
        endDate: String,
        status: String,
        limit: Limit
    ): Flux<Wallet>
}
