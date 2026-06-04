#!/usr/bin/env python3
"""Bot 用户生成脚本 — 通过 HTTP API 调用，无需直连数据库。
用法: python bot_api_generator.py [数量] [base_url]  (默认 10, http://localhost:8080)
依赖: pip install requests
"""

import sys
import random
import time
import requests

# ==================== 配置 ====================
BASE_URL = sys.argv[2] if len(sys.argv) > 2 else "http://localhost:8080"

# ==================== 素材池 ====================
SURNAMES = "张李王赵陈杨黄周吴徐孙马胡朱郭何罗高林郑梁谢唐许冯宋韩邓彭曹曾田萧潘袁蔡"
GIVEN_NAMES = [
    "伟","芳","敏","静","丽","强","磊","洋","艳","勇","军","杰",
    "娜","秀英","涛","明","超","秀兰","霞","平","刚","桂英","文",
    "华","飞","玉兰","桂花","波","斌","玲","婷","宇","浩","然","博",
    "思远","雨桐","一鸣","天宇","欣怡","子涵","梓轩","诗涵","皓轩",
]

CAN_SKILLS = [
    "Python","Java","JavaScript","TypeScript","C++","Go","Rust","PHP","Swift","Kotlin",
    "Vue.js","React","Angular","HTML/CSS","微信小程序","Spring Boot","Node.js","Django","Flask",
    "MySQL","Redis","MongoDB",
    "机器学习","深度学习","数据分析","TensorFlow","PyTorch","NLP自然语言处理",
    "UI设计","UX设计","Figma","Photoshop","Illustrator","平面设计",
    "吉他","钢琴","声乐","音乐制作","架子鼓",
    "英语","日语","韩语","法语","德语",
    "健身指导","瑜伽","游泳","篮球","羽毛球",
    "摄影","视频剪辑","写作","演讲","PPT制作",
]
WANT_SKILLS = [
    "Python","Java","JavaScript","TypeScript","C++","Go","Rust","PHP","Swift","Kotlin",
    "Vue.js","React","Angular","HTML/CSS","微信小程序","Spring Boot","Node.js","Django","Flask",
    "MySQL","Redis","MongoDB",
    "机器学习","深度学习","数据分析","TensorFlow","PyTorch","NLP自然语言处理",
    "UI设计","UX设计","Figma","Photoshop","Illustrator","平面设计",
    "吉他","钢琴","声乐","音乐制作","架子鼓",
    "英语","日语","韩语","法语","德语",
    "健身指导","瑜伽","游泳","篮球","羽毛球",
    "摄影","视频剪辑","写作","演讲","PPT制作",
]
HOBBY_NAMES = [
    "篮球","足球","羽毛球","游泳","跑步","健身","瑜伽","乒乓球",
    "摄影","绘画","书法","手工","吉他","钢琴","唱歌","架子鼓",
    "编程","机器人","电子制作","3D打印","烹饪","烘焙","旅行",
    "阅读","电影","游戏","宠物","数学","物理","天文","历史",
]
HOBBY_ICONS = [
    "🏀","⚽","🏸","🏊","🏃","💪","🧘","🏓",
    "📷","🎨","✒️","✂️","🎸","🎹","🎤","🥁",
    "💻","🤖","🔧","🖨️","🍳","🍰","✈️",
    "📖","🎬","🎮","🐕","🔢","⚛️","🔭","📜",
]

CITIES = [
    ("北京", 39.9042, 116.4074), ("上海", 31.2304, 121.4737),
    ("广州", 23.1291, 113.2644), ("深圳", 22.5431, 114.0579),
    ("杭州", 30.2741, 120.1551), ("成都", 30.5728, 104.0668),
    ("武汉", 30.5928, 114.3055), ("南京", 32.0603, 118.7969),
    ("西安", 34.3416, 108.9398), ("重庆", 29.4316, 106.9123),
    ("长沙", 28.2282, 112.9388), ("郑州", 34.7466, 113.6254),
    ("苏州", 31.2990, 120.5853), ("天津", 39.3434, 117.3616),
    ("青岛", 36.0671, 120.3826), ("大连", 38.9140, 121.6147),
    ("厦门", 24.4798, 118.0894), ("昆明", 25.0389, 102.7183),
    ("合肥", 31.8206, 117.2272), ("福州", 26.0745, 119.2965),
    ("哈尔滨",45.8038, 126.5350), ("沈阳", 41.8057, 123.4315),
    ("济南", 36.6512, 116.9972), ("太原", 37.8706, 112.5489),
    ("贵阳", 26.6470, 106.6302), ("南宁", 22.8170, 108.3665),
    ("南昌", 28.6820, 115.8579), ("长春", 43.8178, 125.3235),
    ("兰州", 36.0611, 103.8343), ("海口", 20.0440, 110.1999),
]

