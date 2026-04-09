package it.pagopa.wallet.scheduler.exceptions

import it.pagopa.wallet.scheduler.repositories.redis.ExclusiveLockDocument

class LockNotAcquiredException(jobName: String, exclusiveLockDocument: ExclusiveLockDocument) :
    RuntimeException(
        "Lock not acquired for job with name: [$jobName] and locking key: [${exclusiveLockDocument.id}]"
    )
