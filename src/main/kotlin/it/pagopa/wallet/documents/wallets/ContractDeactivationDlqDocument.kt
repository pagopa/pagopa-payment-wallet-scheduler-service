package it.pagopa.wallet.documents.wallets

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("wallet-contractId-deactivation-dlq")
data class ContractDeactivationDlqDocument(
    @Id val id: String,
    val timestamp: String,
)
