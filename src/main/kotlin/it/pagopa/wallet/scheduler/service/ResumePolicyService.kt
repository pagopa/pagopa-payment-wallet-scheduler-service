package it.pagopa.wallet.scheduler.service

import java.time.Instant

interface ResumePolicyService {
    fun getResumeTimestamp(target: String): Instant
    fun saveResumeTimestamp(target: String, timestamp: Instant)
}
