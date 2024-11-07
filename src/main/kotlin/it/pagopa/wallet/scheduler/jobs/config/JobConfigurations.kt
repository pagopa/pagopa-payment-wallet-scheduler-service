package it.pagopa.wallet.scheduler.jobs.config

import java.time.Instant

/**
 * Job configuration common parameter class: every job have its own configuration class that will
 * extend
 */
sealed class JobConfiguration

/** Payment wallet job configuration */
data class OnboardedPaymentWalletJobConfiguration(val startDate: Instant, val endDate: Instant) :
    JobConfiguration()
