package it.pagopa.wallet.scheduler.repositories.redis

import java.time.OffsetDateTime
import org.springframework.data.annotation.Id
import org.springframework.lang.NonNull

/**
 * Exclusive lock Redis document: this document is used to gain and hold information about
 * applicative locks
 *
 * @param id lock id: id that uniquely identifies the lock
 * @param creationDate lock creation date
 * @param holderName name of the application/process that hold this lock
 */
data class ExclusiveLockDocument(
    @param:NonNull @Id val id: String,
    val creationDate: String?,
    val holderName: String?
) {
    /**
     * Convenience constructor that set creation date to now
     *
     * @param id lock id: id that uniquely identifies the lock
     * @param holderName name of the application/process that hold this lock
     */
    constructor(
        id: String,
        holderName: String?
    ) : this(id, OffsetDateTime.now().toString(), holderName)
}
