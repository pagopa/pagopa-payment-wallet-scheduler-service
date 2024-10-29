package it.pagopa.wallet.scheduler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import reactor.core.publisher.Hooks

@SpringBootApplication class PagopaPaymentWalletSchedulerServiceApplication

fun main(args: Array<String>) {
    Hooks.enableAutomaticContextPropagation()
    runApplication<PagopaPaymentWalletSchedulerServiceApplication>(*args)
}
