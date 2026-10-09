# -*- coding: utf-8 -*-
"""M1 课程模块接口测试

重点验证：
  1. 课程的增删改查链路
  2. 分页与筛选
  3. 【核心】数据权限 —— 普通学生不能修改/删除别人的课程
  4. 【核心】防重复加入
"""
import json
import sys
import time
import urllib.parse
import urllib.request
import urllib.error

BASE = 'http://localhost:8080'
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

results = []


def call(method, path, body=None, token=None):
    url = BASE + path
    data = json.dumps(body).encode('utf-8') if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header('Content-Type', 'application/json;charset=UTF-8')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req, timeout=8) as r:
            return r.status, json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        raw = e.read().decode('utf-8', errors='replace')
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {'raw': raw}
    except Exception as e:
        return None, {'error': str(e)}


def check(name, cond, detail=''):
    results.append((name, cond, detail))
    print(('  [PASS] ' if cond else '  [FAIL] ') + name + ('  ' + detail if detail else ''))


print('=' * 66)
print('  M1 课程模块接口测试')
print('=' * 66)

# ---------- 等待服务就绪 ----------
print('\n[0] 等待后端就绪...')
ready = False
for i in range(40):
    code, body = call('GET', '/api/health')
    if code == 200 and body.get('code') == 0:
        print('  后端就绪（%d 秒）' % (i * 2))
        ready = True
        break
    time.sleep(2)

if not ready:
    print('  后端未就绪，测试中止')
    sys.exit(1)

# ---------- 1. 学院列表 ----------
print('\n[1] 学院列表（无需登录）')
code, body = call('GET', '/api/colleges')
print('  响应:', json.dumps(body, ensure_ascii=False)[:160])
check('HTTP 200', code == 200)
check('业务码为 0', body.get('code') == 0)
colleges = body.get('data') or []
check('返回了学院列表', len(colleges) > 0, '共 %d 个学院' % len(colleges))
college_id = colleges[0]['id'] if colleges else None

# ---------- 2. 课程列表（未登录也能看） ----------
print('\n[2] 课程列表（未登录，验证浏览无需登录）')
code, body = call('GET', '/api/courses?page=1&pageSize=5')
print('  响应:', json.dumps(body, ensure_ascii=False)[:160])
check('业务码为 0', body.get('code') == 0)
page_data = body.get('data') or {}
check('返回分页结构', 'list' in page_data and 'total' in page_data)
check('pageSize 生效为 9 或 5', page_data.get('pageSize') in (5, 9))

# ---------- 3. 登录管理员 ----------
print('\n[3] 登录管理员')
code, body = call('POST', '/api/auth/login', {'username': 'admin', 'password': 'admin123'})
token_admin = (body.get('data') or {}).get('token')
check('管理员登录成功', body.get('code') == 0 and bool(token_admin))

# ---------- 4. 未登录创建课程（应拒绝） ----------
print('\n[4] 未登录创建课程（应返回 40101）')
code, body = call('POST', '/api/courses', {'name': '测试课程-未登录'})
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('业务码为 40101（未登录）', body.get('code') == 40101)

# ---------- 5. 管理员创建课程 ----------
print('\n[5] 管理员创建课程')
course_name = '测试课程-' + str(int(time.time()))
code, body = call('POST', '/api/courses', {
    'name': course_name,
    'courseCode': 'TEST-001',
    'collegeId': college_id,
    'teacher': '测试老师',
    'term': '2026-2027-1',
    'credit': 3.0,
    'description': '用于自动化测试的课程'
}, token=token_admin)
print('  响应:', json.dumps(body, ensure_ascii=False)[:160])
check('创建成功', body.get('code') == 0)
course_id = body.get('data')
check('返回了新课程 ID', bool(course_id), 'courseId=%s' % course_id)

# ---------- 6. 课程详情 ----------
print('\n[6] 课程详情')
code, body = call('GET', '/api/courses/%s' % course_id)
detail = body.get('data') or {}
print('  响应:', json.dumps(body, ensure_ascii=False)[:200])
check('业务码为 0', body.get('code') == 0)
check('课程名正确', detail.get('name') == course_name)
check('学院名已被 JOIN 带出', bool(detail.get('collegeName')), detail.get('collegeName'))
check('创建者自己算 1 个成员', detail.get('memberCount') == 1)

# ---------- 7. 关键词搜索 ----------
print('\n[7] 关键词搜索（中文需 URL 编码）')
code, body = call('GET', '/api/courses?keyword=' + urllib.parse.quote('测试课程'))
found = [c for c in ((body.get('data') or {}).get('list') or []) if c.get('id') == course_id]
check('搜到了刚创建的课程', len(found) > 0)

