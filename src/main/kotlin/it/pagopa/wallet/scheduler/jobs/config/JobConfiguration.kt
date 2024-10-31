package it.pagopa.wallet.scheduler.jobs.config

/**
 * Job configuration common parameter class: every job have its own configuration class
 * that will extend
 */
sealed class JobConfiguration(val jobId: String)