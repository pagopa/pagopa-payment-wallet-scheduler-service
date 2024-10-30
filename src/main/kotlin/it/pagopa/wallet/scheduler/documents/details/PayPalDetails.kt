package it.pagopa.wallet.scheduler.documents.details

data class PayPalDetails(val maskedEmail: String?, val pspId: String, val pspBusinessName: String) :
    WalletDetails<PayPalDetails>
