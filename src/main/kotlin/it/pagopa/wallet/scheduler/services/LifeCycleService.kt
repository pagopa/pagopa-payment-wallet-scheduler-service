package it.pagopa.wallet.scheduler.services

import it.pagopa.wallet.documents.wallets.Wallet
import it.pagopa.wallet.scheduler.repositories.WalletRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class LifeCycleService(
    @param:Autowired val walletReactiveMongoTemplate : ReactiveMongoTemplate,
    @param:Autowired val repository: WalletRepository
) {

    fun updateTtlBulk(limit: Int): Mono<Int>{
        /**
         * Execute the bulk update in a cronological order starting from the first, with the given limit
         */
        val bulkOps =
            walletReactiveMongoTemplate.bulkOps(
                BulkOperations.BulkMode.UNORDERED,
                Wallet::class.java
            )

        return repository.findByStatusesOrderByUpdateDateAsc(listOf("TODELETE"),limit)
            .switchIfEmpty { TODO("Exception custom") }
            .collectList()
            .flatMap {wallets ->
                wallets.forEach {
                    bulkOps.updateOne(
                        Query.query(Criteria.where("_id").`is`(it.id)),
                        Update().set("ttl", 5)
                    )
                }
                bulkOps.execute()
            }.map { it.modifiedCount }
    }

}