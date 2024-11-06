package it.pagopa.wallet.scheduler.exceptions

class LockNotAcquiredException(lockName: String, throwable: Throwable? = null) :
    RuntimeException("Could not acquire the lock [${lockName}]", throwable)
