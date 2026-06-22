import os

# Redis
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REDIS_DB = int(os.getenv("REDIS_DB", "0"))
REDIS_PREFIX = "user:embed:"

# Hugging Face 镜像（国内网络需要）
HF_ENDPOINT = os.getenv("HF_ENDPOINT", "https://hf-mirror.com")

# Embedding 模型配置
EMBEDDING_MODEL = os.getenv("AI_EMBEDDING_MODEL", "BAAI/bge-m3")

# AI
MAX_CANDIDATES = int(os.getenv("AI_MAX_CANDIDATES", "200"))
SEMANTIC_WEIGHT = float(os.getenv("AI_SEMANTIC_WEIGHT", "0.3"))
COMPLEMENT_WEIGHT = float(os.getenv("AI_COMPLEMENT_WEIGHT", "0.4"))
INTEREST_WEIGHT = float(os.getenv("AI_INTEREST_WEIGHT", "0.3"))

# Server
HOST = os.getenv("AI_HOST", "0.0.0.0")
PORT = int(os.getenv("AI_PORT", "8000"))

# LLM API Key（环境变量 > config_local.py > 留空不启用）
LLM_API_KEY = os.getenv("LLM_API_KEY", "")

# 本地配置覆盖（不提交 Git，优先级高于默认值，低于环境变量）
try:
    from config_local import *  # noqa: F403
except ImportError:
    pass
