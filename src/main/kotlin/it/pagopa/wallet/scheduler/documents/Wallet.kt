package it.pagopa.wallet.scheduler.documents

import it.pagopa.wallet.scheduler.documents.details.WalletDetails
import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("payment-wallets")
data class Wallet(
    @Id val id: String,
    val status: String,
    val paymentMethodId: String,
    val validationOperationResult: String?,
    val validationErrorCode: String?,
    val errorReason: String?,
    val applications: List<WalletApplication>,
    val details: WalletDetails<*>?,
    val clients: Map<String, Client>,
    val version: Int,
    val creationDate: Instant,
    val updateDate: Instant,
    val onboardingChannel: String
)
