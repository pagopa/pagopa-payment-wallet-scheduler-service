package it.pagopa.wallet.scheduler.exceptions

import java.time.Instant

class WalletInvalidRangeException(startDate: Instant, endDate: Instant) :
    RuntimeException("End date [$endDate] is before start date [$startDate]")
