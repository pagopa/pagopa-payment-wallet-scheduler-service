package it.pagopa.wallet.scheduler.exceptions

class SemNotAcquiredException(semName: String, throwable: Throwable? = null) :
    LockException("Could not acquire the semaphore [${semName}]", throwable)
