package it.pagopa.wallet.scheduler

import it.pagopa.wallet.scheduler.documents.Wallet
import java.time.Instant
import java.util.*

object WalletTestUtils {

    val USER_ID = UUID.randomUUID()
    val WALLET_UUID = UUID.randomUUID()
    val WALLET_CREATED_STATUS = "CREATED"
    val CONTRACT_ID = "W49357937935R869i"
    val PAYMENT_METHOD_ID = UUID.randomUUID()
    val ONBOARDING_CHANNEL = "IO"
    val CREATION_DATE: Instant = Instant.now()

    fun walletDocument(status: String): Wallet {
        return Wallet(
            id = WALLET_UUID.toString(),
            userId = USER_ID.toString(),
            status = status,
            paymentMethodId = PAYMENT_METHOD_ID.toString(),
            contractId = CONTRACT_ID,
            validationOperationResult = null,
            validationErrorCode = null,
            errorReason = null,
            // applications = listOf(),
            // details = null,
            // clients = clients.entries.associate { it.key.name to it.value.toDocument() },
            version = 0,
            creationDate = CREATION_DATE,
            updateDate = CREATION_DATE,
            onboardingChannel = ONBOARDING_CHANNEL
        )
    }
}
