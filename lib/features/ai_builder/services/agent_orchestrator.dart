import 'dart:async';
import 'package:openai_dart/openai_dart.dart';
import '../models/agent_models.dart';

class AIAgentOrchestrator {
  final OpenAIClient _client;

  AIAgentOrchestrator(String apiKey) 
      : _client = OpenAIClient(apiKey: apiKey);

  Stream<AgentResponse> orchestrate(String userPrompt) async* {
    final agents = [
      AgentType.productManager,
      AgentType.architect,
      AgentType.developer,
      AgentType.devOps,
      AgentType.security,
    ];

    String cumulativeContext = "User Request: $userPrompt\n\n";

    for (final agent in agents) {
      yield* _runAgentStream(agent, userPrompt, cumulativeContext).map((response) {
        if (response.isComplete) {
          cumulativeContext += "\n--- ${agent.displayName} Output ---\n${response.content}\n";
        }
        return response;
      });
    }
  }

  Stream<AgentResponse> _runAgentStream(
    AgentType type, 
    String userPrompt, 
    String context
  ) async* {
    final request = ChatCompletionCreateRequest(
      model: const ChatCompletionModel.model(ChatCompletionModels.gpt4oMini),
      messages: [
        ChatCompletionMessage.system(content: type.systemPrompt),
        ChatCompletionMessage.user(content: "Context:\n$context\n\nTask: Process this request: $userPrompt"),
      ],
      stream: true,
    );

    String fullContent = "";
    
    try {
      final stream = _client.createChatCompletionStream(request: request);
      
      await for (final chunk in stream) {
        final delta = chunk.choices.first.delta.content;
        if (delta != null) {
          fullContent += delta;
          yield AgentResponse(agentType: type, content: fullContent);
        }
      }
      
      yield AgentResponse(agentType: type, content: fullContent, isComplete: true);
    } catch (e) {
      yield AgentResponse(agentType: type, content: "Error: $e", isError: true);
    }
  }
}
