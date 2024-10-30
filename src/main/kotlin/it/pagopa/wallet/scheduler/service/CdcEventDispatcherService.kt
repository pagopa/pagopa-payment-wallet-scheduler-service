package it.pagopa.wallet.scheduler.service

import it.pagopa.wallet.client.WalletQueueClient
import it.pagopa.wallet.scheduler.common.cdc.LoggingEvent
import it.pagopa.wallet.scheduler.common.tracing.TracingUtils
import it.pagopa.wallet.scheduler.config.properties.CdcQueueConfig
import it.pagopa.wallet.scheduler.config.properties.RetrySendPolicyConfig
import java.time.Duration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

@Component
class CdcEventDispatcherService(
    private val walletQueueClient: WalletQueueClient,
    private val tracingUtils: TracingUtils,
    private val cdcQueueConfig: CdcQueueConfig,
    private val retrySendPolicyConfig: RetrySendPolicyConfig
) {

    private val WALLET_CDC_EVENT_HANDLER_SPAN_NAME = "schedulerWalletEvent"
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun dispatchEvent(event: LoggingEvent?): Mono<LoggingEvent> =
        if (event != null) {
            Mono.defer {
                    logger.info(
                        "Handling new change stream event of type {} with id {} published on {}",
                        event.javaClass,
                        event.id,
                        event.timestamp
                    )
                    tracingUtils.traceMonoQueue(WALLET_CDC_EVENT_HANDLER_SPAN_NAME) { tracingInfo ->
                        walletQueueClient.sendWalletEvent(
                            event = event,
                            delay = Duration.ofSeconds(cdcQueueConfig.visibilityTimeoutWalletCdc),
                            tracingInfo = tracingInfo,
                        )
                    }
                }
                .retryWhen(
                    Retry.fixedDelay(
                            retrySendPolicyConfig.maxAttempts,
                            Duration.ofMillis(retrySendPolicyConfig.intervalInMs)
                        )
                        .filter { t -> t is Exception }
                        .doBeforeRetry { signal ->
                            logger.warn(
                                "Retrying writing event on CDC queue due to: ${signal.failure().message}"
                            )
                        }
                )
                .doOnError { e -> logger.error("Failed to send event after retries {}", e.message) }
                .map { event }
        } else {
            Mono.empty()
        }
}
