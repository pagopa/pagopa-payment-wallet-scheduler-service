package it.pagopa.wallet.scheduler.scheduledjob

import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementConfiguration
import it.pagopa.wallet.scheduler.exceptions.SemNotAcquiredException
import it.pagopa.wallet.scheduler.jobs.lifecyclemanagement.UpdateTtlWalletJob
import it.pagopa.wallet.scheduler.service.SchedulerLockService
import kotlinx.coroutines.reactor.mono
import org.mockito.kotlin.after
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import reactor.core.publisher.Mono
import kotlin.test.Test

class LifeCycleManagementScheduledJobTest {

    private val updateTtlWalletJob: UpdateTtlWalletJob = mock()
    private val jobConfiguration: LifecycleManagementConfiguration = LifecycleManagementConfiguration(90)   // 3 mounth excluded
    private val schedulerLockService: SchedulerLockService = mock()

   private val lifeCycleManagementScheduledJob : LifeCycleManagementScheduledJob =
       LifeCycleManagementScheduledJob(updateTtlWalletJob,jobConfiguration,schedulerLockService)

   @Test
   fun `Should execute batch successfully`() {
       val jobId = "jobId"
       val semaphoreId = "semaphore-id"
       given(schedulerLockService.acquireJobSemaphore(any())).willReturn(mono { semaphoreId })
       given(schedulerLockService.releaseJobSemaphore(any(), any())).willReturn(null)
       given(updateTtlWalletJob.process(any())).willReturn(Mono.just<Int>(10))
       given(updateTtlWalletJob.id()).willReturn(jobId)

       // Test the process
       lifeCycleManagementScheduledJob.processLifeCycleWallets()

       verify(updateTtlWalletJob, after(1000).times(2)).id()
       verify(updateTtlWalletJob, after(1000).times(1)).process(any())

       verify(schedulerLockService,times(1)).acquireJobSemaphore(jobId)
       verify(schedulerLockService,times(1)).releaseJobSemaphore(jobId,semaphoreId)

   }

    @Test
    fun `Should handle process exception during the process`() {
        val jobId = "jobId"
        val semaphoreId = "semaphore-id"
        given(schedulerLockService.acquireJobSemaphore(any())).willReturn(mono { semaphoreId })
        given(schedulerLockService.releaseJobSemaphore(any(), any())).willReturn(null)
        given(updateTtlWalletJob.process(any())).willReturn(Mono.error(RuntimeException("Error during the process")))
        given(updateTtlWalletJob.id()).willReturn(jobId)

        // Test the process
        lifeCycleManagementScheduledJob.processLifeCycleWallets()

        verify(updateTtlWalletJob, after(1000).times(2)).id()
        verify(updateTtlWalletJob, after(1000).times(1)).process(any())

        verify(schedulerLockService,times(1)).acquireJobSemaphore(jobId)
        verify(schedulerLockService,times(1)).releaseJobSemaphore(jobId,semaphoreId)

    }


    @Test
    fun `Should not call process if the lock is not acquired`() {
        val jobId = "jobId"
        val semaphoreId = "semaphore-id"
        given(schedulerLockService.acquireJobSemaphore(any())).willReturn(Mono.error(SemNotAcquiredException("jobName")))
        given(schedulerLockService.releaseJobSemaphore(any(), any())).willReturn(null)
        given(updateTtlWalletJob.process(any())).willReturn(Mono.error(RuntimeException("Error during the process")))
        given(updateTtlWalletJob.id()).willReturn(jobId)

        // Test the process
        lifeCycleManagementScheduledJob.processLifeCycleWallets()

        verify(updateTtlWalletJob, after(1000).times(1)).id()
        verify(updateTtlWalletJob, after(1000).times(0)).process(any())

        verify(schedulerLockService,times(1)).acquireJobSemaphore(jobId)
        verify(schedulerLockService,times(0)).releaseJobSemaphore(jobId,semaphoreId)

    }


}