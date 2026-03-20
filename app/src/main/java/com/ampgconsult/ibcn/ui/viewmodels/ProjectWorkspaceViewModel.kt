package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.Project
import com.ampgconsult.ibcn.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectWorkspaceViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    fun loadProject(projectName: String) {
        viewModelScope.launch {
            repository.allProjects.collect { projects ->
                _project.value = projects.find { it.name == projectName }
            }
        }
    }
}
