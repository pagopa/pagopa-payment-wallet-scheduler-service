package it.pagopa.wallet.scheduler

import it.pagopa.wallet.scheduler.documents.Client
import it.pagopa.wallet.scheduler.documents.Wallet
import it.pagopa.wallet.scheduler.documents.WalletApplication
import it.pagopa.wallet.scheduler.documents.details.CardDetails
import it.pagopa.wallet.scheduler.documents.details.PayPalDetails
import java.time.Instant
import java.util.*

object WalletTestUtils {

    val USER_ID = UUID.randomUUID()
    val WALLET_VALIDATED_STATUS = "VALIDATED"
    val CONTRACT_ID = "W49357937935R869i"
    val PAYMENT_METHOD_ID = UUID.randomUUID()
    val ONBOARDING_CHANNEL = "IO"
    val CREATION_DATE: Instant = Instant.now()

    fun walletDocument(status: String): Wallet {
        return Wallet(
            id = UUID.randomUUID().toString(),
            userId = USER_ID.toString(),
            status = status,
            paymentMethodId = PAYMENT_METHOD_ID.toString(),
            contractId = CONTRACT_ID,
            validationOperationResult = null,
            validationErrorCode = null,
            errorReason = null,
            applications =
                listOf(
                    WalletApplication(
                        id = "PAGOPA",
                        status = "ENABLED",
                        creationDate = CREATION_DATE.toString(),
                        updateDate = CREATION_DATE.toString(),
                        metadata = mapOf()
                    )
                ),
            details = paypalDetails(),
            clients = mapOf("IO" to Client(status = "ENABLED")),
            version = 0,
            creationDate = CREATION_DATE,
            updateDate = CREATION_DATE,
            onboardingChannel = ONBOARDING_CHANNEL
        )
    }

    fun paypalDetails() =
        PayPalDetails(
            maskedEmail = "b***@icbpi.it",
            pspId = "BCITITMM",
            pspBusinessName = "Intesa Sanpaolo S.p.A"
        )

    fun cardDetails() =
        CardDetails(
            type = "CARDS",
            bin = "559416",
            lastFourDigits = "8043",
            expiryDate = "202703",
            brand = "MASTERCARD",
            paymentInstrumentGatewayId =
                "49374e5143376a7337473775354d4e4d564e6e534661775756346f564b7531734f6672526b516675417a553d",
        )
}
