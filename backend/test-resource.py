# -*- coding: utf-8 -*-
"""M2-2 资料模块接口测试

重点验证【三级可见性】—— 这是本模块最核心的数据权限能力：
  · PUBLIC      → 谁都能看（含未登录游客）
  · COURSE_ONLY → 仅课程成员 + 课程负责人
  · PRIVATE     → 仅上传者本人

以及：
  · 修改/删除权限（上传者 / 课程负责人 / 管理员）
  · 课程资料数冗余字段的维护
"""
import hashlib
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

BASE = 'http://localhost:8080'
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

results = []


def json_call(method, path, body=None, token=None):
    data = json.dumps(body).encode('utf-8') if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header('Content-Type', 'application/json;charset=UTF-8')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req, timeout=30) as r:
            return r.status, json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        raw = e.read().decode('utf-8', errors='replace')
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {'raw': raw}
    except Exception as e:
        return None, {'error': str(e)}


def upload(filename, content, token):
    boundary = '----CourseHubBoundary' + uuid.uuid4().hex
    body = b''
    body += ('--' + boundary + '\r\n').encode()
    body += ('Content-Disposition: form-data; name="file"; filename="%s"\r\n'
             % filename).encode('utf-8')
    body += b'Content-Type: application/octet-stream\r\n\r\n'
    body += content
    body += b'\r\n'
    body += ('--' + boundary + '--\r\n').encode()
    req = urllib.request.Request(BASE + '/api/files/upload', data=body, method='POST')
    req.add_header('Content-Type', 'multipart/form-data; boundary=' + boundary)
    req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req, timeout=60) as r:
            return json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode('utf-8', errors='replace'))
    except Exception as e:
        return {'error': str(e)}


def check(name, cond, detail=''):
    results.append((name, cond, detail))
    print(('  [PASS] ' if cond else '  [FAIL] ') + name + ('  ' + detail if detail else ''))


def pdf_bytes(seed):
    """生成内容不同的 PDF（seed 不同 → MD5 不同 → 不会误触发秒传）"""
    return ('%%PDF-1.4\n%s\n%%%%EOF\n' % seed).encode('utf-8')


print('=' * 66)
print('  M2-2 资料模块接口测试（三级可见性）')
print('=' * 66)

print('\n[0] 等待后端就绪...')
ready = False
for i in range(40):
    code, body = json_call('GET', '/api/health')
    if code == 200 and body.get('code') == 0:
        print('  后端就绪（%d 秒）' % (i * 2))
        ready = True
        break
    time.sleep(2)
if not ready:
    print('  后端未就绪，测试中止')
    sys.exit(1)

# ---------- 1. 登录管理员 ----------
print('\n[1] 登录管理员')
code, body = json_call('POST', '/api/auth/login', {'username': 'admin', 'password': 'admin123'})
token_admin = (body.get('data') or {}).get('token')
check('管理员登录成功', bool(token_admin))

# ---------- 2. 管理员创建一门课程 ----------
print('\n[2] 管理员创建测试课程')
stamp = str(int(time.time()))
code, body = json_call('POST', '/api/courses',
                       {'name': '资料测试课程-' + stamp, 'teacher': '测试老师'},
                       token=token_admin)
course_id = body.get('data')
check('课程创建成功', body.get('code') == 0, 'courseId=%s' % course_id)

# ---------- 3. 上传文件 ----------
print('\n[3] 上传文件')
up = upload('线性表讲义.pdf', pdf_bytes('A'), token_admin)
file_a = (up.get('data') or {}).get('id')
check('文件上传成功', up.get('code') == 0, 'fileId=%s' % file_a)

# ---------- 4. 发布 PUBLIC 资料 ----------
print('\n[4] 发布 PUBLIC 资料')
code, body = json_call('POST', '/api/resources', {
    'courseId': course_id, 'fileId': file_a,
    'title': '第二章 线性表', 'description': '含顺序表与链表',
    'type': 'LECTURE', 'visibility': 'PUBLIC'
}, token=token_admin)
rid_public = body.get('data')
print('  响应:', json.dumps(body, ensure_ascii=False)[:160])
check('发布成功', body.get('code') == 0, 'resourceId=%s' % rid_public)

