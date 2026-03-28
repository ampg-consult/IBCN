enum AgentType {
  productManager,
  architect,
  developer,
  devOps,
  security,
  usernameAssistant
}

class AgentResponse {
  final AgentType agentType;
  final String content;
  final bool isComplete;
  final bool isError;

  AgentResponse({
    required this.agentType,
    required this.content,
    this.isComplete = false,
    this.isError = false,
  });
}

extension AgentTypeExtension on AgentType {
  String get displayName {
    switch (this) {
      case AgentType.productManager: return 'AI Product Manager';
      case AgentType.architect: return 'AI Architect';
      case AgentType.developer: return 'AI Developer';
      case AgentType.devOps: return 'AI DevOps Engineer';
      case AgentType.security: return 'AI Security Engineer';
      case AgentType.usernameAssistant: return 'Identity Assistant';
    }
  }

  String get systemPrompt {
    switch (this) {
      case AgentType.productManager:
        return "Act as an AI Product Manager. Convert ideas into professional product specs, features, and user stories.";
      case AgentType.architect:
        return "Act as an AI Architect. Design scalable architecture, service diagrams, and database structures.";
      case AgentType.developer:
        return "Act as a Senior Developer. Generate production-ready Flutter and Backend code.";
      case AgentType.devOps:
        return "Act as a DevOps Engineer. Create deployment plans, Docker configs, and scaling strategies.";
      case AgentType.security:
        return "Act as a Security Engineer. Audit architecture for risks and recommend protection.";
      case AgentType.usernameAssistant:
        return "Creative naming assistant. Generate unique, professional usernames.";
    }
  }
}
