"""Redis 连接池 — 用于缓存用户 embedding 向量"""
import json
import redis
from config import REDIS_HOST, REDIS_PORT, REDIS_DB, REDIS_PREFIX

_pool = None


def _get_pool():
    global _pool
    if _pool is None:
        _pool = redis.ConnectionPool(
            host=REDIS_HOST, port=REDIS_PORT, db=REDIS_DB,
            decode_responses=True, max_connections=10,
        )
    return _pool


def get_client():
    return redis.Redis(connection_pool=_get_pool())


def get_embedding(user_id):
    """从 Redis 读取缓存的 embedding 向量"""
    raw = get_client().hget(f"{REDIS_PREFIX}{user_id}", "vector")
    if raw is None:
        return None
    return json.loads(raw)


def set_embedding(user_id, vector):
    """将 embedding 向量写入 Redis 缓存"""
    get_client().hset(
        f"{REDIS_PREFIX}{user_id}",
        mapping={"vector": json.dumps(vector)},
    )
