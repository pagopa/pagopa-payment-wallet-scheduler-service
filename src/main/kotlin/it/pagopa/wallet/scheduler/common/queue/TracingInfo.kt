package it.pagopa.wallet.scheduler.common.queue

data class TracingInfo(
    val traceparent: String? = "",
    val tracestate: String? = "",
    val baggage: String? = ""
)
