package it.pagopa.wallet.scheduler.scheduledjob

import it.pagopa.wallet.scheduler.config.properties.PaymentWalletJobConfiguration
import it.pagopa.wallet.scheduler.jobs.config.OnboardedPaymentWalletJobConfiguration
import it.pagopa.wallet.scheduler.jobs.paymentwallet.OnboardedPaymentWalletJob
import java.time.Duration
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class PaymentWalletScheduledJob(
    @Autowired private val onboardedPaymentWalletJob: OnboardedPaymentWalletJob,
    @Autowired private val paymentWalletJobConfiguration: PaymentWalletJobConfiguration
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${paymentWalletJob.execution.cron}")
    fun processOnboardedPaymentWallets() {
        val startTime = Instant.now()
        onboardedPaymentWalletJob
            .process(
                OnboardedPaymentWalletJobConfiguration(
                    startDate = paymentWalletJobConfiguration.startDate,
                    endDate = paymentWalletJobConfiguration.endDate
                )
            )
            .subscribe(
                {},
                {},
                {
                    logger.info(
                        "Overall processing completed. Elapsed time: [{}]",
                        Duration.between(startTime, Instant.now())
                    )
                }
            )
    }
}
