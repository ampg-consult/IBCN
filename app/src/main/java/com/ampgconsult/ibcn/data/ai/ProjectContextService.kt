package com.ampgconsult.ibcn.data.ai

import com.ampgconsult.ibcn.data.models.ProjectFile
import com.ampgconsult.ibcn.data.repository.ProjectFileService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectContextService @Inject constructor(
    private val fileService: ProjectFileService
) {
    suspend fun buildProjectContext(projectId: String): String {
        val files = fileService.getProjectFiles(projectId)
        if (files.isEmpty()) return "No project files found."

        val sb = StringBuilder()
        sb.append("FLUTTER PROJECT CONTEXT:\n")
        
        // Prioritize key files
        val criticalFiles = listOf("pubspec.yaml", "main.dart")
        val prioritized = files.filter { file -> criticalFiles.any { it == file.fileName } }
        val others = files.filter { file -> criticalFiles.none { it == file.fileName } }

        fun appendFile(file: ProjectFile) {
            sb.append("\n--- FILE: ${file.path}${file.fileName} ---\n")
            // Basic summarization if file is huge (rudimentary)
            val content = if (file.content.length > 3000) {
                file.content.take(1500) + "\n... [CONTENT TRUNCATED] ...\n" + file.content.takeLast(1000)
            } else {
                file.content
            }
            sb.append(content)
            sb.append("\n---------------------------\n")
        }

        prioritized.forEach { appendFile(it) }
        
        // Limit total context size to ~10k chars for Ollama efficiency
        var totalLength = sb.length
        for (file in others) {
            if (totalLength > 10000) break
            appendFile(file)
            totalLength += file.content.length
        }

        return sb.toString()
    }
}
