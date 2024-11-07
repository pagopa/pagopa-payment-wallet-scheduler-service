package it.pagopa.wallet.scheduler.exceptions

class SemNotReleasedException(semName: String, throwable: Throwable? = null) :
    RuntimeException("Could not release the semaphore [${semName}]", throwable)
