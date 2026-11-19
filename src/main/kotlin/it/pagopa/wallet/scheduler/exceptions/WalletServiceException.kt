package it.pagopa.wallet.scheduler.exceptions

class WalletServiceException(walletId: String, throwable: Throwable) :
    RuntimeException("Wallet service exception for walletId [$walletId]", throwable)
