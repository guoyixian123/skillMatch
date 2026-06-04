"""测试匹配引擎"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from services.matcher import batch_match


def test_batch_match():
    source_bio = "3年前端开发，坐标北京，想学后端"
    source_can = ["Vue.js", "React", "JavaScript", "CSS"]
    source_want = ["Spring Boot", "Python", "摄影"]
    source_hobbies = ["跑步", "电影", "篮球"]

    candidates = [
        {
            "userId": "u1",
            "bio": "Java后端5年，想学前端",
            "canSkills": ["Spring Boot", "Python", "MySQL"],
            "wantSkills": ["Vue.js", "React"],
            "hobbies": ["足球", "电影", "跑步"],
        },
        {
            "userId": "u2",
            "bio": "UI设计师，喜欢摄影",
            "canSkills": ["Figma", "Photoshop"],
            "wantSkills": ["摄影技巧", "插画"],
            "hobbies": ["摄影", "绘画", "旅行"],
        },
        {
            "userId": "u3",
            "bio": "数据分析师，想学Python进阶",
            "canSkills": ["Python", "SQL", "Tableau"],
            "wantSkills": ["机器学习", "深度学习"],
            "hobbies": ["编程", "阅读", "游戏"],
        },
    ]

    results = batch_match(source_bio, source_can, source_want, source_hobbies, candidates)

    print("AI 匹配结果:")
    for r in results:
        print(f"  {r['userId']}: score={r['score']:.4f} "
              f"(tfidf={r['tfidfSimilarity']:.4f} "
              f"complement={r['skillComplement']:.4f} "
              f"interest={r['interestOverlap']:.4f})")

    assert results[0]["userId"] == "u1", "u1 应该排第一（技能互补最高）"
    assert all(r["score"] > 0 for r in results), "所有分数 > 0"
    print("\n✅ 测试通过!")


if __name__ == "__main__":
    test_batch_match()
