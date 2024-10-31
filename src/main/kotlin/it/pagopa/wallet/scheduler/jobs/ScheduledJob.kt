package it.pagopa.wallet.scheduler.jobs

import it.pagopa.wallet.scheduler.jobs.config.JobConfiguration
import reactor.core.publisher.Mono

/** Generic scheduled job common interface */
interface ScheduledJob<T, V> where T : JobConfiguration {

    fun id(): String

    fun process(configuration: T): Mono<V>
}
