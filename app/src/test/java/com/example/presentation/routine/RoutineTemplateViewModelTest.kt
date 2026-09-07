package com.example.presentation.routine

import com.example.application.usecase.routine.CreateRoutineTemplateUseCase
import com.example.application.usecase.routine.ObserveRoutineTemplatesUseCase
import com.example.domain.model.RoutineTemplate
import com.example.application.usecase.routine.FakeRoutineTemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineTemplateViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeRoutineTemplateRepository
    private lateinit var viewModel: RoutineTemplateViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeRoutineTemplateRepository()
        
        val observeUseCase = ObserveRoutineTemplatesUseCase(repository)
        val createUseCase = CreateRoutineTemplateUseCase(repository)
        
        viewModel = RoutineTemplateViewModel(observeUseCase, createUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createEmptyTemplate should add template to repository`() = runTest {
        viewModel.createEmptyTemplate("Pull Day")
        
        advanceUntilIdle() // Wait for coroutines to complete
        
        val templates = repository.templates
        assertEquals(1, templates.size)
        assertEquals("Pull Day", templates[0].name)
    }
}
