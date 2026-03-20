import asyncio
import uuid
from typing import List, Dict, Any, Optional
from datetime import datetime
from pydantic import BaseModel
from agents import agents

class AgentTask(BaseModel):
    task_id: str = str(uuid.uuid4())
    project_id: str
    agent_type: str
    input_data: Dict[str, Any]
    status: str = "PENDING" # PENDING, RUNNING, COMPLETED, FAILED
    result: Optional[Any] = None
    created_at: datetime = datetime.now()
    updated_at: datetime = datetime.now()

class AIOrchestrator:
    def __init__(self):
        self.task_history: Dict[str, AgentTask] = {}
        self.active_workflows: Dict[str, List[str]] = {} # project_id -> list of task_ids

    async def dispatch_task(self, project_id: str, agent_type: str, input_data: Dict[str, Any]) -> str:
        if agent_type not in agents:
            raise ValueError(f"Agent {agent_type} not found in registry.")
        
        task = AgentTask(project_id=project_id, agent_type=agent_type, input_data=input_data)
        self.task_history[task.task_id] = task
        
        if project_id not in self.active_workflows:
            self.active_workflows[project_id] = []
        self.active_workflows[project_id].append(task.task_id)

        # Asynchronous execution trigger
        asyncio.create_task(self._run_task(task.task_id))
        return task.task_id

    async def _run_task(self, task_id: str):
        task = self.task_history[task_id]
        task.status = "RUNNING"
        task.updated_at = datetime.now()
        
        try:
            agent = agents[task.agent_type]
            # Depending on agent type, call specific generation methods or generic process_task
            if task.agent_type == "architect":
                task.result = agent.generate_schema(task.input_data.get("prompt", ""))
            elif task.agent_type == "developer":
                task.result = agent.generate_code(task.input_data.get("context", {}))
            else:
                task.result = await agent.process_task(str(task.input_data))
            
            task.status = "COMPLETED"
        except Exception as e:
            task.status = "FAILED"
            task.result = str(e)
        
        task.updated_at = datetime.now()

    def get_task_status(self, task_id: str) -> Optional[AgentTask]:
        return self.task_history.get(task_id)

    def get_project_workflow(self, project_id: str) -> List[AgentTask]:
        task_ids = self.active_workflows.get(project_id, [])
        return [self.task_history[tid] for tid in task_ids]

orchestrator = AIOrchestrator()
