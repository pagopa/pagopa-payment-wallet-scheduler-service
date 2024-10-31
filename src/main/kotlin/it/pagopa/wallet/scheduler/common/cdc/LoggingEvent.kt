package it.pagopa.wallet.scheduler.common.cdc

sealed class LoggingEvent(open val id: String, open val timestamp: String)

sealed class WalletLoggingEvent(
    open val walletId: String,
    override val id: String,
    override val timestamp: String
) : LoggingEvent(id, timestamp)

data class WalletDeletedEvent(
    override val id: String,
    override val timestamp: String,
    override val walletId: String
) : WalletLoggingEvent(walletId, id, timestamp)

data class WalletApplicationsUpdatedEvent(
    override val id: String,
    override val timestamp: String,
    override val walletId: String,
    val updatedApplications: List<AuditWalletApplication>
) : WalletLoggingEvent(walletId, id, timestamp)

data class WalletOnboardCompletedEvent(
    override val id: String,
    override val timestamp: String,
    override val walletId: String,
    val auditWallet: AuditWallet
) : WalletLoggingEvent(walletId, id, timestamp)