# ---------- 8. 修改课程（管理员=负责人，应成功） ----------
print('\n[8] 修改课程（负责人本人）')
# PUT 是全量更新语义，必填字段 name 必须一起传
code, body = call('PUT', '/api/courses/%s' % course_id,
                  {'name': course_name, 'teacher': '改后的老师', 'credit': 4.0},
                  token=token_admin)
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('修改成功', body.get('code') == 0)
code, body = call('GET', '/api/courses/%s' % course_id)
check('字段已更新', (body.get('data') or {}).get('teacher') == '改后的老师')

# ---------- 9. 注册一个普通学生 ----------
print('\n[9] 注册普通学生账号')
stu_name = 'stu_' + str(int(time.time()))
code, body = call('POST', '/api/auth/register', {
    'username': stu_name, 'password': '123456', 'nickname': '测试学生'
})
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('注册成功', body.get('code') == 0)

code, body = call('POST', '/api/auth/login', {'username': stu_name, 'password': '123456'})
token_stu = (body.get('data') or {}).get('token')
check('学生登录成功', bool(token_stu))
check('学生角色为 STUDENT', ((body.get('data') or {}).get('user') or {}).get('role') == 'STUDENT')

# ---------- 10. 【核心】越权修改别人的课程 ----------
print('\n[10] 【核心】学生越权修改管理员的课程（应返回 40301）')
code, body = call('PUT', '/api/courses/%s' % course_id,
                  {'name': '我篡改的课程名'}, token=token_stu)
print('  响应:', json.dumps(body, ensure_ascii=False)[:160])
check('业务码为 40301（无权执行该操作）', body.get('code') == 40301)

code, body = call('GET', '/api/courses/%s' % course_id)
check('课程名【没有被改动】（越权被成功拦截）',
      (body.get('data') or {}).get('name') == course_name)

# ---------- 11. 【核心】越权删除别人的课程 ----------
print('\n[11] 【核心】学生越权删除管理员的课程（应返回 40301）')
code, body = call('DELETE', '/api/courses/%s' % course_id, token=token_stu)
print('  响应:', json.dumps(body, ensure_ascii=False)[:160])
check('业务码为 40301', body.get('code') == 40301)

code, body = call('GET', '/api/courses/%s' % course_id)
check('课程仍然存在（未被删除）', body.get('code') == 0)

# ---------- 12. 学生加入课程 ----------
print('\n[12] 学生加入课程')
code, body = call('POST', '/api/courses/%s/join' % course_id, token=token_stu)
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('加入成功', body.get('code') == 0)

code, body = call('GET', '/api/courses/%s' % course_id)
check('成员数变为 2', (body.get('data') or {}).get('memberCount') == 2)

# ---------- 13. 【核心】重复加入 ----------
print('\n[13] 【核心】重复加入（应返回 40906）')
code, body = call('POST', '/api/courses/%s/join' % course_id, token=token_stu)
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('业务码为 40906（已加入该课程）', body.get('code') == 40906)

# ---------- 14. 我加入的课程 ----------
print('\n[14] 查询「我加入的课程」')
code, body = call('GET', '/api/courses/mine', token=token_stu)
mine = ((body.get('data') or {}).get('list') or [])
check('查到了刚加入的课程', any(c.get('id') == course_id for c in mine))

# ---------- 15. 学生退出课程 ----------
print('\n[15] 学生退出课程')
code, body = call('DELETE', '/api/courses/%s/join' % course_id, token=token_stu)
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('退出成功', body.get('code') == 0)

code, body = call('GET', '/api/courses/%s' % course_id)
check('成员数回到 1', (body.get('data') or {}).get('memberCount') == 1)

# ---------- 16. 负责人不能退出自己的课程 ----------
print('\n[16] 负责人退出自己的课程（应返回 40904）')
code, body = call('DELETE', '/api/courses/%s/join' % course_id, token=token_admin)
print('  响应:', json.dumps(body, ensure_ascii=False)[:160])
check('业务码为 40904（当前状态不允许该操作）', body.get('code') == 40904)

# ---------- 17. 删除课程 ----------
print('\n[17] 负责人删除课程')
code, body = call('DELETE', '/api/courses/%s' % course_id, token=token_admin)
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('删除成功', body.get('code') == 0)

# ---------- 18. 删除后查询详情 ----------
print('\n[18] 删除后查询详情（应返回 40403）')
code, body = call('GET', '/api/courses/%s' % course_id)
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('业务码为 40403（课程不存在）', body.get('code') == 40403)

# ---------- 19. 课程不存在时的操作 ----------
print('\n[19] 对不存在的课程加入（应返回 40403）')
code, body = call('POST', '/api/courses/999999/join', token=token_stu)
check('业务码为 40403', body.get('code') == 40403)

# ---------- 汇总 ----------
passed = sum(1 for _, c, _ in results if c)
total = len(results)
print('\n' + '=' * 66)
print('  测试结果: %d / %d 通过' % (passed, total))
print('=' * 66)
for name, c, _ in results:
    if not c:
        print('  失败: ' + name)

sys.exit(0 if passed == total else 1)
