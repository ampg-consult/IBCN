import 'package:openai_dart/openai_dart.dart';
import '../models/agreement.dart';

class LegalService {
  final OpenAIClient _client;

  LegalService(String apiKey) : _client = OpenAIClient(apiKey: apiKey);

  Future<String> generateAgreementContent({
    required AgreementType type,
    required Map<String, dynamic> context,
  }) async {
    final typeStr = type.name.toUpperCase();
    final prompt = """
    Act as an AI Legal Consultant. Draft a professional $typeStr agreement based on the following context:
    $context
    
    Ensure the agreement includes standard clauses, placeholders for specific details if not provided, and clear terms.
    The response should be in a structured text format suitable for a contract.
    """;

    final request = ChatCompletionCreateRequest(
      model: const ChatCompletionModel.model(ChatCompletionModels.gpt4o),
      messages: [
        ChatCompletionMessage.system(content: "You are an expert AI Legal Drafter for IBCN. Create legally sound, professional agreements."),
        ChatCompletionMessage.user(content: prompt),
      ],
    );

    try {
      final response = await _client.createChatCompletion(request: request);
      return response.choices.first.message.content ?? "Failed to generate content.";
    } catch (e) {
      return "Error generating agreement: $e";
    }
  }
}
