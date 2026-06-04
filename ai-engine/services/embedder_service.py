"""Embedding 服务 — 文本转向量 + Redis 缓存。升级到词向量模型时替换此文件。"""
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer

from config import MAX_CANDIDATES
from utils.text import build_profile_text
from utils.redis_client import get_embedding, set_embedding


class EmbedderService:
    """文本向量化服务（单例）"""

    def __init__(self):
        self.vectorizer = None  # phase2 替换为 SentenceTransformer

    def encode(self, text):
        """将文本转为向量。Phase1 用 TF-IDF，Phase2 换词向量模型"""
        if self.vectorizer is None:
            self.vectorizer = TfidfVectorizer(max_features=500)
            # 初始化一个空 fit
            self.vectorizer.fit(["初始化占位文本"])
        vec = self.vectorizer.transform([text]).toarray()[0]
        return vec.tolist()

    def get_or_build_embedding(self, user_id: str, bio: str, can_skills, want_skills, hobbies) -> list[float]:
        """获取用户向量：先查 Redis 缓存，miss 则计算并写入"""
        cached = get_embedding(user_id)
        if cached is not None:
            return cached
        text = build_profile_text(bio, can_skills, want_skills, hobbies)
        vector = self.encode(text)
        set_embedding(user_id, vector)
        return vector

    def batch_update(self, users: list[dict]) -> int:
        """批量更新用户向量（异步调用）"""
        count = 0
        for u in users:
            uid = u.get("userId")
            text = build_profile_text(
                u.get("bio", ""),
                u.get("canSkills", []),
                u.get("wantSkills", []),
                u.get("hobbies", []),
            )
            vector = self.encode(text)
            set_embedding(uid, vector)
            count += 1
        return count


# 全局单例
embedder = EmbedderService()
