package it.pagopa.wallet.scheduler.scheduledjob

import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementExecutionConfig
import it.pagopa.wallet.scheduler.exceptions.LockNotAcquiredException
import it.pagopa.wallet.scheduler.jobs.lifecyclemanagement.UpdateTtlWalletJob
import it.pagopa.wallet.scheduler.repositories.redis.ExclusiveLockDocument
import it.pagopa.wallet.scheduler.services.SchedulerLockService
import java.time.Duration
import kotlin.test.Test
import kotlinx.coroutines.reactor.mono
import org.mockito.kotlin.after
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import reactor.core.publisher.Mono

class LifeCycleManagementScheduledJobTest {

    private val updateTtlWalletJob: UpdateTtlWalletJob = mock()
    private val jobConfiguration: LifecycleManagementExecutionConfig =
        LifecycleManagementExecutionConfig(90, 30)
    private val schedulerLockService: SchedulerLockService = mock()

    private val lifeCycleManagementScheduledJob: LifeCycleManagementScheduledJob =
        LifeCycleManagementScheduledJob(updateTtlWalletJob, jobConfiguration, schedulerLockService)

    @Test
    fun `Should execute batch successfully`() {
        val jobId = "jobId"
        val lockDocument = ExclusiveLockDocument(jobId, "test")
        given(schedulerLockService.acquireJobLock(any(), any())).willReturn(mono { lockDocument })
        given(schedulerLockService.releaseJobLock(any())).willReturn(mono { true })
        given(updateTtlWalletJob.process(any())).willReturn(Mono.just<Int>(10))
        given(updateTtlWalletJob.id()).willReturn(jobId)

        // Test the process
        lifeCycleManagementScheduledJob.processLifeCycleWallets()

        verify(updateTtlWalletJob, after(1000).times(1)).id()
        verify(updateTtlWalletJob, after(1000).times(1)).process(any())

        verify(schedulerLockService, times(1)).acquireJobLock(jobId, Duration.ofSeconds(30))
        verify(schedulerLockService, times(1)).releaseJobLock(lockDocument)
    }

    @Test
    fun `Should handle process exception during the process`() {
        val jobId = "jobId"
        val lockDocument = ExclusiveLockDocument(jobId, "test")
        given(schedulerLockService.acquireJobLock(any(), any())).willReturn(mono { lockDocument })
        given(schedulerLockService.releaseJobLock(any())).willReturn(mono { true })
        given(updateTtlWalletJob.process(any()))
            .willReturn(Mono.error(RuntimeException("Error during the process")))
        given(updateTtlWalletJob.id()).willReturn(jobId)

        // Test the process
        lifeCycleManagementScheduledJob.processLifeCycleWallets()

        verify(updateTtlWalletJob, after(1000).times(1)).id()
        verify(updateTtlWalletJob, after(1000).times(1)).process(any())

        verify(schedulerLockService, times(1)).acquireJobLock(jobId, Duration.ofSeconds(30))
        verify(schedulerLockService, times(1)).releaseJobLock(lockDocument)
    }

    @Test
    fun `Should not call process if the lock is not acquired`() {
        val jobId = "jobId"
        val lockDocument = ExclusiveLockDocument(jobId, "test")
        given(schedulerLockService.acquireJobLock(any(), any()))
            .willReturn(Mono.error(LockNotAcquiredException(jobId, lockDocument)))
        given(schedulerLockService.releaseJobLock(any())).willReturn(null)
        given(updateTtlWalletJob.process(any()))
            .willReturn(Mono.error(RuntimeException("Error during the process")))
        given(updateTtlWalletJob.id()).willReturn(jobId)

        // Test the process
        lifeCycleManagementScheduledJob.processLifeCycleWallets()

        verify(updateTtlWalletJob, after(1000).times(1)).id()
        verify(updateTtlWalletJob, after(1000).times(0)).process(any())

        verify(schedulerLockService, times(1)).acquireJobLock(jobId, Duration.ofSeconds(30))
        verify(schedulerLockService, times(0)).releaseJobLock(lockDocument)
    }
}