POST_TITLES = [
    "零基础学{}，有人一起吗？",
    "分享我的{}学习心得",
    "周末{}活动组队",
    "想找个{}搭子",
    "{}小白求带",
    "今天练习{}的第30天",
    "有人想一起{}吗？",
    "免费教{}，交个朋友",
]
POST_BODIES = [
    "纯新手，希望能找到志同道合的朋友一起进步。线上线下都可以，主要还是想多认识一些有共同爱好的朋友～",
    "学了快一年了，有点心得体会。最近想找个搭子互相督促，一起打卡练习。感兴趣的可以私信我！",
    "周末有空，想组个小局。地点可以商量，主要是想找几个喜欢这个的朋友一起玩。",
    "刚开始接触，什么都不懂。有大佬愿意带带吗？我请你喝奶茶！",
    "之前一直自己玩，有点无聊。想找个组织或者搭子，一起交流进步。",
]
COMMENT_TEXTS = [
    "写得不错，学习了！", "太棒了，一起加油！", "好帖，顶一下～",
    "深有同感！", "大佬带带我！", "已收藏，谢谢分享",
]


def random_name():
    return random.choice(SURNAMES) + random.choice(GIVEN_NAMES)


def random_bot_id():
    ts = str(int(time.time() * 1000))
    suffix = str(random.randint(0, 9999)).zfill(4)
    return ts[:8] + suffix[:4] + "8080"


def pick(arr, lo, hi):
    pool = list(arr)
    random.shuffle(pool)
    n = random.randint(lo, min(hi, len(pool)))
    return pool[:n]


def ok(resp):
    """检查响应是否成功，返回解析后的 data"""
    if resp.status_code != 200:
        return None
    body = resp.json()
    if body.get("code") != 200:
        print(f"    API error: {body.get('message', 'unknown')}")
        return None
    return body.get("data")


