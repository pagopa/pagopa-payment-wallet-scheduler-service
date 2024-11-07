package it.pagopa.wallet.documents.wallets

data class WalletApplication(
    val id: String,
    val status: String,
    val creationDate: String,
    val updateDate: String,
    val metadata: Map<String, String?>
)
