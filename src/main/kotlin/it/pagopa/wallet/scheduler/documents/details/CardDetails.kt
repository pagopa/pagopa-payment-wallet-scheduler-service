package it.pagopa.wallet.scheduler.documents.details

data class CardDetails(
    val type: String,
    val bin: String,
    val lastFourDigits: String,
    val expiryDate: String,
    val brand: String,
    val paymentInstrumentGatewayId: String
) : WalletDetails<CardDetails>
