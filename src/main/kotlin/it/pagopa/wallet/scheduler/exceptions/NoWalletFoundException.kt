package it.pagopa.wallet.scheduler.exceptions

import java.time.Instant

class NoWalletFoundException(message: String) : RuntimeException(message) {

    constructor(
        startDate: Instant,
        endDate: Instant
    ) : this("No wallet found in time window: [$startDate] - [$endDate]")

    constructor() : this("No wallet found with the given criteria")
}
