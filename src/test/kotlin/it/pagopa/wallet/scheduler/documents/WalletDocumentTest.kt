package it.pagopa.wallet.scheduler.documents

import it.pagopa.wallet.scheduler.WalletTestUtils
import kotlin.test.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class WalletDocumentTest {

    @Test
    fun `can build wallet document`() {
        val wallet = WalletTestUtils.walletDocument(WalletTestUtils.WALLET_CREATED_STATUS)
        assertNotNull(wallet.id)
        assertNotNull(wallet.userId)
        assertNotNull(wallet.status)
        assertNotNull(wallet.paymentMethodId)
        assertNotNull(wallet.contractId)
        assertNull(wallet.validationOperationResult)
        assertNull(wallet.validationErrorCode)
        assertNull(wallet.errorReason)
        assertNotNull(wallet.version)
        assertNotNull(wallet.creationDate)
        assertNotNull(wallet.updateDate)
        assertNotNull(wallet.onboardingChannel)
    }
}
