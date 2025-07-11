package it.pagopa.wallet.scheduler.jobs.paymentwallet

import it.pagopa.wallet.documents.wallets.Wallet
import it.pagopa.wallet.documents.wallets.details.CardDetails
import it.pagopa.wallet.documents.wallets.details.PayPalDetails
import it.pagopa.wallet.scheduler.common.cdc.*
import it.pagopa.wallet.scheduler.exceptions.NoWalletFoundException
import it.pagopa.wallet.scheduler.jobs.ScheduledJob
import it.pagopa.wallet.scheduler.jobs.config.OnboardedPaymentWalletJobConfiguration
import it.pagopa.wallet.scheduler.services.CdcEventDispatcherService
import it.pagopa.wallet.scheduler.services.RedisResumePolicyService
import it.pagopa.wallet.scheduler.services.WalletService
import java.time.Instant
import java.time.OffsetDateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * Payment wallet job: this job will scan all payment wallets in the given configuration retrieving
 * all the onboarded ones and populating the CDC queue
 */
@Component
class OnboardedPaymentWalletJob(
    @Autowired private val walletService: WalletService,
    @Autowired private val cdcEventDispatcherService: CdcEventDispatcherService,
    @Autowired private val redisResumePolicyService: RedisResumePolicyService
) : ScheduledJob<OnboardedPaymentWalletJobConfiguration, String> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun id(): String = "onboarded-payment-wallet-job"

    override fun process(configuration: OnboardedPaymentWalletJobConfiguration): Mono<String> {
        val startDate = configuration.startDate
        val endDate = configuration.endDate
        logger.info("Starting payment wallet processing in time window {} - {}", startDate, endDate)
        return Mono.fromCallable { redisResumePolicyService.getResumeTimestamp(id()) }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany {
                walletService.getWalletsForCdcIngestion(
                    startDate = it.orElse(startDate),
                    endDate = endDate
                )
            }
            .switchIfEmpty(
                Flux.error(NoWalletFoundException(startDate = startDate, endDate = endDate))
            )
            .map { getWalletOnboardCompletedEvent(it) }
            .flatMap { cdcEventDispatcherService.dispatchEvent(it) }
            .collectSortedList(compareBy<LoggingEvent> { OffsetDateTime.parse(it.timestamp) })
            .flatMap {
                Mono.fromCallable {
                        redisResumePolicyService.saveResumeTimestamp(
                            id(),
                            Instant.parse(it.last().timestamp)
                        )
                    }
                    .subscribeOn(Schedulers.boundedElastic())
                    .thenReturn(it.last().timestamp)
            }
            .doOnError {
                logger.error(
                    "Error processing payment wallet chunk in time window: $startDate - $endDate",
                    it
                )
            }
    }

    private fun getWalletOnboardCompletedEvent(wallet: Wallet): WalletOnboardCompletedEvent {
        return WalletOnboardCompletedEvent(
            id = wallet.id,
            timestamp = wallet.creationDate.toString(),
            walletId = wallet.id,
            auditWallet =
                AuditWallet(
                    paymentMethodId = wallet.paymentMethodId,
                    creationDate = wallet.creationDate.toString(),
                    updateDate = wallet.updateDate.toString(),
                    applications =
                        wallet.applications.map { application ->
                            AuditWalletApplication(
                                id = application.id,
                                creationDate = application.creationDate,
                                updateDate = application.updateDate,
                                metadata = application.metadata,
                                status = application.status
                            )
                        },
                    details =
                        wallet.details?.let { walletDetails ->
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
                    status = wallet.status,
                    validationOperationId = null, // TODO is ok to pass it as null here?
                    validationOperationResult = wallet.validationOperationResult,
                    validationOperationTimestamp =
                        wallet.creationDate
                            .toString(), // use the wallet creation date as validation
                    // operation timestamp
                    validationErrorCode = wallet.validationErrorCode,
                )
        )
    }
}
