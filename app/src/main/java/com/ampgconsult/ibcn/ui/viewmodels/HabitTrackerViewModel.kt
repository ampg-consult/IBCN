package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.ai.AIBuilderOrchestrator
import com.ampgconsult.ibcn.data.models.DailyHabitRecord
import com.ampgconsult.ibcn.data.models.HabitTask
import com.ampgconsult.ibcn.data.models.HabitTaskStatus
import com.ampgconsult.ibcn.data.repository.HabitTrackerService
import com.ampgconsult.ibcn.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HabitTrackerViewModel @Inject constructor(
    private val habitTrackerService: HabitTrackerService,
    private val projectRepository: ProjectRepository,
    private val aiOrchestrator: AIBuilderOrchestrator
) : ViewModel() {

    private val _dailyRecord = MutableStateFlow<DailyHabitRecord?>(null)
    val dailyRecord: StateFlow<DailyHabitRecord?> = _dailyRecord

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadDailyRecord(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val record = habitTrackerService.getDailyHabitRecord(projectId)
                _dailyRecord.value = record
                
                if (record == null || record.tasks.isEmpty()) {
                    generateDailyTasks(projectId)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load habit record"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun generateDailyTasks(projectId: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val projectResult = projectRepository.getProjectById(projectId)
                val project = projectResult.getOrNull() ?: throw Exception("Project not found")
                
                val tasksResult = aiOrchestrator.generateHabits(
                    projectDescription = project.description,
                    stage = project.status
                )
                
                val tasks = tasksResult.getOrThrow().map { content ->
                    HabitTask(
                        taskId = java.util.UUID.randomUUID().toString(),
                        description = content,
                        status = HabitTaskStatus.PENDING
                    )
                }
                
                if (tasks.isNotEmpty()) {
                    habitTrackerService.saveDailyTasks(projectId, tasks)
                    loadDailyRecord(projectId)
                }
            } catch (e: Exception) {
                _error.value = "AI Task Generation Failed: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun updateTaskStatus(projectId: String, taskId: String, newStatus: HabitTaskStatus) {
        viewModelScope.launch {
            val result = habitTrackerService.updateTaskStatus(projectId, taskId, newStatus)
            if (result.isSuccess) {
                loadDailyRecord(projectId)
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to update task"
            }
        }
    }
}
