"""LangGraph ReAct agent — the core reasoning engine."""

from langchain_google_genai import ChatGoogleGenerativeAI
from langgraph.graph import StateGraph, MessagesState
from langgraph.prebuilt import ToolNode, tools_condition

from app.agent.prompts import SYSTEM_PROMPT
from app.config import get_settings
from app.tools.patient_tools import (
    get_all_patients,
    get_patient_by_id,
    search_patients,
    create_patient,
    update_patient,
    delete_patient,
    admit_patient,
    discharge_patient,
)
from app.tools.billing_tools import (
    create_billing_account,
    get_billing_status,
    add_invoice,
    record_payment,
    get_billing_details,
)
from app.tools.analytics_tools import get_patient_count, get_recent_events
from app.tools.appointment_tools import (
    schedule_appointment,
    get_patient_appointments,
    get_today_appointments,
    get_appointments_by_date,
    update_appointment_status,
)


def build_agent():
    """Build and compile the LangGraph ReAct agent."""
    settings = get_settings()

    if settings.openrouter_api_key:
        from langchain_openai import ChatOpenAI
        llm = ChatOpenAI(
            model=settings.openrouter_model or "meta-llama/llama-3-8b-instruct:free",
            openai_api_key=settings.openrouter_api_key,
            openai_api_base="https://openrouter.ai/api/v1",
            temperature=0.1,
        )
    else:
        llm = ChatGoogleGenerativeAI(
            model="gemini-2.5-flash",
            google_api_key=settings.gemini_api_key,
            temperature=0.1,
        )

    # All tools the agent can use
    all_tools = [
        get_all_patients,
        get_patient_by_id,
        search_patients,
        create_patient,
        update_patient,
        delete_patient,
        admit_patient,
        discharge_patient,
        create_billing_account,
        get_billing_status,
        add_invoice,
        record_payment,
        get_billing_details,
        get_patient_count,
        get_recent_events,
        schedule_appointment,
        get_patient_appointments,
        get_today_appointments,
        get_appointments_by_date,
        update_appointment_status,
    ]

    llm_with_tools = llm.bind_tools(all_tools)

    def reasoning_node(state: MessagesState):
        """The LLM reasoning step — decides what to do next."""
        # Prepend system prompt to every invocation
        messages = [("system", SYSTEM_PROMPT)] + state["messages"]
        response = llm_with_tools.invoke(messages)
        return {"messages": [response]}

    # Build the graph
    graph = StateGraph(MessagesState)
    graph.add_node("reason", reasoning_node)
    graph.add_node("tools", ToolNode(all_tools))

    # Entry point
    graph.set_entry_point("reason")

    # Conditional edge: if LLM wants to call a tool → tools node,
    # otherwise → END
    graph.add_conditional_edges("reason", tools_condition)

    # After tool execution, go back to reasoning
    graph.add_edge("tools", "reason")

    return graph.compile()
