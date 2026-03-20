import psutil
import time
import asyncio
from typing import Dict, Any
from datetime import datetime

class PlatformHealthMonitor:
    def __init__(self):
        self.start_time = time.time()
        self.ai_executions = 0
        self.failed_executions = 0
        self.total_latency = 0.0

    def get_system_metrics(self) -> Dict[str, Any]:
        return {
            "uptime": int(time.time() - self.start_time),
            "cpu_usage": psutil.cpu_percent(),
            "memory_usage": psutil.virtual_memory().percent,
            "active_agents": 5, # Constant for now, can be dynamic
            "health_status": "OPERATIONAL" if psutil.cpu_percent() < 90 else "DEGRADED"
        }

    def log_ai_execution(self, latency: float, success: bool):
        self.ai_executions += 1
        self.total_latency += latency
        if not success:
            self.failed_executions += 1

    def get_ai_metrics(self) -> Dict[str, Any]:
        avg_latency = self.total_latency / self.ai_executions if self.ai_executions > 0 else 0
        success_rate = (1 - (self.failed_executions / self.ai_executions)) * 100 if self.ai_executions > 0 else 100
        return {
            "total_calls": self.ai_executions,
            "avg_latency": round(avg_latency, 3),
            "success_rate": f"{round(success_rate, 2)}%",
            "active_tasks": 0 # Logic to link with orchestrator
        }

monitor = PlatformHealthMonitor()

async def monitoring_loop():
    """Background loop to periodically log system health"""
    while True:
        metrics = monitor.get_system_metrics()
        # In a real system, send this to Prometheus / InfluxDB
        # print(f"Health Check: {metrics}")
        await asyncio.sleep(60)
