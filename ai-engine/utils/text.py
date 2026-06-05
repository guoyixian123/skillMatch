"""Profile 文本构建 — 将用户多字段拼接为语义丰富的自然语言文本。

SentenceTransformer 比 TF-IDF 更擅长理解自然语言，因此我们构建更接近真实描述的文本，
而非简单的关键词拼接。
"""
from typing import Optional, List


def build_profile_text(
    bio: str = "",
    can_skills: Optional[list] = None,
    want_skills: Optional[list] = None,
    hobbies: Optional[list] = None,
) -> str:
    """构建用户画像的自然语言描述，适配 SentenceTransformer 语义理解。"""
    parts = []

    # bio 权重最高，重复一次以增强语义影响
    if bio:
        parts.append(bio)
        parts.append(bio)

    # 技能用自然语言描述，帮助模型理解"会"和"想学"的区别
    if can_skills:
        skills_str = "、".join(can_skills)
        parts.append(f"擅长{skills_str}")

    if want_skills:
        skills_str = "、".join(want_skills)
        parts.append(f"想学习{skills_str}")

    # 爱好描述
    if hobbies:
        hobbies_str = "、".join(hobbies)
        parts.append(f"兴趣爱好是{hobbies_str}")

    return "。".join(parts) if parts else ""


def build_all_texts(
    source_bio: str,
    source_can: list,
    source_want: list,
    source_hobbies: list,
    candidates: list,
) -> list:
    """构建源用户 + 全部候选用户的文本列表，供 SentenceTransformer 批量编码。"""
    texts = [
        build_profile_text(source_bio, source_can, source_want, source_hobbies)
    ]
    for c in candidates:
        texts.append(
            build_profile_text(
                c.get("bio", ""),
                c.get("canSkills", []),
                c.get("wantSkills", []),
                c.get("hobbies", []),
            )
        )
    return texts
