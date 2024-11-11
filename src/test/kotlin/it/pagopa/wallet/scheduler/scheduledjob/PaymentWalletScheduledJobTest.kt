package it.pagopa.wallet.scheduler.scheduledjob

import it.pagopa.wallet.scheduler.config.properties.PaymentWalletJobConfiguration
import it.pagopa.wallet.scheduler.exceptions.SemNotAcquiredException
import it.pagopa.wallet.scheduler.jobs.config.OnboardedPaymentWalletJobConfiguration
import it.pagopa.wallet.scheduler.jobs.paymentwallet.OnboardedPaymentWalletJob
import it.pagopa.wallet.scheduler.service.SchedulerLockService
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.reactor.mono
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono

class PaymentWalletScheduledJobTest {
    private val startDate = Instant.now()
    private val onboardedPaymentWalletJob: OnboardedPaymentWalletJob = mock()
    private val schedulerLockService: SchedulerLockService = mock()
    private val paymentWalletJobConfiguration =
        PaymentWalletJobConfiguration(
            startDate = startDate,
            endDate = startDate + Duration.ofMinutes(10)
        )

    private val paymentWalletScheduledJob =
        PaymentWalletScheduledJob(
            onboardedPaymentWalletJob = onboardedPaymentWalletJob,
            paymentWalletJobConfiguration = paymentWalletJobConfiguration,
            schedulerLockService = schedulerLockService
        )

    @Test
    fun `Should execute batch successfully`() {
        // pre-requisites
        val jobId = "jobId"
        val semaphoreId = "semaphore-id"
        given(onboardedPaymentWalletJob.process(configuration = any()))
            .willReturn(mono { Instant.now().toString() })
        given(onboardedPaymentWalletJob.id()).willReturn(jobId)
        given(schedulerLockService.acquireJobSemaphore(any())).willReturn(mono { semaphoreId })
        given(schedulerLockService.releaseJobSemaphore(any(), any())).willReturn(null)

        // Test
        paymentWalletScheduledJob.processOnboardedPaymentWallets()
        // verifications
        verify(onboardedPaymentWalletJob, after(1000).times(2)).id()
        verify(onboardedPaymentWalletJob, after(1000).times(1))
            .process(
                configuration =
                    OnboardedPaymentWalletJobConfiguration(
                        startDate = paymentWalletJobConfiguration.startDate,
                        endDate = paymentWalletJobConfiguration.endDate
                    )
            )
        verify(schedulerLockService, times(1)).acquireJobSemaphore(jobId)
        verify(schedulerLockService, times(1)).releaseJobSemaphore(jobId, semaphoreId)
    }

    @Test
    fun `Should handle processing exception`() {
        // pre-requisites
        val jobId = "jobId"
        val semaphoreId = "semaphore-id"
        given(onboardedPaymentWalletJob.process(configuration = any()))
            .willReturn(Mono.error(RuntimeException("Exception during job execution")))
        given(onboardedPaymentWalletJob.id()).willReturn(jobId)
        given(schedulerLockService.acquireJobSemaphore(any())).willReturn(mono { semaphoreId })
        given(schedulerLockService.releaseJobSemaphore(any(), any())).willReturn(null)

        // Test
        assertDoesNotThrow { paymentWalletScheduledJob.processOnboardedPaymentWallets() }
        // verifications
        verify(onboardedPaymentWalletJob, after(1000).times(2)).id()
        verify(onboardedPaymentWalletJob, after(1000).times(1))
            .process(
                configuration =
                    OnboardedPaymentWalletJobConfiguration(
                        startDate = paymentWalletJobConfiguration.startDate,
                        endDate = paymentWalletJobConfiguration.endDate
                    )
            )
        verify(schedulerLockService, times(1)).acquireJobSemaphore(jobId)
        verify(schedulerLockService, times(1)).releaseJobSemaphore(jobId, semaphoreId)
    }

    @Test
    fun `Should skip batch execution if don't acquire lock`() {
        // pre-requisites
        val jobId = "jobId"
        given(onboardedPaymentWalletJob.id()).willReturn(jobId)
        given(schedulerLockService.acquireJobSemaphore(any()))
            .willReturn(Mono.error(SemNotAcquiredException("jobName")))

        // Test
        paymentWalletScheduledJob.processOnboardedPaymentWallets()

        // verifications
        verify(onboardedPaymentWalletJob, times(1)).id()
        verify(schedulerLockService, times(1)).acquireJobSemaphore(jobId)
        verify(onboardedPaymentWalletJob, times(0)).process(any())
    }
}
