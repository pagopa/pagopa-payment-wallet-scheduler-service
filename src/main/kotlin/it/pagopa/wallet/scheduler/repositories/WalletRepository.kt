package it.pagopa.wallet.scheduler.repositories

import it.pagopa.wallet.scheduler.documents.Wallet
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface WalletRepository : ReactiveCrudRepository<Wallet, String> {

    @Aggregation(
        "{\$match: {'creationDate': {'\$gt': '?0','\$lte': '?1'},'status': '?2'}}",
        "{\$sort: {'updateDate': 1}}",
        "{\$limit: ?3}",
    )
    fun findByCreationDateBetweenAndStatusOrderByUpdateDateAsc(
        startDate: String,
        endDate: String,
        status: String,
        limit: Int
    ): Flux<Wallet>
}
