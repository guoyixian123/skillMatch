import os

# Redis
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REDIS_DB = int(os.getenv("REDIS_DB", "0"))
REDIS_PREFIX = "user:embed:"

# AI
MAX_CANDIDATES = int(os.getenv("AI_MAX_CANDIDATES", "200"))
TFIDF_WEIGHT = float(os.getenv("AI_TFIDF_WEIGHT", "0.3"))
COMPLEMENT_WEIGHT = float(os.getenv("AI_COMPLEMENT_WEIGHT", "0.4"))
INTEREST_WEIGHT = float(os.getenv("AI_INTEREST_WEIGHT", "0.3"))

# Server
HOST = os.getenv("AI_HOST", "0.0.0.0")
PORT = int(os.getenv("AI_PORT", "8000"))
