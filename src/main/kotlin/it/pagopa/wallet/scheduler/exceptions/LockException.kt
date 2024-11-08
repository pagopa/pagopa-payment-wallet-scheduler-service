package it.pagopa.wallet.scheduler.exceptions

open class LockException(message: String, throwable: Throwable? = null) :
    RuntimeException(message, throwable)
