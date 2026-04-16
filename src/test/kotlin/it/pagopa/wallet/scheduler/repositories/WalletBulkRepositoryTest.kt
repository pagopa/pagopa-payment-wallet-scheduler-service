package it.pagopa.wallet.scheduler.repositories

import com.mongodb.bulk.BulkWriteResult
import it.pagopa.wallet.documents.wallets.Wallet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.ReactiveBulkOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class WalletBulkRepositoryTest {
    private val reactiveMongoTemplate: ReactiveMongoTemplate = mock()
    private val bulkOperations: ReactiveBulkOperations = mock()

    private val repository = WalletBulkRepository(reactiveMongoTemplate)

    @Test
    fun `should execute bulk updates and return modified count`() {
        // Arrange
        val walletTtlMap = mapOf("wallet-1" to 3600, "wallet-2" to 7200)

        val mockWriteResult: BulkWriteResult = mock { on { modifiedCount } doReturn 2 }

        whenever(
                reactiveMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Wallet::class.java)
            )
            .thenReturn(bulkOperations)

        whenever(bulkOperations.execute()).thenReturn(Mono.just(mockWriteResult))

        // Act & Assert
        StepVerifier.create(repository.bulkUpdateTtl(walletTtlMap)).expectNext(2).verifyComplete()

        argumentCaptor<Query>().apply {
            val updateCaptor = argumentCaptor<Update>()

            verify(bulkOperations, times(2)).updateOne(capture(), updateCaptor.capture())

            val queries = allValues
            val updates = updateCaptor.allValues

            // Asserting Wallet 1
            val query1Doc = queries[0].queryObject
            assertEquals("wallet-1", query1Doc["_id"], "First query should target wallet-1")

            val update1Doc = updates[0].updateObject
            val setDoc1 = update1Doc["\$set"] as org.bson.Document
            assertEquals(3600, setDoc1["ttl"], "First update should set ttl to 3600")

            // Asserting Wallet 2
            val query2Doc = queries[1].queryObject
            assertEquals("wallet-2", query2Doc["_id"], "Second query should target wallet-2")

            val update2Doc = updates[1].updateObject
            val setDoc2 = update2Doc["\$set"] as org.bson.Document
            assertEquals(7200, setDoc2["ttl"], "Second update should set ttl to 7200")
        }

        verify(bulkOperations, times(1)).execute()
    }
}
