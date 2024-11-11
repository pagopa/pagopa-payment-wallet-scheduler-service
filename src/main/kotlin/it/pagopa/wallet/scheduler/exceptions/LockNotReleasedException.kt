package it.pagopa.wallet.scheduler.exceptions

class LockNotReleasedException(lockName: String, throwable: Throwable? = null) :
    LockException("Could not release the lock [${lockName}]", throwable)
