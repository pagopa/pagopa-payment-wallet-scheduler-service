package it.pagopa.wallet.scheduler.services

import java.time.Instant
import java.util.*

interface ResumePolicyService {
    fun getResumeTimestamp(target: String): Optional<Instant>
    fun saveResumeTimestamp(target: String, timestamp: Instant)
}
