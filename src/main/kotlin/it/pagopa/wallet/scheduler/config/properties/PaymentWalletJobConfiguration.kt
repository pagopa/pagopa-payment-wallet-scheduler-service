package it.pagopa.wallet.scheduler.config.properties

import java.time.Instant
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("payment-wallet-job.execution")
class PaymentWalletJobConfiguration(val startDate: Instant, val endDate: Instant)
