package it.pagopa.wallet.scheduler.repositories

import it.pagopa.wallet.documents.wallets.Wallet
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
class WalletBulkRepository(
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
) {
    fun bulkUpdateTtl(walletIds: List<String>, ttlValue: Int): Mono<Int> {
        if (walletIds.isEmpty()) {
            return Mono.just(0)
        }

        val bulkOps =
            reactiveMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Wallet::class.java)

        walletIds.forEach {
            bulkOps.updateOne(
                Query.query(Criteria.where("_id").`is`(it)),
                Update.update("ttl", ttlValue)
            )
        }

        return bulkOps.execute().map { it.modifiedCount }
    }
}
