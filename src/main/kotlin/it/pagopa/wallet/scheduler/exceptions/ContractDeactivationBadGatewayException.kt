package it.pagopa.wallet.scheduler.exceptions

class ContractDeactivationBadGatewayException(walletId: String, cause: Throwable) :
    WalletServiceException(walletId, cause)
