import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/agent_models.dart';
import '../services/agent_orchestrator.dart';

// Provider for the orchestrator (API key should be injected properly in production)
final orchestratorProvider = Provider((ref) => AIAgentOrchestrator("YOUR_OPENAI_API_KEY"));

class AIBuilderStudio extends ConsumerStatefulWidget {
  const AIBuilderStudio({super.key});

  @override
  ConsumerState<AIBuilderStudio> createState() => _AIBuilderStudioState();
}

class _AIBuilderStudioState extends ConsumerState<AIBuilderStudio> {
  final TextEditingController _promptController = TextEditingController();
  final List<AgentResponse> _responses = [];
  bool _isBuilding = false;
  String _currentStatus = "Ready to build";

  void _startBuilding() {
    if (_promptController.text.isEmpty) return;

    setState(() {
      _isBuilding = true;
      _responses.clear();
    });

    final orchestrator = ref.read(orchestratorProvider);
    
    orchestrator.orchestrate(_promptController.text).listen(
      (response) {
        setState(() {
          _currentStatus = "${response.agentType.displayName} working...";
          
          final index = _responses.indexWhere((r) => r.agentType == response.agentType);
          if (index != -1) {
            _responses[index] = response;
          } else {
            _responses.add(response);
          }
        });
      },
      onDone: () {
        setState(() {
          _isBuilding = false;
          _currentStatus = "AI Build Complete";
        });
      },
      onError: (error) {
        setState(() {
          _isBuilding = false;
          _currentStatus = "Error during build";
        });
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('AI Builder Studio'),
      ),
      body: Column(
        children: [
          _buildInputSection(),
          const Divider(),
          Expanded(
            child: _responses.isEmpty 
              ? _buildEmptyState()
              : _buildAgentActivityList(),
          ),
        ],
      ),
    );
  }

  Widget _buildInputSection() {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextField(
            controller: _promptController,
            decoration: const InputDecoration(
              hintText: "Describe your app idea (e.g., A decentralized marketplace for AI assets)",
              border: OutlineInputBorder(),
            ),
            maxLines: 3,
          ),
          const SizedBox(height: 12),
          ElevatedButton.icon(
            onPressed: _isBuilding ? null : _startBuilding,
            icon: _isBuilding 
              ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
              : const Icon(Icons.auto_awesome),
            label: Text(_isBuilding ? _currentStatus : "Launch Multi-Agent Build"),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.hub_outlined, size: 64, color: Colors.grey[700]),
          const SizedBox(height: 16),
          const Text("Enter a prompt to activate the AI Agent Swarm"),
        ],
      ),
    );
  }

  Widget _buildAgentActivityList() {
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: _responses.length,
      itemBuilder: (context, index) {
        final response = _responses[index];
        return _AgentResponseCard(response: response);
      },
    );
  }
}

class _AgentResponseCard extends StatelessWidget {
  final AgentResponse response;
  const _AgentResponseCard({required this.response});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: ExpansionTile(
        initiallyExpanded: !response.isComplete,
        leading: _buildIcon(response),
        title: Text(response.agentType.displayName, style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Text(response.isComplete ? "Completed" : "Streaming..."),
        trailing: response.isComplete 
          ? const Icon(Icons.check_circle, color: Colors.green)
          : const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)),
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: SelectableText(
              response.content,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildIcon(AgentResponse response) {
    switch (response.agentType) {
      case AgentType.productManager: return const Icon(Icons.assignment);
      case AgentType.architect: return const Icon(Icons.account_tree);
      case AgentType.developer: return const Icon(Icons.code);
      case AgentType.devOps: return const Icon(Icons.cloud_upload);
      case AgentType.security: return const Icon(Icons.security);
      case AgentType.usernameAssistant: return const Icon(Icons.person_search);
    }
  }
}