def generate(count: int):
    session = requests.Session()
    base = BASE_URL.rstrip("/")

    for i in range(1, count + 1):
        bot_id = random_bot_id()
        name = random_name()
        password = "123321"
        city, base_lat, base_lng = random.choice(CITIES)
        lat = base_lat + random.uniform(-0.3, 0.3)
        lng = base_lng + random.uniform(-0.3, 0.3)
        signature = "技能交换bot，测试用"
        bio = f"我是自动化测试bot #{i}，坐标{city}。来找我交换技能吧～"

        try:
            # ===== 1. 注册 =====
            resp = session.post(f"{base}/api/auth/register", json={
                "userId": bot_id, "name": name, "password": password,
            })
            if resp.status_code != 200:
                print(f"[{i}/{count}] 注册失败 {name}: {resp.status_code}")
                continue

            # ===== 2. 登录 =====
            resp = session.post(f"{base}/api/auth/login", json={
                "userId": bot_id, "name": name, "password": password,
            })
            data = ok(resp)
            if not data:
                print(f"[{i}/{count}] 登录失败 {name}: {resp.status_code}")
                continue
            token = data.get("token", "")
            if not token:
                print(f"[{i}/{count}] 未获取到 token")
                continue
            headers = {"user_info": token}

            # ===== 3. 获取合法爱好标签（首次请求后缓存）=====
            if "hobby_tags" not in generate.__dict__:
                resp = session.get(f"{base}/api/tags/hobbies", headers=headers)
                if resp.status_code == 200:
                    tags_data = resp.json().get("data", {})
                    generate.hobby_tags = []
                    for category, items in tags_data.items():
                        for item in items:
                            generate.hobby_tags.append(
                                (item.get("name", ""), item.get("icon", "⭐"))
                            )
                    if not generate.hobby_tags:
                        generate.hobby_tags = list(zip(HOBBY_NAMES, HOBBY_ICONS))
                else:
                    generate.hobby_tags = list(zip(HOBBY_NAMES, HOBBY_ICONS))

            # ===== 4. 完善个人资料 =====
            ok(session.put(f"{base}/api/user/profile", json={
                "signature": signature, "bio": bio,
            }, headers=headers))

            # ===== 5. 更新位置 =====
            ok(session.put(f"{base}/api/user/location", json={
                "longitude": lng, "latitude": lat,
            }, headers=headers))

            # ===== 6. 批量保存技能（can 和 want 不重复）=====
            can = pick(CAN_SKILLS, 2, 4)
            want_pool = [s for s in WANT_SKILLS if s not in can]
            want = pick(want_pool, 2, 3) if len(want_pool) >= 2 else want_pool
            ok(session.put(f"{base}/api/user/skills", json={
                "canSkills": can, "wantSkills": want,
            }, headers=headers))

            # ===== 7. 逐个添加爱好（使用 API 返回的合法标签）=====
            pool = list(generate.hobby_tags)
            random.shuffle(pool)
            n = random.randint(2, min(5, len(pool)))
            for hn, icon in pool[:n]:
                ok(session.post(f"{base}/api/user/hobbies", json={
                    "hobbyName": hn, "icon": icon,
                }, headers=headers))

            # ===== 8. 发 1~2 篇帖子 (标签必须 # 开头) =====
            post_count = 0
            for _ in range(random.randint(1, 2)):
                skill = random.choice(CAN_SKILLS)
                title = random.choice(POST_TITLES).format(skill)
                body = random.choice(POST_BODIES)
                resp = session.post(f"{base}/api/community/posts", json={
                    "title": title,
                    "body": body,
                    "tags": [f"#{skill}"],
                }, headers=headers)
                if resp.status_code == 200:
                    post_count += 1
                else:
                    print(f"    发帖失败: {resp.status_code} {resp.text[:100]}")

            # ===== 9. 随机 10~20 次点赞 + 几条评论 =====
            other_posts = []
            other_users = set()
            resp = session.get(
                f"{base}/api/community/posts?page=1&size=50&sort=latest",
                headers=headers,
            )
            if resp.status_code == 200:
                all_posts = resp.json().get("data", {}).get("list", [])
                for p in all_posts:
                    author = p.get("author", {})
                    author_id = author.get("id")
                    if author_id != bot_id:
                        other_posts.append(p)
                        other_users.add(author_id)
            else:
                print(f"    获取帖子列表失败: HTTP={resp.status_code}")

            if not all_posts:
                print(f"    全局帖子列表为空 (all=0)")
            elif not other_posts:
                print(f"    无其他用户帖子 (all={len(all_posts)} other=0)")

            total_likes = random.randint(10, 20)
            like_count = 0
            comment_count = 0

            if other_posts or other_users:
                # 构建候选池：帖子 + 作者，混合打乱后取 10~20 个
                candidates = []
                for p in other_posts:
                    candidates.append(("post", p.get("id")))
                for uid in other_users:
                    candidates.append(("profile", uid))
                random.shuffle(candidates)

                comment_targets = random.sample(other_posts, min(3, len(other_posts)))

                first_like_err = None
                for biz_type, biz_id in candidates[:total_likes]:
                    typ = 2 if biz_type == "post" else 1
                    resp = session.post(f"{base}/api/like", json={
                        "bizId": biz_id, "type": typ,
                    }, headers=headers)
                    if resp.status_code == 200 and resp.json().get("code") == 200:
                        like_count += 1
                    elif first_like_err is None:
                        first_like_err = f"HTTP={resp.status_code} body={resp.text[:200]}"

                if first_like_err:
                    print(f"    点赞首错: {first_like_err}")

                # 随机评论 1~3 条
                first_cmt_err = None
                for p in comment_targets:
                    comment = random.choice(COMMENT_TEXTS)
                    resp = session.post(
                        f"{base}/api/community/posts/{p.get('id')}/comments",
                        json=comment,
                        headers=headers,
                    )
                    if resp.status_code == 200:
                        comment_count += 1
                    elif first_cmt_err is None:
                        first_cmt_err = f"HTTP={resp.status_code} body={resp.text[:200]}"

                if first_cmt_err:
                    print(f"    评论首错: {first_cmt_err}")
            else:
                print(f"    无其他用户帖子可互动")

            print(f"[{i}/{count}] {name} ({city})  ID={bot_id}  "
                  f"posts={post_count}  likes={like_count}/{total_likes}  "
                  f"comments={comment_count}  can={can}  want={want}")

        except requests.RequestException as e:
            print(f"[{i}/{count}] 网络错误 {name}: {e}")
        except Exception as e:
            print(f"[{i}/{count}] 未知错误 {name}: {type(e).__name__}: {e}")

    print(f"\n搞定！生成了 {count} 个 Bot 用户。")


if __name__ == "__main__":
    n = int(sys.argv[1]) if len(sys.argv) > 1 else 10
    generate(n)
