# IBCN - Functional Specifications

## 1. Authentication & Profile System
- **Sign-up/Login**: Support for Email/Password and Social Auth (Firebase).
- **Profile Management**: Users can set roles (Architect, Developer, Designer, Investor), list skills, and view their reputation score.
- **Reputation System**: Points earned through project completion, successful AI collaboration, and community ratings.

## 2. AI-Powered Project Engine
- **Project Initiation**: Users provide a natural language prompt.
- **Agent Orchestration**:
    - **Architect Agent**: Proposes tech stack and DB schema.
    - **Developer Agent**: Scaffolds code based on architecture.
    - **Designer Agent**: Generates UI component suggestions and themes.
- **Hybrid Execution**: Simple tasks run on-device (local LLM), complex reasoning is offloaded to Cloud AI (Gemini/GPT-4).

## 3. Real-Time Collaboration
- **Shared Workspace**: Multi-user editing of project files and AI prompts.
- **Live Notifications**: Alerts for agent actions, collaborator comments, and system updates.

## 4. Marketplace
- **Asset Listings**: Templates, AI models, and custom workflows.
- **Transaction Flow**: Support for free and paid assets with creator royalties.

## 5. Versioning & Rollback
- **Automated Commits**: Every AI-generated change creates a version entry.
- **Point-in-time Rollback**: Restore any previous project state from the history tab.
- **Audit Log**: Trace which agent or user made specific changes.

## 6. Analytics & Dashboard
- **Creator Dashboard**: Track project health, contribution stats, and AI efficiency.
- **Global Mapping**: Visual map of active projects and collaboration nodes across the network.
