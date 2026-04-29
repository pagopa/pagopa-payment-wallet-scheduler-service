package it.pagopa.wallet.scheduler.utils

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes

class LifeCycleTracerUtils {

    data class WalletLifecycleSessionStats(
        val totalItem: Long,
        val elapsedTime: Long,
        val lastProcessedTimestamp: String
    ) {
        val WALLET_LIFECYCLE_SESSION_SPAN_NAME = "payWalletLifeCycleSession"
        val WALLET_LIFECYCLE_SESSION_ELAPSED_TIME_MS_KEY =
            AttributeKey.longKey("payWallet.lifeCycle.session.elapsedTime")
        val WALLET_LIFECYCLE_SESSION_TOTAL_ITEM_KEY =
            AttributeKey.longKey("payWallet.lifeCycle.session.totalItems")
        val WALLET_LIFECYCLE_SESSION_LAST_PROCESSED_TIMESTAMP_KEY =
            AttributeKey.stringKey("payWallet.lifeCycle.session.lastProcessedTimestamp")

        fun getSpanAttributes(
            totalItem: Long,
            elapsedTime: Long,
            lastProcessedTimestamp: String
        ): Attributes =
            Attributes.of(
                WALLET_LIFECYCLE_SESSION_TOTAL_ITEM_KEY,
                totalItem,
                WALLET_LIFECYCLE_SESSION_ELAPSED_TIME_MS_KEY,
                elapsedTime,
                WALLET_LIFECYCLE_SESSION_LAST_PROCESSED_TIMESTAMP_KEY,
                lastProcessedTimestamp
            )
    }

    data class WalletLifecycleItemStats(val status: String, val ttlApplied: Long) {
        val WALLET_LIFECYCLE_ITEM_SPAN_NAME = "payWalletLifeCycleItem"
        val WALLET_LIFECYCLE_ITEM_STATUS_KEY =
            AttributeKey.stringKey("payWallet.lifeCycle.item.wallet_Status")
        val WALLET_LIFECYCLE_ITEM_TTL_APPLIED_KEY =
            AttributeKey.longKey("payWallet.lifeCycle.item.ttlApplied")

        fun getSpanAttributes(status: String, ttlApplied: Long): Attributes =
            Attributes.of(
                WALLET_LIFECYCLE_ITEM_STATUS_KEY,
                status,
                WALLET_LIFECYCLE_ITEM_TTL_APPLIED_KEY,
                ttlApplied
            )
    }
}
