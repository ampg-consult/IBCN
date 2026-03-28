import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import '../models/project_file.dart';
import '../repositories/devlab_repository.dart';
import '../../ai_builder/services/agent_orchestrator.dart';
import '../../ai_builder/ui/ai_builder_studio.dart';

final devLabRepositoryProvider = Provider((ref) => DevLabRepository(FirebaseFirestore.instance));

final projectFilesProvider = StreamProvider.family<List<ProjectFile>, String>((ref, projectId) {
  return ref.watch(devLabRepositoryProvider).watchProjectFiles(projectId);
});

class DevLabState {
  final ProjectFile? selectedFile;
  final bool isGenerating;
  final String? aiSuggestion;

  DevLabState({this.selectedFile, this.isGenerating = false, this.aiSuggestion});

  DevLabState copyWith({ProjectFile? selectedFile, bool? isGenerating, String? aiSuggestion}) {
    return DevLabState(
      selectedFile: selectedFile ?? this.selectedFile,
      isGenerating: isGenerating ?? this.isGenerating,
      aiSuggestion: aiSuggestion ?? this.aiSuggestion,
    );
  }
}

class DevLabNotifier extends StateNotifier<DevLabState> {
  final DevLabRepository _repository;
  final AIAgentOrchestrator _orchestrator;

  DevLabNotifier(this._repository, this._orchestrator) : super(DevLabState());

  void selectFile(ProjectFile file) {
    state = state.copyWith(selectedFile: file);
  }

  Future<void> updateFileContent(String projectId, String content) async {
    if (state.selectedFile == null) return;
    await _repository.updateFileContent(projectId, state.selectedFile!.id, content);
    state = state.copyWith(selectedFile: state.selectedFile!.copyWith(content: content));
  }

  Future<void> requestAIEdit(String projectId, String instruction) async {
    if (state.selectedFile == null) return;
    
    state = state.copyWith(isGenerating: true, aiSuggestion: "");
    
    final prompt = "Edit the following code based on this instruction: $instruction\n\nCode:\n${state.selectedFile!.content}";
    
    String fullSuggestion = "";
    _orchestrator.orchestrate(prompt).listen((response) {
      fullSuggestion = response.content;
      state = state.copyWith(aiSuggestion: fullSuggestion);
    }, onDone: () {
      state = state.copyWith(isGenerating: false);
    });
  }

  void applyAISuggestion(String projectId) {
    if (state.aiSuggestion != null) {
      updateFileContent(projectId, state.aiSuggestion!);
      state = state.copyWith(aiSuggestion: null);
    }
  }
}

final devLabProvider = StateNotifierProvider<DevLabNotifier, DevLabState>((ref) {
  return DevLabNotifier(
    ref.watch(devLabRepositoryProvider),
    ref.watch(orchestratorProvider),
  );
});
