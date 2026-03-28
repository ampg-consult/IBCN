import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:responsive_framework/responsive_framework.dart';
import '../viewmodels/devlab_viewmodel.dart';
import '../models/project_file.dart';
import 'package:code_text_field/code_text_field.dart';
import 'package:highlight/languages/dart.dart';
import 'package:flutter_highlight/themes/monokai-sublime.dart';

class DevLabScreen extends ConsumerStatefulWidget {
  final String projectId;
  const DevLabScreen({super.key, required this.projectId});

  @override
  ConsumerState<DevLabScreen> createState() => _DevLabScreenState();
}

class _DevLabScreenState extends ConsumerState<DevLabScreen> {
  CodeController? _codeController;

  @override
  void dispose() {
    _codeController?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(devLabProvider);
    final filesAsync = ref.watch(projectFilesProvider(widget.projectId));
    final bool isMobile = ResponsiveBreakpoints.of(context).isMobile;

    // Initialize or update controller when file changes
    if (state.selectedFile != null && (_codeController == null || _codeController!.text != state.selectedFile!.content)) {
      _codeController = CodeController(
        text: state.selectedFile!.content,
        language: dart,
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(state.selectedFile?.fileName ?? 'Dev Lab IDE'),
        actions: [
          if (state.selectedFile != null)
            IconButton(
              icon: const Icon(Icons.save),
              onPressed: () => ref.read(devLabProvider.notifier).updateFileContent(
                widget.projectId, 
                _codeController?.text ?? ""
              ),
            ),
          IconButton(
            icon: const Icon(Icons.play_arrow, color: Colors.green),
            onPressed: () {},
          ),
        ],
      ),
      body: filesAsync.when(
        data: (files) {
          if (isMobile) {
            return _MobileLayout(
              projectId: widget.projectId,
              files: files,
              codeController: _codeController,
            );
          } else {
            return _DesktopLayout(
              projectId: widget.projectId,
              files: files,
              codeController: _codeController,
            );
          }
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(child: Text('Error: $err')),
      ),
    );
  }
}

class _DesktopLayout extends StatelessWidget {
  final String projectId;
  final List<ProjectFile> files;
  final CodeController? codeController;

  const _DesktopLayout({
    required this.projectId,
    required this.files,
    this.codeController,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        // 1. Files Panel
        SizedBox(
          width: 250,
          child: _FileExplorer(files: files),
        ),
        const VerticalDivider(width: 1),
        
        // 2. Editor Panel
        Expanded(
          flex: 3,
          child: _EditorView(codeController: codeController),
        ),
        const VerticalDivider(width: 1),
        
        // 3. AI Assistant Panel
        SizedBox(
          width: 350,
          child: _AIAssistantSidePanel(projectId: projectId),
        ),
      ],
    );
  }
}

class _MobileLayout extends StatefulWidget {
  final String projectId;
  final List<ProjectFile> files;
  final CodeController? codeController;

  const _MobileLayout({
    required this.projectId,
    required this.files,
    this.codeController,
  });

  @override
  State<_MobileLayout> createState() => _MobileLayoutState();
}

class _MobileLayoutState extends State<_MobileLayout> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: "Files"),
            Tab(text: "Editor"),
            Tab(text: "AI Chat"),
          ],
        ),
        Expanded(
          child: TabBarView(
            controller: _tabController,
            children: [
              _FileExplorer(files: widget.files),
              _EditorView(codeController: widget.codeController),
              _AIAssistantSidePanel(projectId: widget.projectId),
            ],
          ),
        ),
      ],
    );
  }
}

class _FileExplorer extends ConsumerWidget {
  final List<ProjectFile> files;
  const _FileExplorer({required this.files});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return ListView.builder(
      itemCount: files.length,
      itemBuilder: (context, index) {
        final file = files[index];
        return ListTile(
          leading: const Icon(Icons.insert_drive_file_outlined, size: 18),
          title: Text(file.fileName, style: const TextStyle(fontSize: 13)),
          onTap: () => ref.read(devLabProvider.notifier).selectFile(file),
          selected: ref.watch(devLabProvider).selectedFile?.id == file.id,
        );
      },
    );
  }
}

class _EditorView extends StatelessWidget {
  final CodeController? codeController;
  const _EditorView({this.codeController});

  @override
  Widget build(BuildContext context) {
    if (codeController == null) {
      return const Center(child: Text("Select a file to start coding"));
    }

    return CodeTheme(
      data: CodeThemeData(styles: monokaiSublimeTheme),
      child: CodeField(
        controller: codeController!,
        textStyle: const TextStyle(fontFamily: 'SourceCodePro', fontSize: 14),
        expands: true,
      ),
    );
  }
}

class _AIAssistantSidePanel extends ConsumerStatefulWidget {
  final String projectId;
  const _AIAssistantSidePanel({required this.projectId});

  @override
  ConsumerState<_AIAssistantSidePanel> createState() => _AIAssistantSidePanelState();
}

class _AIAssistantSidePanelState extends ConsumerState<_AIAssistantSidePanel> {
  final TextEditingController _aiInput = TextEditingController();

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(devLabProvider);

    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.auto_awesome, color: Colors.purpleAccent),
              const SizedBox(width: 8),
              Text("AI Coding Assistant", style: Theme.of(context).textTheme.titleMedium),
            ],
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _aiInput,
            decoration: const InputDecoration(
              hintText: "e.g. Refactor this function to be more efficient",
              border: OutlineInputBorder(),
            ),
            maxLines: 3,
          ),
          const SizedBox(height: 12),
          ElevatedButton(
            onPressed: state.isGenerating || state.selectedFile == null
                ? null 
                : () => ref.read(devLabProvider.notifier).requestAIEdit(widget.projectId, _aiInput.text),
            child: const Text("Apply AI Instruction"),
          ),
          const Divider(height: 32),
          if (state.aiSuggestion != null) ...[
            Text("Diff Preview", style: Theme.of(context).textTheme.labelLarge),
            const SizedBox(height: 8),
            Expanded(
              child: Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.black26,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: SingleChildScrollView(
                  child: Text(
                    state.aiSuggestion!,
                    style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                TextButton(
                  onPressed: () => ref.read(devLabProvider.notifier).applyAISuggestion(widget.projectId),
                  child: const Text("Accept Changes"),
                ),
                TextButton(
                  onPressed: () {}, // Reject logic
                  child: const Text("Reject", style: TextStyle(color: Colors.red)),
                ),
              ],
            )
          ] else if (state.isGenerating)
            const Center(child: CircularProgressIndicator())
          else
            const Expanded(child: Center(child: Text("AI results will appear here", style: TextStyle(color: Colors.grey)))),
        ],
      ),
    );
  }
}
