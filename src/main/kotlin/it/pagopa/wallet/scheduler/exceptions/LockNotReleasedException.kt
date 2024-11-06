package it.pagopa.wallet.scheduler.exceptions

class LockNotReleasedException(lockName: String, throwable: Throwable? = null) :
    RuntimeException("Could not release the lock [${lockName}]", throwable)
