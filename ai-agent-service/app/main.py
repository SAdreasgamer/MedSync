"""MedSync AI Agent Service — FastAPI entrypoint."""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import router as chat_router
from app.api.websocket import router as ws_router

app = FastAPI(
    title="MedSync AI Agent",
    description=(
        "Agentic AI service for the Patient Management System. "
        "Provides a natural language interface to manage patients, "
        "billing, and analytics through a LangGraph ReAct agent."
    ),
    version="1.0.0",
)

# CORS — allow frontend to connect
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Tighten this in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount routes
app.include_router(chat_router, prefix="/agent", tags=["Agent"])
app.include_router(ws_router, prefix="/agent", tags=["Agent WebSocket"])


@app.get("/health")
def health():
    """Health check endpoint."""
    return {"status": "healthy", "service": "ai-agent-service"}
