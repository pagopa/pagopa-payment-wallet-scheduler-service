package it.pagopa.wallet.documents.wallets

import it.pagopa.wallet.documents.wallets.details.WalletDetails
import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("payment-wallets")
data class Wallet(
    @Id val id: String,
    val userId: String,
    val status: String,
    val paymentMethodId: String,
    val contractId: String?,
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
