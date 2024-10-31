package it.pagopa.wallet.scheduler.jobs

/**
 * Scheduled Job common interface
 */
interface ScheduledJob {

    fun id(): String

    fun process(configuration: JobConfiguration)
}