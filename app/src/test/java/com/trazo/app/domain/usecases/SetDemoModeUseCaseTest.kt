package com.trazo.app.domain.usecases

import com.trazo.app.data.repository.FakeDemoModeRepository
import com.trazo.app.data.repository.FakeRunSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SetDemoModeUseCaseTest {
    private lateinit var fakeDemoModeRepository: FakeDemoModeRepository
    private lateinit var fakeRunSessionRepository: FakeRunSessionRepository
    private lateinit var useCase: SetDemoModeUseCase

    @Before
    fun setUp() {
        fakeDemoModeRepository = FakeDemoModeRepository()
        fakeRunSessionRepository = FakeRunSessionRepository()
        useCase = SetDemoModeUseCase(fakeDemoModeRepository, fakeRunSessionRepository)
    }

    @Test
    fun `with run in progress, does not persist demo mode`() =
        runTest {
            fakeRunSessionRepository.setActive(id = "active-run-1")

            useCase(true)

            assertTrue(fakeDemoModeRepository.setDemoModeCalls.isEmpty())

            useCase(false)

            assertTrue(fakeDemoModeRepository.setDemoModeCalls.isEmpty())
        }

    @Test
    fun `without run in progress, persists true and false`() =
        runTest {
            useCase(true)

            assertEquals(listOf(true), fakeDemoModeRepository.setDemoModeCalls)

            useCase(false)

            assertEquals(listOf(true, false), fakeDemoModeRepository.setDemoModeCalls)
        }
}
