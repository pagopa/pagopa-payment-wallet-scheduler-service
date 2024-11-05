package it.pagopa.wallet.documents.wallets.details

data class PayPalDetails(val maskedEmail: String?, val pspId: String, val pspBusinessName: String) :
    WalletDetails<PayPalDetails>
