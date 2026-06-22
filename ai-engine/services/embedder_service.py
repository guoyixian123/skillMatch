"""Embedding 服务 — 使用 SentenceTransformer 生成语义向量 + Redis 缓存。"""
import os
import numpy as np
from sentence_transformers import SentenceTransformer

from config import EMBEDDING_MODEL, HF_ENDPOINT
from utils.text import build_profile_text
from utils.redis_client import get_embedding, set_embedding

# 设置 Hugging Face 镜像（国内网络必需）
os.environ["HF_ENDPOINT"] = HF_ENDPOINT


class EmbedderService:
    """文本向量化服务（单例），基于 SentenceTransformer 语义模型。"""

    def __init__(self):
        self.model = None  # 延迟加载，首次调用时初始化

    def _ensure_model(self):
        """延迟加载 SentenceTransformer 模型，避免启动时阻塞。"""
        if self.model is None:
            print(f"[Embedder] 正在加载 SentenceTransformer 模型: {EMBEDDING_MODEL}")
            print(f"[Embedder] 使用 Hugging Face 镜像: {HF_ENDPOINT}")
            try:
                self.model = SentenceTransformer(EMBEDDING_MODEL)
                dim = self.model.get_sentence_embedding_dimension()
                print(f"[Embedder] 模型加载完成，向量维度: {dim}")
                # 预热: 避免首次请求冷启动延迟
                _ = self.model.encode("预热", normalize_embeddings=True)
                print(f"[Embedder] 模型预热完成")
            except Exception as e:
                print(f"[Embedder] 模型加载失败: {e}")
                print(f"[Embedder] 请检查网络连接或设置 HF_ENDPOINT 环境变量")
                raise

    def encode(self, text: str) -> list[float]:
        """将文本转为语义向量，基于 SentenceTransformer。"""
        self._ensure_model()
        embedding = self.model.encode(text, normalize_embeddings=True)
        return embedding.tolist()

    def encode_batch(self, texts: list[str]) -> list[list[float]]:
        """批量将文本转为语义向量，效率更高。"""
        self._ensure_model()
        embeddings = self.model.encode(texts, normalize_embeddings=True, batch_size=32)
        return [vec.tolist() for vec in embeddings]

    def get_or_build_embedding(self, user_id: str, bio: str, can_skills, want_skills, hobbies) -> list[float]:
        """获取用户向量：先查 Redis 缓存，miss 则计算并写入。"""
        cached = get_embedding(user_id)
        if cached is not None:
            return cached
        text = build_profile_text(bio, can_skills, want_skills, hobbies)
        vector = self.encode(text)
        set_embedding(user_id, vector)
        return vector

    def batch_update(self, users: list[dict]) -> int:
        """批量更新用户向量（异步调用）。"""
        count = 0
        # 收集所有需要计算的文本
        uid_text_pairs = []
        for u in users:
            uid = u.get("userId")
            text = build_profile_text(
                u.get("bio", ""),
                u.get("canSkills", []),
                u.get("wantSkills", []),
                u.get("hobbies", []),
            )
            uid_text_pairs.append((uid, text))

        # 批量编码，效率远高于逐条 encode
        if uid_text_pairs:
            texts = [pair[1] for pair in uid_text_pairs]
            vectors = self.encode_batch(texts)
            for (uid, _), vector in zip(uid_text_pairs, vectors):
                set_embedding(uid, vector)
                count += 1

        return count


# 全局单例
embedder = EmbedderService()
