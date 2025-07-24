package it.pagopa.wallet.scheduler.services

import java.time.Instant
import java.util.*
import reactor.core.publisher.Mono

interface ResumePolicyService {
    fun getResumeTimestamp(target: String): Mono<Instant>

    fun saveResumeTimestamp(target: String, timestamp: Instant)
}
