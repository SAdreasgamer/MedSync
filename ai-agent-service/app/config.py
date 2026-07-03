from functools import lru_cache
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Configuration loaded from environment variables."""

    gemini_api_key: str = ""
    openrouter_api_key: str = ""
    openrouter_model: str = "openrouter/free"
    patient_service_url: str = "http://patient-service:4000"
    billing_service_host: str = "billing-service"
    billing_service_grpc_port: int = 9001
    analytics_service_url: str = "http://analytics-service:4002"
    appointment_service_url: str = "http://appointment-service:4006"

    class Config:
        env_file = ".env"


@lru_cache()
def get_settings() -> Settings:
    return Settings()
