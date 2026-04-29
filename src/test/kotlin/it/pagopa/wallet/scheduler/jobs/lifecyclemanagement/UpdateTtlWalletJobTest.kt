package it.pagopa.wallet.scheduler.jobs.lifecyclemanagement

import it.pagopa.wallet.scheduler.common.tracing.TracingUtils
import it.pagopa.wallet.scheduler.jobs.config.LifecycleManagementJobConfiguration
import it.pagopa.wallet.scheduler.services.LifecycleManagementService
import java.time.Instant
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doNothing
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
        val lifecycleManagementJobConfiguration = LifecycleManagementJobConfiguration(endDate)

        given(lifecycleManagementService.setWalletsTtl(any())).willReturn(Mono.just(1))
        doNothing().`when`(tracingUtils).addSpan(anyOrNull(), anyOrNull())

        // Test
        StepVerifier.create(updateTtlWalletJob.process(lifecycleManagementJobConfiguration))
            .expectNext(1)
            .verifyComplete()

        // verifications
        verify(lifecycleManagementService, times(1)).setWalletsTtl(endDate)
        verify(tracingUtils, times(1)).addSpan(anyOrNull(), anyOrNull())
    }

    @Test
    fun `Should return the id of the job`() {
        // Test
        assertEquals(updateTtlWalletJob.id(), "lifecycle-management-wallet-job")
    }
}
