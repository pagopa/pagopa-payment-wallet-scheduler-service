package it.pagopa.wallet.scheduler.service

import java.time.Instant

interface ResumePolicyService {
    fun getResumeTimestamp(): Instant
    fun saveResumeTimestamp(timestamp: Instant)
}
