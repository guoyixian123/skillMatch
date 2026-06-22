"""匹配解释服务 — 使用 LLM 生成自然语言匹配原因。"""

from typing import List, Optional

import httpx

from config import LLM_API_KEY
from utils.text import build_profile_text


def explain_match(
    source_name: str,
    source_bio: str,
    source_can: List[str],
    source_want: List[str],
    source_hobbies: List[str],
    target_name: str,
    target_bio: str,
    target_can: List[str],
    target_want: List[str],
    target_hobbies: List[str],
    *,
    llm_base_url: str = "https://api.deepseek.com/v1",
    llm_model: str = "deepseek-chat",
    llm_timeout: int = 8,
) -> Optional[str]:
    """
    用 LLM 生成两个用户之间的自然语言匹配解释。

    API Key 通过环境变量注入（不出 AI Engine 进程），
    base_url / model 等非敏感配置由调用方（Java 端 application.yaml）传入。

    返回一句话解释，失败时返回 None（上游降级显示）。
    """
    if not LLM_API_KEY:
        print("[Explainer] LLM_API_KEY 未配置，跳过匹配解释")
        return None

    source_text = build_profile_text(source_bio, source_can, source_want, source_hobbies)
    target_text = build_profile_text(target_bio, target_can, target_want, target_hobbies)

    prompt = f"""你是一个技能社交平台的匹配助手。请用一句话（不超过40字）解释为什么这两个用户适合技能交换。

用户A（{source_name}）：{source_text}

用户B（{target_name}）：{target_text}

从技能互补、兴趣重叠的角度，生成一句自然、友好的解释。不要重复两人的名字，直接说原因。
只输出解释文本，不要加引号或任何前缀。"""

    try:
        with httpx.Client(timeout=llm_timeout) as client:
            resp = client.post(
                f"{llm_base_url}/chat/completions",
                headers={
                    "Authorization": f"Bearer {LLM_API_KEY}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": llm_model,
                    "messages": [
                        {"role": "system", "content": "你是一个简洁友好的匹配助手，输出不超过40字。"},
                        {"role": "user", "content": prompt},
                    ],
                    "max_tokens": 80,
                    "temperature": 0.7,
                },
            )
            if resp.status_code != 200:
                print(f"[Explainer] LLM 调用失败: HTTP {resp.status_code} — {resp.text[:200]}")
                return None

            data = resp.json()
            content = data["choices"][0]["message"]["content"].strip()
            content = content.strip('"\'""'' ')

            if len(content) > 80:
                content = content[:77] + "..."

            print(f"[Explainer] 生成解释: {content}")
            return content

    except httpx.TimeoutException:
        print(f"[Explainer] LLM 调用超时 ({llm_timeout}s)")
        return None
    except Exception as e:
        print(f"[Explainer] LLM 调用异常: {e}")
        return None
