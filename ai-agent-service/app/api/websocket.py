"""WebSocket endpoint for streaming agent responses."""

import json
import traceback

from fastapi import APIRouter, WebSocket, WebSocketDisconnect

from app.agent.graph import build_agent

router = APIRouter()

# Reuse agent instance
agent = None


def get_agent():
    """Lazy-init the agent."""
    global agent
    if agent is None:
        agent = build_agent()
    return agent


@router.websocket("/stream")
async def stream(websocket: WebSocket):
    """WebSocket endpoint that streams agent reasoning in real-time.

    Client sends: {"message": "your question here"}
    Server streams back events:
      - {"type": "token", "content": "..."} — LLM text tokens
      - {"type": "tool_start", "name": "...", "input": "..."} — tool invocation
      - {"type": "tool_end", "name": "...", "output": "..."} — tool result
      - {"type": "done"} — agent finished
      - {"type": "error", "content": "..."} — error occurred
    """
    await websocket.accept()

    try:
        # Receive the user's message
        data = await websocket.receive_json()
        user_message = data.get("message", "")

        if not user_message:
            await websocket.send_json(
                {"type": "error", "content": "No message provided"}
            )
            await websocket.close()
            return

        ag = get_agent()

        # Stream events from the agent
        async for event in ag.astream_events(
            {"messages": [("user", user_message)]},
            version="v2",
        ):
            kind = event["event"]

            if kind == "on_chat_model_stream":
                chunk = event["data"]["chunk"]
                if hasattr(chunk, "content") and chunk.content:
                    await websocket.send_json(
                        {"type": "token", "content": chunk.content}
                    )

            elif kind == "on_tool_start":
                await websocket.send_json(
                    {
                        "type": "tool_start",
                        "name": event.get("name", "unknown"),
                        "input": str(
                            event.get("data", {}).get("input", "")
                        ),
                    }
                )

            elif kind == "on_tool_end":
                output = event.get("data", "")
                if hasattr(output, "content"):
                    output = output.content
                await websocket.send_json(
                    {
                        "type": "tool_end",
                        "name": event.get("name", "unknown"),
                        "output": str(output),
                    }
                )

        await websocket.send_json({"type": "done"})

    except WebSocketDisconnect:
        pass
    except Exception as e:
        try:
            await websocket.send_json(
                {"type": "error", "content": str(e)}
            )
        except Exception:
            pass
        traceback.print_exc()
    finally:
        try:
            await websocket.close()
        except Exception:
            pass
