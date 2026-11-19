package it.pagopa.wallet.scheduler.exceptions

open class WalletServiceException(walletId: String, throwable: Throwable) :
    RuntimeException("Wallet service exception for walletId [$walletId]", throwable)
