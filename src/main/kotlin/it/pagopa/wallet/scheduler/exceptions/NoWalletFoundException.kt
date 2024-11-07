package it.pagopa.wallet.scheduler.exceptions

import java.time.Instant

class NoWalletFoundException(startDate: Instant, endDate: Instant) :
    RuntimeException("No wallet found in time window: [$startDate] - [$endDate]")
