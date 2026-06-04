"""Profile 文本构建 — 将用户多字段拼接为可用于 TF-IDF 的扁平文本"""
from typing import Optional, List


def build_profile_text(
    bio: str = "",
    can_skills: Optional[list] = None,
    want_skills: Optional[list] = None,
    hobbies: Optional[list] = None,
) -> str:
    """拼接用户所有文本特征，空格分隔"""
    parts = []
    if bio:
        parts.append(bio)
    if can_skills:
        parts.append("会 " + " ".join(can_skills))
    if want_skills:
        parts.append("想学 " + " ".join(want_skills))
    if hobbies:
        parts.append("爱好 " + " ".join(hobbies))
    return " ".join(parts)


def build_all_texts(
    source_bio: str,
    source_can: list,
    source_want: list,
    source_hobbies: list,
    candidates: list,
) -> list:
    """构建源用户 + 全部候选用户的文本列表（给 TF-IDF 批量 transform）"""
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
