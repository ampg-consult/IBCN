from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect
from pydantic import BaseModel
from typing import List, Optional, Dict
import datetime
import uuid
import asyncio
from agents import agents, ArchitectAgent, DeveloperAgent

app = FastAPI(title="IBCN Backend API", version="1.1.0")

# --- Models ---
class Project(BaseModel):
    id: str = str(uuid.uuid4())
    name: str
    description: str
    owner_id: str
    status: str = "DRAFT"
    progress: float = 0.0
    created_at: datetime.datetime = datetime.datetime.now()

class ChatMessage(BaseModel):
    user_id: str
    text: str
    is_user: bool
    timestamp: datetime.datetime = datetime.datetime.now()

# --- In-Memory DB (Replace with MongoDB/Firebase in production) ---
projects_db: List[Project] = [
    Project(id="1", name="Neural Nexus", description="AI-driven knowledge graph", owner_id="user_1", status="ACTIVE", progress=0.75),
    Project(id="2", name="EcoSphere", description="Decentralized carbon tracking", owner_id="user_1", status="ACTIVE", progress=0.40),
]

# --- WebSocket Manager for Real-time AI Collab ---
class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)

    async def broadcast(self, message: dict):
        for connection in self.active_connections:
            await connection.send_json(message)

manager = ConnectionManager()

# --- Endpoints ---
@app.get("/")
async def root():
    return {"message": "IBCN API v1.1.0 Online"}

@app.get("/health")
async def health():
    return {
        "status": "healthy",
        "timestamp": datetime.datetime.now().isoformat(),
        "version": "1.1.0",
        "services": {
            "openai": "online",
            "database": "online",
            "websocket_manager": "active"
        }
    }

@app.get("/projects", response_model=List[Project])
async def get_projects():
    return projects_db

@app.post("/projects", response_model=Project)
async def create_project(project: Project):
    projects_db.append(project)
    return project

@app.get("/projects/{project_id}", response_model=Project)
async def get_project(project_id: str):
    for p in projects_db:
        if p.id == project_id:
            return p
    raise HTTPException(status_code=404, detail="Project not found")

# --- AI Collaborative WebSocket ---
@app.websocket("/ws/builder/{project_id}")
async def websocket_endpoint(websocket: WebSocket, project_id: str):
    await manager.connect(websocket)
    try:
        while True:
            data = await websocket.receive_text()
            # User sent a prompt
            await manager.broadcast({"sender": "user", "text": data})
            
            # Trigger AI Agents based on prompt analysis
            await asyncio.sleep(1) # Simulate thinking
            await manager.broadcast({"sender": "system", "text": "Architect Agent is analyzing requirements..."})
            
            # Simulate Agent Output
            await asyncio.sleep(2)
            architect_response = agents["architect"].generate_schema(data)
            await manager.broadcast({
                "sender": "architect", 
                "text": f"Proposed Schema: {architect_response['schema']}",
                "action": "SCHEMA_GEN"
            })
            
    except WebSocketDisconnect:
        manager.disconnect(websocket)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
