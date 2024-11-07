package it.pagopa.wallet.scheduler.jobs.paymentwallet

import it.pagopa.wallet.documents.wallets.Wallet
import it.pagopa.wallet.documents.wallets.details.CardDetails
import it.pagopa.wallet.documents.wallets.details.PayPalDetails
import it.pagopa.wallet.scheduler.common.cdc.AuditWallet
import it.pagopa.wallet.scheduler.common.cdc.AuditWalletApplication
import it.pagopa.wallet.scheduler.common.cdc.AuditWalletDetails
import it.pagopa.wallet.scheduler.common.cdc.WalletOnboardCompletedEvent
import it.pagopa.wallet.scheduler.exceptions.NoWalletFoundException
import it.pagopa.wallet.scheduler.jobs.ScheduledJob
import it.pagopa.wallet.scheduler.jobs.config.OnboardedPaymentWalletJobConfiguration
import it.pagopa.wallet.scheduler.services.CdcEventDispatcherService
import it.pagopa.wallet.scheduler.services.WalletService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Payment wallet job: this job will scan all payment wallets in the given configuration retrieving
 * all the onboarded ones and populating the CDC queue
 */
@Component
class OnboardedPaymentWalletJob(
    @Autowired private val walletService: WalletService,
    @Autowired private val cdcEventDispatcherService: CdcEventDispatcherService
) : ScheduledJob<OnboardedPaymentWalletJobConfiguration, String> {

    private val logger = LoggerFactory.getLogger(javaClass)
    override fun id(): String = "onboarded-payment-wallet-job"

    override fun process(configuration: OnboardedPaymentWalletJobConfiguration): Mono<String> {
        /*
           TODO: add redis checkpoint logic here to retrieve the latest processed wallet
           recover from redis the latest sent event date
           if null -> use start date
           else -> use valued last created wallet processed date (sliding window)
        */
        val startDate = configuration.startDate
        val endDate = configuration.endDate
        logger.info("Starting payment wallet processing in time window {} - {}", startDate, endDate)
        return walletService
            .getWalletsForCdcIngestion(startDate = startDate, endDate = endDate)
            .switchIfEmpty {
                Flux.error<Wallet>(NoWalletFoundException(startDate = startDate, endDate = endDate))
            }
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
            .flatMap { cdcEventDispatcherService.dispatchEvent(it) }
            .collectList()
            .map {
                it.last().timestamp // <- this is the wallet creation date and can be used to save
                // it to
                // Redis as latest processed chunk
                // TODO: add redis checkpoint save here with the latest saved wallet date
            }
            .doOnError {
                logger.error(
                    "Error processing payment wallet chunk in time window: $startDate - $endDate",
                    it
                )
            }
    }
}
