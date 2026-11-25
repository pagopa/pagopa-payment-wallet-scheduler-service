package it.pagopa.wallet.scheduler.repositories

import it.pagopa.generated.walletservice.model.WalletStatus
import it.pagopa.wallet.documents.wallets.Wallet
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface WalletRepository : ReactiveCrudRepository<Wallet, String> {

    @Aggregation(
        "{\$match: {'creationDate': {'\$gt': '?0','\$lte': '?1'},'status': '?2'}}",
        "{\$sort: {'creationDate': 1}}",
        "{\$limit: ?3}",
    )
    fun findByCreationDateBetweenAndStatusOrderByUpdateDateAsc(
        startDate: String,
        endDate: String,
        status: String,
        limit: Int
    ): Flux<Wallet>

    @Aggregation(
        "{\$match: { 'ttl': null, 'status': {'\$in':?0}, 'updateDate': {'\$lte': '?1'}, '\$or': [{ 'deactivationRetryCount': null }, { 'deactivationRetryCount': {'\$lt': ?2} }] }}",
        "{\$sort: {'updateDate': 1}}",
        "{\$limit: ?3}",
    )
    fun findWalletToDelete(
        statuses: Set<WalletStatus>,
        endDate: String,
        contractDeactivationMaxRetries: Int,
        limit: Int
    ): Flux<Wallet>

    /** Updates retry counter without triggering last-modified auditing. */
    @Query("{ '_id': ?0, 'version': ?1 }")
    @Update("{ '\$set': { 'deactivationRetryCount': ?2 }, '\$inc': { 'version': 1 } }")
    fun updateDeactivationRetryCount(id: String, version: Int, retryCount: Int): Mono<Long>
}
