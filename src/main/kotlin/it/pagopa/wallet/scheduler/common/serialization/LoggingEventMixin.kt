package it.pagopa.wallet.scheduler.common.serialization

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import it.pagopa.wallet.scheduler.common.cdc.WalletApplicationsUpdatedEvent
import it.pagopa.wallet.scheduler.common.cdc.WalletDeletedEvent
import it.pagopa.wallet.scheduler.common.cdc.WalletOnboardCompletedEvent
import it.pagopa.wallet.scheduler.common.serialization.LoggingEventMixin.Companion.WALLET_APPLICATIONS_UPDATE_TYPE
import it.pagopa.wallet.scheduler.common.serialization.LoggingEventMixin.Companion.WALLET_DELETED_TYPE
import it.pagopa.wallet.scheduler.common.serialization.LoggingEventMixin.Companion.WALLET_ONBOARD_COMPLETE_TYPE

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_class")
@JsonSubTypes(
    JsonSubTypes.Type(value = WalletDeletedEvent::class, name = WALLET_DELETED_TYPE),
    JsonSubTypes.Type(
        value = WalletOnboardCompletedEvent::class,
        name = WALLET_ONBOARD_COMPLETE_TYPE
    ),
    JsonSubTypes.Type(
        value = WalletApplicationsUpdatedEvent::class,
        name = WALLET_APPLICATIONS_UPDATE_TYPE
    )
)
interface LoggingEventMixin {
    @JsonProperty("_id") fun getId(): String?
    companion object {
        const val WALLET_APPLICATIONS_UPDATE_TYPE =
            "it.pagopa.wallet.audit.WalletApplicationsUpdatedEvent"
        const val WALLET_DELETED_TYPE = "it.pagopa.wallet.audit.WalletDeletedEvent"
        const val WALLET_ONBOARD_COMPLETE_TYPE =
            "it.pagopa.wallet.audit.WalletOnboardCompletedEvent"
    }
}
