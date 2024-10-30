package it.pagopa.wallet.scheduler.common.queue

import it.pagopa.wallet.scheduler.common.cdc.LoggingEvent

data class CdcQueueEvent<T : LoggingEvent>(val data: T, val tracingInfo: TracingInfo? = null)
