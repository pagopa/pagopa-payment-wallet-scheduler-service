package it.pagopa.wallet.scheduler.jobs.lifecyclemanagement

import it.pagopa.wallet.scheduler.jobs.config.LifecycleManagementJobConfiguration
import it.pagopa.wallet.scheduler.services.LifecycleManagementService
import java.time.Instant
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import reactor.core.publisher.Mono

class UpdateTtlWalletJobTest {

    private val lifecycleManagementService: LifecycleManagementService = mock()

    private val updateTtlWalletJob = UpdateTtlWalletJob(lifecycleManagementService)

    @Test
    fun `Should execute process successfully`() {
        // pre-requisites
        val endDate = Instant.now()
        val lifecycleManagementJobConfiguration = LifecycleManagementJobConfiguration(endDate)

        given(lifecycleManagementService.setWalletsTtl(any())).willReturn(Mono.just(1))

        // Test
        updateTtlWalletJob.process(lifecycleManagementJobConfiguration)

        // verifications
        verify(lifecycleManagementService, times(1)).setWalletsTtl(endDate)
    }

    @Test
    fun `Should return the id of the job`() {
        // Test
        assertEquals(updateTtlWalletJob.id(), "lifecycle-management-wallet-job")
    }
}
