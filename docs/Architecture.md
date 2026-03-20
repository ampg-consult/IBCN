# IBCN - Intelligent Builder Collaboration Network Architecture

## High-Level Architecture
IBCN is designed as a hybrid cloud-local platform to ensure "offline-first" capabilities while leveraging powerful cloud AI for complex tasks.

### 1. Frontend (Cross-platform)
- **Mobile/Desktop**: Flutter / Jetpack Compose (Android)
- **Web**: React / Flutter Web
- **State Management**: Redux / Bloc / Compose State

### 2. Backend (Microservices)
- **API Gateway**: Node.js / FastAPI
- **Auth Service**: Firebase Auth / Custom JWT
- **Project Service**: FastAPI (Manages project lifecycle)
- **Collaboration Service**: Socket.io (Real-time sync)
- **Marketplace Service**: Node.js (Transactions & Assets)

### 3. AI Layer (Hybrid)
- **Local AI**: ONNX Runtime / TensorFlow Lite (Small LLMs, Basic Code Generation)
- **Cloud AI**: Google Gemini / OpenAI GPT-4 (Advanced Architecture, Security Audits)
- **Agents**:
    - Architect: Structural design & Tech stack
    - Developer: Code generation & Refactoring
    - Designer: UI/UX Wireframes & Assets
    - Tester: Automated unit/integration tests
    - Security: Vulnerability scanning

### 4. Data Layer
- **NoSQL**: MongoDB (Project metadata, AI logs, User Activity)
- **Real-time**: Firebase Firestore (Notifications, Messaging, Real-time Collab)
- **Object Storage**: AWS S3 / Google Cloud Storage (Project assets, templates)
- **Local Cache**: Room (Android) / SQLite (Flutter)

---

## Database Schema (Simplified)

### User Module
- `uid`: String (Primary Key)
- `name`: String
- `reputation`: Float
- `skills`: List<String>
- `portfolio_ids`: List<String>

### Project Module
- `project_id`: String (PK)
- `owner_id`: String (FK)
- `collaborators`: List<CollaboratorRole>
- `current_version`: String (FK)
- `ai_config`: AIConfig
- `status`: Enum (Draft, Active, Launched)

### Versioning Module
- `version_id`: String (PK)
- `project_id`: String (FK)
- `parent_version_id`: String (Optional)
- `changeset`: JSON (Diff of code/assets)
- `created_by`: String (User or AI Agent)
- `timestamp`: DateTime

### Analytics Module
- `event_id`: String (PK)
- `user_id`: String
- `event_type`: Enum (ProjectCreate, AIInteraction, ToolUsage)
- `metadata`: JSON
- `timestamp`: DateTime
