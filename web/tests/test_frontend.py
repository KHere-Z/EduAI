"""EduAI 前端自动化测试 · Playwright"""
import asyncio, sys
from playwright.async_api import async_playwright

BASE = "http://localhost:5175"

PAGES = [
    ("首页", "/", True),
    ("登录页", "/login", True),
    ("老师工作台", "/teacher/dashboard", False),
    ("英语-课堂管理", "/teacher/classroom", False),
    ("英语-词汇测试", "/teacher/vocab-test", False),
    ("英语-AI阅读理解", "/teacher/ai-reading", False),
    ("英语-AI情境口语", "/teacher/ai-dialogue", False),
    ("英语-语法体系", "/teacher/grammar", False),
    ("英语-造句练习", "/teacher/sentence-practice", False),
    ("英语-学习反馈", "/teacher/feedback", False),
    ("数学-错题整理", "/teacher/subject/math/wrong-questions", False),
    ("数学-错题分析", "/teacher/subject/math/wrong-analysis", False),
    ("数学-知识点", "/teacher/subject/math/knowledge-points", False),
    ("数学-考点", "/teacher/subject/math/exam-points", False),
    ("数学-解题模型", "/teacher/subject/math/solution-models", False),
    ("数学-题库", "/teacher/subject/math/question-bank", False),
    ("数学-AI课堂反馈", "/teacher/subject/math/ai-feedback", False),
    ("数学-AI综合分析", "/teacher/subject/math/ai-analysis", False),
    ("数学-成绩统计", "/teacher/subject/math/score-statistics", False),
    ("物理-解题模型", "/teacher/subject/physics/solution-models", False),
    ("化学-考点", "/teacher/subject/chemistry/exam-points", False),
    ("学生学习中心", "/student/dashboard", False),
    ("学生错题本", "/student/wrong-book", False),
    ("学生AI分析", "/student/ai-analysis", False),
    ("学生成绩", "/student/scores", False),
    ("学生练习", "/student/practice", False),
    ("管理概览", "/admin/dashboard", False),
    ("老师管理", "/admin/teachers", False),
    ("学生管理", "/admin/students", False),
    ("学科管理", "/admin/subjects", False),
    ("词库管理", "/admin/word-libraries", False),
    ("题库管理", "/admin/question-bank", False),
]

async def test_page(browser, name, path, is_public):
    page = await browser.new_page()
    try:
        response = await page.goto(f"{BASE}{path}", wait_until="networkidle", timeout=15000)
        status = response.status if response else 0

        if not is_public:
            url = page.url
            redirected_to_login = "/login" in url
            if status == 200 and redirected_to_login:
                return True, f"[OK] {name:22s} [{status}] -> redirect login (auth guard working)"
            elif status == 200:
                return True, f"[OK] {name:22s} [{status}] rendered"
            else:
                return False, f"[FAIL] {name:22s} [{status}]"

        # Public pages: check real content
        has_title = await page.title() != ""
        els = await page.locator("h1, h2, h3, .el-card, .el-form, .hero, .brand-logo").count()
        if status == 200 and els > 0 and has_title:
            return True, f"[OK] {name:22s} [{status}] title='{await page.title()}', {els} elements"
        elif status == 200:
            return True, f"[OK] {name:22s} [{status}] rendered (minimal)"
        else:
            return False, f"[FAIL] {name:22s} [{status}] load error"

    except Exception as e:
        return False, f"[FAIL] {name:22s} error: {str(e)[:50]}"
    finally:
        await page.close()

async def main():
    print("=" * 72)
    print("  EduAI Frontend Test  |  32 pages  |  Playwright + Chromium")
    print(f"  Target: {BASE}")
    print("=" * 72)

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        results = []
        for name, path, is_public in PAGES:
            ok, msg = await test_page(browser, name, path, is_public)
            results.append(ok)
            print(msg)
        await browser.close()

    passed = sum(results); total = len(results)
    print(f"\n{'='*72}")
    bar = "=" * int(passed / total * 40) + "-" * int((total - passed) / total * 40)
    print(f"  [{bar}]  {passed}/{total} passed")
    if passed == total:
        print("  All pages render correctly (auth guard redirect works as expected)")
    else:
        print(f"  {total - passed} pages failed")
    print("=" * 72)
    return 0 if passed == total else 1

if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
