package it.pagopa.wallet.scheduler.jobs.paymentwallet

import it.pagopa.wallet.scheduler.common.cdc.AuditWallet
import it.pagopa.wallet.scheduler.common.cdc.AuditWalletApplication
import it.pagopa.wallet.scheduler.common.cdc.AuditWalletDetails
import it.pagopa.wallet.scheduler.common.cdc.WalletOnboardCompletedEvent
import it.pagopa.wallet.scheduler.documents.details.CardDetails
import it.pagopa.wallet.scheduler.documents.details.PayPalDetails
import it.pagopa.wallet.scheduler.jobs.ScheduledJob
import it.pagopa.wallet.scheduler.jobs.config.OnboardedPaymentWalletJobConfiguration
import it.pagopa.wallet.scheduler.services.CdcEventDispatcherService
import it.pagopa.wallet.scheduler.services.WalletService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

/**
 * Payment wallet job: this job will scan all payment wallets in the given configuration retrieving
 * all the onboarded ones and populating the CDC queue
 */
@Component
class OnboardedPaymentWalletJob(
    @Autowired private val walletService: WalletService,
    @Autowired private val cdcEventDispatcherService: CdcEventDispatcherService
) : ScheduledJob<OnboardedPaymentWalletJobConfiguration> {
    override fun id(): String = "onboarded-payment-wallet-job"

    override fun process(configuration: OnboardedPaymentWalletJobConfiguration) {
        val startDate = configuration.startDate
        val endDate = configuration.endDate
        walletService
            .getWalletsForCdcIngestion(startDate = startDate, endDate = endDate)
            .flatMapMany { Flux.fromIterable(it) }
            .map {
                WalletOnboardCompletedEvent(
                    id = it.id,
                    timestamp = it.creationDate.toString(),
                    walletId = it.id,
                    auditWallet =
                        AuditWallet(
                            paymentMethodId = it.paymentMethodId,
                            creationDate = it.creationDate.toString(),
                            updateDate = it.updateDate.toString(),
                            applications =
                                it.applications.map { application ->
                                    AuditWalletApplication(
                                        id = application.id,
                                        creationDate = application.creationDate,
                                        updateDate = application.updateDate,
                                        metadata = application.metadata,
                                        status = application.status
                                    )
                                },
                            details =
                                it.details?.let { walletDetails ->
                                    when (walletDetails) {
                                        is CardDetails ->
                                            AuditWalletDetails(
                                                type = "CARDS", // TODO check this value
                                                cardBrand = walletDetails.brand,
                                                pspId = null
                                            )
                                        is PayPalDetails ->
                                            AuditWalletDetails(
                                                type = "PAYPAL", // TODO check this value
                                                cardBrand = null,
                                                pspId = walletDetails.pspId
                                            )
                                    }
                                },
                            status = it.status,
                            validationOperationId = null, // TODO is ok to pass it as null here?
                            validationOperationResult = it.validationOperationResult,
                            validationOperationTimestamp =
                                it.creationDate
                                    .toString(), // use the wallet creation date as validation
                            // operation timestamp
                            validationErrorCode = it.validationErrorCode,
                        )
                )
            }
    }
}
