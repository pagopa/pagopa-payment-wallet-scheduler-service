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
    fun bulkUpdateTtl(walletTtlMap: Map<String, Int>): Mono<Int> {
        val bulkOps =
            reactiveMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Wallet::class.java)

        walletTtlMap.forEach {
            bulkOps.updateOne(
                Query.query(Criteria.where("_id").`is`(it.key)),
                Update.update("ttl", it.value)
            )
        }

        return bulkOps.execute().map { it.modifiedCount }
    }
}
