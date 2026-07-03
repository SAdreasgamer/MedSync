"""REST API routes for the AI agent."""

from typing import Any

from fastapi import APIRouter
from pydantic import BaseModel

from app.agent.graph import build_agent

router = APIRouter()

# Build the agent once at module level
agent = None


def get_agent():
    """Lazy-init the agent (built on first request)."""
    global agent
    if agent is None:
        agent = build_agent()
    return agent


class ChatRequest(BaseModel):
    """Request body for the chat endpoint."""

    message: str


class ToolCallInfo(BaseModel):
    """Info about a tool call made during agent execution."""

    name: str
    args: dict[str, Any] = {}


class ChatResponse(BaseModel):
    """Response from the chat endpoint."""

    response: str
    tool_calls: list[ToolCallInfo] = []


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """Send a message to the AI agent and get a response.

    The agent will reason about the message, call any necessary tools
    (patient lookup, billing, analytics), and return a final answer
    along with the list of tool calls it made.
    """
    ag = get_agent()
    result = await ag.ainvoke(
        {"messages": [("user", request.message)]}
    )

    # Extract tool calls from message history for transparency
    tool_calls = []
    for msg in result["messages"]:
        if hasattr(msg, "tool_calls") and msg.tool_calls:
            for tc in msg.tool_calls:
                tool_calls.append(
                    ToolCallInfo(name=tc["name"], args=tc.get("args", {}))
                )

    # The last message is the agent's final response
    final_content = result["messages"][-1].content

    # Gemini sometimes returns content as a list of blocks
    if isinstance(final_content, list):
        final_response = " ".join(
            block.get("text", "") if isinstance(block, dict) else str(block)
            for block in final_content
        ).strip()
    else:
        final_response = str(final_content)

    return ChatResponse(response=final_response, tool_calls=tool_calls)
