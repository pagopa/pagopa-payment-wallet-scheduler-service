package it.pagopa.wallet.scheduler.jobs.lifecyclemanagement

import io.opentelemetry.api.common.Attributes
import it.pagopa.wallet.scheduler.common.tracing.TracingUtils
import it.pagopa.wallet.scheduler.jobs.config.LifecycleManagementJobConfiguration
import it.pagopa.wallet.scheduler.services.LifecycleManagementService
import it.pagopa.wallet.scheduler.utils.LifeCycleTracerUtils
import java.time.Instant
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class UpdateTtlWalletJobTest {

    private val lifecycleManagementService: LifecycleManagementService = mock()
    private val tracingUtils: TracingUtils = mock()

    private val updateTtlWalletJob = UpdateTtlWalletJob(lifecycleManagementService, tracingUtils)

    @Test
    fun `Should execute process successfully`() {
        // pre-requisites
        val endDate = Instant.now()
        val lastProcessedTimestamp = endDate.toString()
        val lifecycleManagementJobConfiguration = LifecycleManagementJobConfiguration(endDate)

        given(lifecycleManagementService.setWalletsTtl(any()))
            .willReturn(
                Mono.just(
                    LifecycleManagementService.SetWalletsTtlResult(
                        updatedWallets = 1,
                        lastProcessedTimestamp = lastProcessedTimestamp
                    )
                )
            )
        doNothing().`when`(tracingUtils).addSpan(anyOrNull(), anyOrNull())

        // Test
        StepVerifier.create(updateTtlWalletJob.process(lifecycleManagementJobConfiguration))
            .expectNext(1)
            .verifyComplete()

        // verifications
        verify(lifecycleManagementService, times(1)).setWalletsTtl(endDate)
        val attributesCaptor = argumentCaptor<Attributes>()
        verify(tracingUtils, times(1))
            .addSpan(eq("payWalletLifeCycleSession"), attributesCaptor.capture())

        val attrs = attributesCaptor.firstValue
        val keys =
            LifeCycleTracerUtils.WalletLifecycleSessionStats(
                totalItem = 0,
                elapsedTime = 0,
                lastProcessedTimestamp = ""
            )
        assertEquals(1L, attrs.get(keys.WALLET_LIFECYCLE_SESSION_TOTAL_ITEM_KEY))
        assertEquals(
            lastProcessedTimestamp,
            attrs.get(keys.WALLET_LIFECYCLE_SESSION_LAST_PROCESSED_TIMESTAMP_KEY)
        )
    }

    @Test
    fun `Should return the id of the job`() {
        // Test
        assertEquals(updateTtlWalletJob.id(), "lifecycle-management-wallet-job")
    }
}
