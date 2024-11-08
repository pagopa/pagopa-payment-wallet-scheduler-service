package it.pagopa.wallet.scheduler.exceptions

sealed class LockException(message: String, throwable: Throwable? = null) :
    RuntimeException(message, throwable)
