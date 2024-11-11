package it.pagopa.wallet.scheduler.service

import it.pagopa.wallet.scheduler.config.properties.RedisJobLockPolicyConfig
import it.pagopa.wallet.scheduler.exceptions.LockNotAcquiredException
import it.pagopa.wallet.scheduler.exceptions.LockNotReleasedException
import it.pagopa.wallet.scheduler.exceptions.SemNotAcquiredException
import it.pagopa.wallet.scheduler.exceptions.SemNotReleasedException
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlinx.coroutines.reactor.mono
import org.mockito.kotlin.*
import org.redisson.api.RLockReactive
import org.redisson.api.RPermitExpirableSemaphoreReactive
import org.redisson.api.RedissonReactiveClient
import reactor.core.publisher.Mono
import reactor.kotlin.test.test

class SchedulerLockServiceTest {
    private val rLockReactive: RLockReactive = mock()
    private val rPermitExpirableSemaphoreReactive: RPermitExpirableSemaphoreReactive = mock()
    private val redissonClient: RedissonReactiveClient = mock()
    private val redisJobLockPolicyConfig: RedisJobLockPolicyConfig =
        RedisJobLockPolicyConfig("keyspace", 20, 2)
    private val schedulerLockService: SchedulerLockService =
        SchedulerLockService(redissonClient, redisJobLockPolicyConfig)

    /*+ Lock tests **/

    @Test
    fun `Should acquire lock`() {
        // pre-requisites
        val jobName = "job-name-lock"
        given(redissonClient.getLock(any<String>())).willReturn(rLockReactive)
        given(rLockReactive.tryLock(any(), any(), any())).willReturn(mono { true })

        // Test
        schedulerLockService.acquireJobLock(jobName).test().expectNext(Unit).verifyComplete()

        // verifications
        verify(redissonClient, times(1)).getLock("keyspace:lock:$jobName")
        verify(rLockReactive, times(1)).tryLock(2, 20, TimeUnit.SECONDS)
    }

    @Test
    fun `Should release lock`() {
        // pre-requisites
        val jobName = "job-name-lock"
        given(redissonClient.getLock(any<String>())).willReturn(rLockReactive)
        given(rLockReactive.unlock()).willReturn(Mono.empty())

        // Test
        schedulerLockService.releaseJobLock(jobName).test().expectNext(Unit).verifyComplete()

        // verifications
        verify(redissonClient, times(1)).getLock("keyspace:lock:$jobName")
        verify(rLockReactive, times(1)).unlock()
    }

    @Test
    fun `Should throw LockNotAcquiredException when lock is already acquired`() {
        // pre-requisites
        val jobName = "job-name-lock"
        given(redissonClient.getLock(any<String>())).willReturn(rLockReactive)
        given(rLockReactive.tryLock(any(), any(), any())).willReturn(Mono.just(false))

        // Test
        schedulerLockService
            .acquireJobLock("job-name-lock")
            .test()
            .expectError(LockNotAcquiredException::class.java)
            .verify()

        // verifications
        verify(redissonClient, times(1)).getLock("keyspace:lock:$jobName")
        verify(rLockReactive, times(1)).tryLock(2, 20, TimeUnit.SECONDS)
    }

    @Test
    fun `Should throw LockNotReleasedException when fail lock release`() {
        // pre-requisites
        val jobName = "job-name-lock"
        given(redissonClient.getLock(any<String>())).willReturn(rLockReactive)
        given(rLockReactive.unlock()).willReturn(Mono.error(RuntimeException("Release lock error")))

        // Test
        schedulerLockService
            .releaseJobLock("job-name-lock")
            .test()
            .expectError(LockNotReleasedException::class.java)
            .verify()

        // verifications
        verify(redissonClient, times(1)).getLock("keyspace:lock:$jobName")
        verify(rLockReactive, times(1)).unlock()
    }

    /*+ Semaphore tests **/

