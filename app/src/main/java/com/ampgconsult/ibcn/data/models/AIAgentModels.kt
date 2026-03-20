package com.ampgconsult.ibcn.data.models

enum class AgentType(val displayName: String, val preferredModel: String) {
    PRODUCT_MANAGER("AI Product Manager", "gpt-4o-mini"),
    ARCHITECT("AI Architect", "gpt-4o-mini"),
    DEVELOPER("AI Developer", "gpt-4o-mini"),
    DEVOPS("AI DevOps Engineer", "gpt-4o-mini"),
    SECURITY("AI Security Engineer", "gpt-4o-mini"),
    USERNAME_ASSISTANT("AI Username Assistant", "gpt-4o-mini"),
    LAUNCHPAD_PLANNER("Launchpad Planner", "gpt-4o-mini"),
    HABIT_GENERATOR("Habit Generator", "gpt-4o-mini"),
    MARKETPLACE_REVIEWER("Marketplace Reviewer", "gpt-4o-mini"),
    DOC_GENERATOR("Documentation Generator", "gpt-4o-mini"),
    ANALYTICS_LAB("Analytics Lab AI", "gpt-4o-mini"),
    MEDIA_STRATEGIST("AI Viral Media Strategist", "gpt-4o-mini")
}

data class AgentResponse(
    val agentType: AgentType,
    val content: String,
    val isComplete: Boolean = false,
    val isError: Boolean = false
)

data class AIBuilderReport(
    val productPlan: String = "",
    val architecture: String = "",
    val generatedCode: String = "",
    val devOpsStrategy: String = "",
    val securityAudit: String = ""
)

data class LaunchpadPlan(
    val roadmap: String = "",
    val features: String = "",
    val phases: String = "",
    val monetization: String = ""
)
