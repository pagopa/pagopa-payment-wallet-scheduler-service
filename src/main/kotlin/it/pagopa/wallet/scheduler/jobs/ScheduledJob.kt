package it.pagopa.wallet.scheduler.jobs

import it.pagopa.wallet.scheduler.jobs.config.JobConfiguration

/** Generic scheduled job common interface */
interface ScheduledJob<T> where T : JobConfiguration {

    fun id(): String

    fun process(configuration: T)
}