# ---------- 5. 未登录也能看 PUBLIC ----------
print('\n[5] 未登录查看 PUBLIC 资料详情（应可见）')
code, body = json_call('GET', '/api/resources/%s' % rid_public)
detail = body.get('data') or {}
check('业务码为 0（PUBLIC 对游客可见）', body.get('code') == 0)
check('带出了课程名（JOIN 生效）', bool(detail.get('courseName')), str(detail.get('courseName')))
check('带出了文件名（JOIN 生效）', bool(detail.get('fileName')), str(detail.get('fileName')))
check('带出了上传者昵称', bool(detail.get('uploaderName')))
check('游客的 canEdit 为 false', detail.get('canEdit') is False)

# ---------- 6. 注册学生并登录 ----------
print('\n[6] 注册普通学生并登录')
stu_name = 'stu2_' + stamp
code, body = json_call('POST', '/api/auth/register',
                       {'username': stu_name, 'password': '123456', 'nickname': '测试学生乙'})
check('学生注册成功', body.get('code') == 0)
code, body = json_call('POST', '/api/auth/login', {'username': stu_name, 'password': '123456'})
token_stu = (body.get('data') or {}).get('token')
check('学生登录成功', bool(token_stu))

# ---------- 7. 发布 PRIVATE 资料 ----------
print('\n[7] 管理员发布 PRIVATE 资料')
up = upload('私密笔记.pdf', pdf_bytes('B'), token_admin)
file_b = (up.get('data') or {}).get('id')
code, body = json_call('POST', '/api/resources', {
    'courseId': course_id, 'fileId': file_b,
    'title': '我的私密笔记', 'visibility': 'PRIVATE'
}, token=token_admin)
rid_private = body.get('data')
check('发布成功', body.get('code') == 0, 'resourceId=%s' % rid_private)

# ---------- 8. 【核心】学生访问 PRIVATE 资料 ----------
print('\n[8] 【核心】学生访问他人的 PRIVATE 资料（应拒绝）')
code, body = json_call('GET', '/api/resources/%s' % rid_private, token=token_stu)
print('  响应:', json.dumps(body, ensure_ascii=False)[:150])
check('业务码为 40303（无权访问该资源）', body.get('code') == 40303)

# ---------- 9. 【核心】列表里看不到 PRIVATE ----------
print('\n[9] 【核心】学生查资料列表（不应出现别人的 PRIVATE 资料）')
code, body = json_call('GET', '/api/resources?courseId=%s&pageSize=50' % course_id, token=token_stu)
items = ((body.get('data') or {}).get('list') or [])
ids = [x.get('id') for x in items]
check('列表里没有 PRIVATE 资料', rid_private not in ids, '看到 %d 条' % len(items))
check('列表里有 PUBLIC 资料', rid_public in ids)

# ---------- 10. 发布 COURSE_ONLY 资料 ----------
print('\n[10] 管理员发布 COURSE_ONLY 资料')
up = upload('期末真题.pdf', pdf_bytes('C'), token_admin)
file_c = (up.get('data') or {}).get('id')
code, body = json_call('POST', '/api/resources', {
    'courseId': course_id, 'fileId': file_c,
    'title': '2025 期末真题', 'type': 'EXAM', 'visibility': 'COURSE_ONLY'
}, token=token_admin)
rid_course = body.get('data')
check('发布成功', body.get('code') == 0, 'resourceId=%s' % rid_course)

# ---------- 11. 【核心】非课程成员访问 COURSE_ONLY ----------
print('\n[11] 【核心】非课程成员访问 COURSE_ONLY 资料（应拒绝）')
code, body = json_call('GET', '/api/resources/%s' % rid_course, token=token_stu)
print('  响应:', json.dumps(body, ensure_ascii=False)[:150])
check('业务码为 40304（不是课程成员）', body.get('code') == 40304)

