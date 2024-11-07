package it.pagopa.wallet.scheduler.documents

import it.pagopa.wallet.scheduler.WalletTestUtils
import kotlin.test.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class WalletDocumentTest {

    @Test
    fun `can build wallet document`() {
        val wallet = WalletTestUtils.paypalWalletDocument(WalletTestUtils.WALLET_VALIDATED_STATUS)
        assertNotNull(wallet.id)
        assertNotNull(wallet.userId)
        assertNotNull(wallet.status)
        assertNotNull(wallet.paymentMethodId)
        assertNotNull(wallet.contractId)
        assertNull(wallet.validationOperationResult)
        assertNull(wallet.validationErrorCode)
        assertNull(wallet.errorReason)
        assertNotNull(wallet.applications)
        wallet.applications.forEach {
            assertNotNull(it.id)
            assertNotNull(it.status)
            assertNotNull(it.creationDate)
            assertNotNull(it.updateDate)
            assertNotNull(it.metadata)
        }
        assertNotNull(wallet.details)
        assertNotNull(wallet.clients)
        wallet.clients.forEach {
            assertNotNull(it.key)
            assertNotNull(it.value.status)
        }
        assertNotNull(wallet.version)
        assertNotNull(wallet.creationDate)
        assertNotNull(wallet.updateDate)
        assertNotNull(wallet.onboardingChannel)
    }

    @Test
    fun `can build wallet details`() {

        val payPalDetails = WalletTestUtils.paypalDetails()
        assertNotNull(payPalDetails.maskedEmail)
        assertNotNull(payPalDetails.pspId)
        assertNotNull(payPalDetails.pspBusinessName)

        val cardDetails = WalletTestUtils.cardDetails()
        assertNotNull(cardDetails.type)
        assertNotNull(cardDetails.bin)
        assertNotNull(cardDetails.lastFourDigits)
        assertNotNull(cardDetails.expiryDate)
        assertNotNull(cardDetails.brand)
        assertNotNull(cardDetails.paymentInstrumentGatewayId)
    }
}
