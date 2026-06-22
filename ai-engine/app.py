"""SkillMatch AI Engine — FastAPI 主入口

启动: uvicorn app:app --host 0.0.0.0 --port 8000
"""

import time
from typing import List, Optional

from fastapi import FastAPI
from pydantic import BaseModel

from services.matcher import batch_match
from services.embedder_service import embedder
from services.explainer import explain_match

app = FastAPI(title="SkillMatch AI Engine", version="0.1.0")


# ====== 请求/响应模型 ======

class CandidateProfile(BaseModel):
    userId: str
    bio: str = ""
    canSkills: List[str] = []
    wantSkills: List[str] = []
    hobbies: List[str] = []


class MatchRequest(BaseModel):
    source: CandidateProfile
    candidates: List[CandidateProfile]


class MatchScore(BaseModel):
    userId: str
    semanticSimilarity: float
    skillComplement: float
    interestOverlap: float
    score: float


class MatchResponse(BaseModel):
    scores: List[MatchScore]
    cost_ms: float


class EmbedRequest(BaseModel):
    userId: str
    bio: str = ""
    canSkills: List[str] = []
    wantSkills: List[str] = []
    hobbies: List[str] = []


class EmbedResponse(BaseModel):
    userId: str
    vector: List[float]
    dim: int


class BatchUpdateRequest(BaseModel):
    users: List[CandidateProfile]


class BatchUpdateResponse(BaseModel):
    updated: int


class ExplainRequest(BaseModel):
    sourceName: str = ""
    sourceBio: str = ""
    sourceCanSkills: List[str] = []
    sourceWantSkills: List[str] = []
    sourceHobbies: List[str] = []
    targetName: str = ""
    targetBio: str = ""
    targetCanSkills: List[str] = []
    targetWantSkills: List[str] = []
    targetHobbies: List[str] = []
    # 非敏感 LLM 配置（由 Java 端从 application.yaml 读取后传入）
    # API Key 不在请求体中，通过 AI Engine 的环境变量注入
    llmBaseUrl: str = "https://api.deepseek.com/v1"
    llmModel: str = "deepseek-chat"
    llmTimeout: int = 8


class ExplainResponse(BaseModel):
    reason: Optional[str] = None


# ====== 端点 ======

@app.post("/api/ai/match", response_model=MatchResponse)
async def match(request: MatchRequest):
    """批量语义匹配 — 基于 SentenceTransformer 对候选用户打分并排序"""
    start = time.perf_counter()
    src = request.source
    cands = [c.model_dump() for c in request.candidates]
    scores = batch_match(
        source_bio=src.bio,
        source_can=src.canSkills,
        source_want=src.wantSkills,
        source_hobbies=src.hobbies,
        candidates=cands,
    )
    cost = (time.perf_counter() - start) * 1000
    return MatchResponse(
        scores=[MatchScore(**s) for s in scores],
        cost_ms=round(cost, 2),
    )


@app.post("/api/ai/embed", response_model=EmbedResponse)
async def embed(request: EmbedRequest):
    """文本转向量 + 写入 Redis 缓存"""
    vector = embedder.get_or_build_embedding(
        request.userId, request.bio,
        request.canSkills, request.wantSkills, request.hobbies,
    )
    return EmbedResponse(userId=request.userId, vector=vector, dim=len(vector))


@app.post("/api/ai/embed/batch", response_model=BatchUpdateResponse)
async def embed_batch(request: BatchUpdateRequest):
    """批量更新用户向量"""
    count = embedder.batch_update([u.model_dump() for u in request.users])
    return BatchUpdateResponse(updated=count)


@app.post("/api/ai/match/explain", response_model=ExplainResponse)
async def explain(request: ExplainRequest):
    """LLM 匹配解释 — 生成自然语言匹配原因（LLM 配置由 Java 端传入）"""
    reason = explain_match(
        source_name=request.sourceName,
        source_bio=request.sourceBio,
        source_can=request.sourceCanSkills,
        source_want=request.sourceWantSkills,
        source_hobbies=request.sourceHobbies,
        target_name=request.targetName,
        target_bio=request.targetBio,
        target_can=request.targetCanSkills,
        target_want=request.targetWantSkills,
        target_hobbies=request.targetHobbies,
        llm_base_url=request.llmBaseUrl,
        llm_model=request.llmModel,
        llm_timeout=request.llmTimeout,
    )
    return ExplainResponse(reason=reason)


@app.get("/api/ai/health")
async def health():
    return {"status": "ok", "version": "0.1.0"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app:app", host="0.0.0.0", port=8000, reload=False)
