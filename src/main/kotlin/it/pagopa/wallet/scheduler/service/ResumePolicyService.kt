package it.pagopa.wallet.scheduler.service

import java.time.Instant
import java.util.*

interface ResumePolicyService {
    fun getResumeTimestamp(target: String): Optional<Instant>
    fun saveResumeTimestamp(target: String, timestamp: Instant)
}