# ---------- 12. 加入课程 ----------
print('\n[12] 学生加入该课程')
code, body = json_call('POST', '/api/courses/%s/join' % course_id, token=token_stu)
check('加入成功', body.get('code') == 0)

# ---------- 13. 加入后可访问 ----------
print('\n[13] 加入课程后再访问 COURSE_ONLY 资料（应可见）')
code, body = json_call('GET', '/api/resources/%s' % rid_course, token=token_stu)
check('业务码为 0（已成为课程成员）', body.get('code') == 0)

# ---------- 14. 【核心】越权修改别人的资料 ----------
print('\n[14] 【核心】学生修改管理员的资料（应拒绝）')
code, body = json_call('PUT', '/api/resources/%s' % rid_public,
                       {'courseId': course_id, 'fileId': file_a, 'title': '我篡改的标题',
                        'visibility': 'PUBLIC'}, token=token_stu)
print('  响应:', json.dumps(body, ensure_ascii=False)[:150])
check('业务码为 40301（无权执行该操作）', body.get('code') == 40301)

code, body = json_call('GET', '/api/resources/%s' % rid_public)
check('标题【没有被改动】', (body.get('data') or {}).get('title') == '第二章 线性表')

# ---------- 15. 【核心】越权删除 ----------
print('\n[15] 【核心】学生删除管理员的资料（应拒绝）')
code, body = json_call('DELETE', '/api/resources/%s' % rid_public, token=token_stu)
check('业务码为 40301', body.get('code') == 40301)

# ---------- 16. 未登录发布 ----------
print('\n[16] 未登录发布资料（应返回 40101）')
code, body = json_call('POST', '/api/resources',
                       {'courseId': course_id, 'fileId': file_a, 'title': 'x'})
check('业务码为 40101', body.get('code') == 40101)

# ---------- 17. 参数校验 ----------
print('\n[17] 发布资料但标题为空（应返回 40001）')
code, body = json_call('POST', '/api/resources',
                       {'courseId': course_id, 'fileId': file_a, 'title': ''},
                       token=token_admin)
print('  响应:', json.dumps(body, ensure_ascii=False)[:150])
check('业务码为 40001（参数校验失败）', body.get('code') == 40001)

# ---------- 18. 课程资料数冗余字段 ----------
print('\n[18] 校验课程资料数（冗余字段是否被正确维护）')
code, body = json_call('GET', '/api/courses/%s' % course_id)
cnt = (body.get('data') or {}).get('resourceCount')
check('课程资料数为 3（发布了 3 份资料）', cnt == 3, 'resourceCount=%s' % cnt)

# ---------- 19. 修改自己的资料 ----------
print('\n[19] 管理员修改自己的资料（应成功）')
code, body = json_call('PUT', '/api/resources/%s' % rid_public, {
    'courseId': course_id, 'fileId': file_a,
    'title': '第二章 线性表（已修订）', 'visibility': 'PUBLIC'
}, token=token_admin)
check('修改成功', body.get('code') == 0)
code, body = json_call('GET', '/api/resources/%s' % rid_public)
check('标题已更新', (body.get('data') or {}).get('title') == '第二章 线性表（已修订）')

# ---------- 20. 删除资料并校验计数 ----------
print('\n[20] 管理员删除资料')
code, body = json_call('DELETE', '/api/resources/%s' % rid_public, token=token_admin)
check('删除成功', body.get('code') == 0)

code, body = json_call('GET', '/api/resources/%s' % rid_public)
check('删除后详情为 40404（资料不存在）', body.get('code') == 40404)

code, body = json_call('GET', '/api/courses/%s' % course_id)
check('课程资料数降为 2', (body.get('data') or {}).get('resourceCount') == 2,
      'resourceCount=%s' % (body.get('data') or {}).get('resourceCount'))

passed = sum(1 for _, c, _ in results if c)
total = len(results)
print('\n' + '=' * 66)
print('  测试结果: %d / %d 通过' % (passed, total))
print('=' * 66)
for name, c, _ in results:
    if not c:
        print('  失败: ' + name)
sys.exit(0 if passed == total else 1)
