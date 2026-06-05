"""匹配引擎 — SentenceTransformer 语义相似度 + 技能互补 + 兴趣重叠"""
from typing import List, Dict
import numpy as np

from config import SEMANTIC_WEIGHT, COMPLEMENT_WEIGHT, INTEREST_WEIGHT
from utils.text import build_all_texts
from services.embedder_service import embedder


def batch_match(
    source_bio: str,
    source_can: List[str],
    source_want: List[str],
    source_hobbies: List[str],
    candidates: List[dict],
) -> List[dict]:
    """批量对候选用户打分，返回排序后的分数列表。"""
    if not candidates:
        return []

    # 1) SentenceTransformer 语义相似度 — 用预训练模型生成高质量语义向量
    texts = build_all_texts(
        source_bio, source_can, source_want, source_hobbies, candidates
    )
    # 批量编码，第一项是 source，其余是 candidates
    all_vectors = embedder.encode_batch(texts)
    source_vec = np.array(all_vectors[0])
    candidate_vecs = np.array(all_vectors[1:])

    # 向量已归一化（normalize_embeddings=True），余弦相似度 = 点积
    # 直接用 numpy 点积，避免 sklearn cosine_similarity 的数值精度警告
    # clamp 到 [0, 1]：语义向量点积范围 [-1, 1]，负相似度视为 0
    semantic_scores = np.clip(candidate_vecs @ source_vec, 0.0, 1.0)

    # 2) 技能互补 + 兴趣重叠
    source_can_set = set(source_can)
    source_want_set = set(source_want)
    source_hobby_set = set(source_hobbies)
    total_skills = max(len(source_can_set) + len(source_want_set), 1)
    max_hobbies = max(len(source_hobby_set), 1)

    results = []
    for i, c in enumerate(candidates):
        # 技能互补度
        c_can = set(c.get("canSkills", []))
        c_want = set(c.get("wantSkills", []))
        complement = (
            len(source_can_set & c_want) + len(source_want_set & c_can)
        )
        complement_score = min(1.0, complement / total_skills)

        # 兴趣重叠度
        c_hobbies = set(c.get("hobbies", []))
        interest_score = min(
            1.0, len(source_hobby_set & c_hobbies) / max_hobbies
        )

        # 加权融合
        final = (
            float(semantic_scores[i]) * SEMANTIC_WEIGHT
            + complement_score * COMPLEMENT_WEIGHT
            + interest_score * INTEREST_WEIGHT
        )
        results.append(
            {
                "userId": c.get("userId", f"candidate_{i}"),
                "semanticSimilarity": round(float(semantic_scores[i]), 4),
                "skillComplement": round(complement_score, 4),
                "interestOverlap": round(interest_score, 4),
                "score": round(final, 4),
            }
        )

    results.sort(key=lambda x: x["score"], reverse=True)
    return results
