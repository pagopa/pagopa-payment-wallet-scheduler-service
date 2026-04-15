package it.pagopa.wallet.scheduler.repositories.redis

import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.lang.NonNull

data class ResumeTimestamp(
    @param:NonNull @Id val id: String,
    val timestamp: Instant = Instant.EPOCH,
)
