package it.pagopa.wallet.scheduler.exceptions

class SemNotReleasedException(semName: String, throwable: Throwable? = null) :
    LockException("Could not release the semaphore [${semName}]", throwable)
