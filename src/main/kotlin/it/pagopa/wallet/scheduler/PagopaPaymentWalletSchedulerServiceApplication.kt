package it.pagopa.wallet.scheduler

import it.pagopa.wallet.scheduler.config.WalletSearchConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import reactor.core.publisher.Hooks

@SpringBootApplication
@EnableConfigurationProperties(WalletSearchConfig::class)
class PagopaPaymentWalletSchedulerServiceApplication

fun main(args: Array<String>) {
    Hooks.enableAutomaticContextPropagation()
    runApplication<PagopaPaymentWalletSchedulerServiceApplication>(*args)
}
