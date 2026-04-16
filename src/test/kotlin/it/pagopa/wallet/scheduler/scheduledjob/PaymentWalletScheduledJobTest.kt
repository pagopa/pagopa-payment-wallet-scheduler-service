package it.pagopa.wallet.scheduler.scheduledjob

import it.pagopa.wallet.scheduler.config.properties.PaymentWalletJobConfiguration
import it.pagopa.wallet.scheduler.exceptions.LockNotAcquiredException
import it.pagopa.wallet.scheduler.jobs.config.OnboardedPaymentWalletJobConfiguration
import it.pagopa.wallet.scheduler.jobs.paymentwallet.OnboardedPaymentWalletJob
import it.pagopa.wallet.scheduler.repositories.redis.ExclusiveLockDocument
import it.pagopa.wallet.scheduler.services.SchedulerLockService
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
        PaymentWalletJobConfiguration(startDate, startDate + Duration.ofMinutes(10), 30)

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
        val lockDocument = ExclusiveLockDocument(jobId, "test")
        given(onboardedPaymentWalletJob.process(configuration = any()))
            .willReturn(mono { Instant.now().toString() })
        given(onboardedPaymentWalletJob.id()).willReturn(jobId)
        given(schedulerLockService.acquireJobLock(any(), any())).willReturn(mono { lockDocument })
        given(schedulerLockService.releaseJobLock(any())).willReturn(mono { true })

        // Test
        paymentWalletScheduledJob.processOnboardedPaymentWallets()
        // verifications
        verify(onboardedPaymentWalletJob, after(1000).times(1)).id()
        verify(onboardedPaymentWalletJob, after(1000).times(1))
            .process(
                configuration =
                    OnboardedPaymentWalletJobConfiguration(
                        startDate = paymentWalletJobConfiguration.startDate,
                        endDate = paymentWalletJobConfiguration.endDate
                    )
            )
        verify(schedulerLockService, times(1)).acquireJobLock(jobId, Duration.ofSeconds(30))
        verify(schedulerLockService, times(1)).releaseJobLock(lockDocument)
    }

    @Test
    fun `Should handle processing exception`() {
        // pre-requisites
        val jobId = "jobId"
        val lockDocument = ExclusiveLockDocument(jobId, "test")
        given(onboardedPaymentWalletJob.process(configuration = any()))
            .willReturn(Mono.error(RuntimeException("Exception during job execution")))
        given(onboardedPaymentWalletJob.id()).willReturn(jobId)
        given(schedulerLockService.acquireJobLock(any(), any())).willReturn(mono { lockDocument })
        given(schedulerLockService.releaseJobLock(any())).willReturn(mono { true })

        // Test
        assertDoesNotThrow { paymentWalletScheduledJob.processOnboardedPaymentWallets() }
        // verifications
        verify(onboardedPaymentWalletJob, after(1000).times(1)).id()
        verify(onboardedPaymentWalletJob, after(1000).times(1))
            .process(
                configuration =
                    OnboardedPaymentWalletJobConfiguration(
                        startDate = paymentWalletJobConfiguration.startDate,
                        endDate = paymentWalletJobConfiguration.endDate
                    )
            )
        verify(schedulerLockService, times(1)).acquireJobLock(jobId, Duration.ofSeconds(30))
        verify(schedulerLockService, times(1)).releaseJobLock(lockDocument)
    }

    @Test
    fun `Should skip batch execution if don't acquire lock`() {
        // pre-requisites
        val jobId = "jobId"
        val lockDocument = ExclusiveLockDocument(jobId, "test")
        given(onboardedPaymentWalletJob.id()).willReturn(jobId)
        given(schedulerLockService.acquireJobLock(any(), any()))
            .willReturn(Mono.error(LockNotAcquiredException(jobId, lockDocument)))
        given(schedulerLockService.releaseJobLock(any())).willReturn(null)

        // Test
        paymentWalletScheduledJob.processOnboardedPaymentWallets()

        // verifications
        verify(onboardedPaymentWalletJob, times(1)).id()
        verify(schedulerLockService, times(1)).acquireJobLock(jobId, Duration.ofSeconds(30))
        verify(schedulerLockService, times(0)).releaseJobLock(lockDocument)
    }
}