    @Test
    fun `Should acquire semaphore`() {
        // pre-requisites
        val jobName = "job-name-semaphore"
        val semaphoreId = "semaphore-id"
        given(redissonClient.getPermitExpirableSemaphore(any<String>()))
            .willReturn(rPermitExpirableSemaphoreReactive)
        given(rPermitExpirableSemaphoreReactive.trySetPermits(any())).willReturn(mono { true })
        given(rPermitExpirableSemaphoreReactive.tryAcquire(any(), any(), any()))
            .willReturn(mono { semaphoreId })

        // Test
        schedulerLockService
            .acquireJobSemaphore(jobName)
            .test()
            .expectNext(semaphoreId)
            .verifyComplete()

        // verifications
        verify(redissonClient, times(1)).getPermitExpirableSemaphore("keyspace:sem:$jobName")
        verify(rPermitExpirableSemaphoreReactive, times(1)).trySetPermits(1)
        verify(rPermitExpirableSemaphoreReactive, times(1)).tryAcquire(2, 20, TimeUnit.SECONDS)
    }

    @Test
    fun `Should release semaphore`() {
        // pre-requisites
        val jobName = "job-name-semaphore"
        val semaphoreId = "semaphore-id"
        given(redissonClient.getPermitExpirableSemaphore(any<String>()))
            .willReturn(rPermitExpirableSemaphoreReactive)
        given(rPermitExpirableSemaphoreReactive.release(any<String>())).willReturn(Mono.empty())

        // Test
        schedulerLockService
            .releaseJobSemaphore(jobName, semaphoreId)
            .test()
            .expectNext(Unit)
            .verifyComplete()

        // verifications
        verify(redissonClient, times(1)).getPermitExpirableSemaphore("keyspace:sem:$jobName")
        verify(rPermitExpirableSemaphoreReactive, times(1)).release(semaphoreId)
    }

    @Test
    fun `Should throw SemNotAcquiredException when semaphore is already acquired`() {
        // pre-requisites
        val jobName = "job-name-semaphore"
        given(redissonClient.getPermitExpirableSemaphore(any<String>()))
            .willReturn(rPermitExpirableSemaphoreReactive)
        given(rPermitExpirableSemaphoreReactive.trySetPermits(1)).willReturn(Mono.just(true))
        given(rPermitExpirableSemaphoreReactive.tryAcquire(any(), any(), any()))
            .willReturn(Mono.error(RuntimeException("Acquire semaphore error")))

        // Test
        schedulerLockService
            .acquireJobSemaphore(jobName)
            .test()
            .expectError(SemNotAcquiredException::class.java)
            .verify()

        // verifications
        verify(redissonClient, times(1)).getPermitExpirableSemaphore("keyspace:sem:$jobName")
        verify(rPermitExpirableSemaphoreReactive, times(1)).trySetPermits(1)
        verify(rPermitExpirableSemaphoreReactive, times(1)).tryAcquire(2, 20, TimeUnit.SECONDS)
    }

    @Test
    fun `Should throw SemNotReleasedException when fail semaphore release`() {
        // pre-requisites
        val jobName = "job-name-semaphore"
        val semaphoreId = "semaphore-id"
        given(redissonClient.getPermitExpirableSemaphore(any<String>()))
            .willReturn(rPermitExpirableSemaphoreReactive)
        given(rPermitExpirableSemaphoreReactive.release(any<String>()))
            .willReturn(Mono.error(RuntimeException("Release semaphore error")))

        // Test
        schedulerLockService
            .releaseJobSemaphore(jobName, semaphoreId)
            .test()
            .expectError(SemNotReleasedException::class.java)
            .verify()

        // verifications
        verify(redissonClient, times(1)).getPermitExpirableSemaphore("keyspace:sem:$jobName")
        verify(rPermitExpirableSemaphoreReactive, times(1)).release(semaphoreId)
    }
}
