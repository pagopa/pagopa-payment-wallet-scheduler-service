package it.pagopa.wallet.scheduler.config

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import it.pagopa.wallet.scheduler.common.tracing.TracingUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenTelemetryConfiguration {

    @Bean fun agentOpenTelemetrySDKInstance(): OpenTelemetry = GlobalOpenTelemetry.get()

    @Bean
    fun openTelemetryTracer(openTelemetry: OpenTelemetry): Tracer =
        openTelemetry.getTracer("pagopa-wallet-scheduler-service")

    @Bean
    fun tracingUtils(openTelemetry: OpenTelemetry, tracer: Tracer): TracingUtils =
        TracingUtils(openTelemetry, tracer)
}
