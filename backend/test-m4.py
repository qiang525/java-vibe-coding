# -*- coding: utf-8 -*-
"""M4 测试：可信度评分 + 操作审计日志"""
import json, sys, time, urllib.error, urllib.request, uuid

BASE = 'http://localhost:8080'
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
results = []

def call(method, path, body=None, token=None, timeout=30):
    data = json.dumps(body).encode('utf-8') if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header('Content-Type', 'application/json;charset=UTF-8')
    if token: req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        raw = e.read().decode('utf-8', errors='replace')
        try: return e.code, json.loads(raw)
        except Exception: return e.code, {'raw': raw}
    except Exception as e:
        return None, {'error': str(e)}

def upload(name, content, token):
    b = '----CH' + uuid.uuid4().hex
    body = b''
    body += ('--' + b + '\r\n').encode()
    body += ('Content-Disposition: form-data; name="file"; filename="%s"\r\n' % name).encode('utf-8')
    body += b'Content-Type: application/octet-stream\r\n\r\n' + content + b'\r\n'
    body += ('--' + b + '--\r\n').encode()
    req = urllib.request.Request(BASE + '/api/files/upload', data=body, method='POST')
    req.add_header('Content-Type', 'multipart/form-data; boundary=' + b)
    req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req, timeout=60) as r:
            return json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode('utf-8', errors='replace'))

def check(name, cond, detail=''):
    results.append((name, cond, detail))
    print(('  [PASS] ' if cond else '  [FAIL] ') + name + ('  ' + detail if detail else ''))

def pdf(seed):
    return ('%%PDF-1.4\n%s\n%%%%EOF\n' % seed).encode('utf-8')

print('=' * 66)
print('  M4 测试：可信度评分 + 操作审计日志')
print('=' * 66)

print('\n[0] 等待后端就绪...')
for i in range(40):
    c, b = call('GET', '/api/health')
    if c == 200 and b.get('code') == 0:
        print('  就绪（%d 秒）' % (i*2)); break
    time.sleep(2)
else:
    sys.exit(1)

print('\n[1] 登录 + 准备数据')
c, b = call('POST', '/api/auth/login', {'username':'admin','password':'admin123'})
tk = (b.get('data') or {}).get('token')
check('admin 登录', bool(tk))

stamp = str(int(time.time()))
c, b = call('POST', '/api/courses', {'name': 'M4评分测试-' + stamp}, token=tk)
cid = b.get('data')

# 发一份"完整度高"的资料（有描述、长标题）
b1 = upload('good.pdf', pdf('good-' + stamp), tk)
f1 = (b1.get('data') or {}).get('id')
c, b = call('POST', '/api/resources', {
    'courseId': cid, 'fileId': f1,
    'title': '数据结构与算法 第二章 线性表完整讲义',
    'description': '本讲义涵盖了顺序表、单链表、双向链表、循环链表的核心概念与代码实现，并配有课堂例题',
    'type': 'LECTURE', 'visibility': 'PUBLIC'
}, token=tk)
rid_good = b.get('data')

# 发一份"完整度低"的资料（无描述、短标题）
b2 = upload('bad.pdf', pdf('bad-' + stamp), tk)
f2 = (b2.get('data') or {}).get('id')
c, b = call('POST', '/api/resources', {
    'courseId': cid, 'fileId': f2, 'title': 'abc', 'visibility': 'PUBLIC'
}, token=tk)
rid_bad = b.get('data')
check('两份测试资料发布成功', bool(rid_good) and bool(rid_bad),
      'good=%s bad=%s' % (rid_good, rid_bad))

print('\n[2] 【评分】触发全量重算')
c, b = call('POST', '/api/admin/recalc-score', token=tk)
print('  响应:', json.dumps(b, ensure_ascii=False)[:140])
check('重算成功', b.get('code') == 0)
check('返回处理条数', isinstance(b.get('data'), int) and b.get('data') >= 2,
      str(b.get('data')))

print('\n[3] 【评分】验证两笔资料的评分（完整度不同 → 分数应不同）')
c, b = call('GET', '/api/resources/%s' % rid_good)
good = b.get('data') or {}
c, b = call('GET', '/api/resources/%s' % rid_bad)
bad = b.get('data') or {}
sg = float(good.get('score') or 0)
sb = float(bad.get('score') or 0)
print('  完整资料评分: %.2f' % sg)
print('  简陋资料评分: %.2f' % sb)
check('两份评分都已写入（>0）', sg > 0 and sb > 0)
check('完整度高的资料评分更高', sg > sb, '%.2f > %.2f' % (sg, sb))
check('评分在 0-100 范围内', 0 <= sg <= 100 and 0 <= sb <= 100)

print('\n[4] 【评分】资料列表按评分排序（score 字段可读）')
c, b = call('GET', '/api/resources?pageSize=5')
lst = ((b.get('data') or {}).get('list') or [])
check('列表里带出了 score 字段', all('score' in x for x in lst))

print('\n[5] 【审计日志】触发重算后应产生一条日志')
c, b = call('GET', '/api/admin/audit-logs?pageSize=20', token=tk)
d = b.get('data') or {}
logs = d.get('list') or []
print('  日志条数:', d.get('total'))
check('有审计日志', d.get('total') and d.get('total') >= 1, str(d.get('total')))
hit = next((x for x in logs if '重算' in (x.get('operation') or '')), None)
check('找到了"重算可信度评分"这条', hit is not None)
if hit:
    check('记录了操作人账号', bool(hit.get('username')), str(hit.get('username')))
    check('记录了操作对象类型', hit.get('targetType') == 'SYSTEM', str(hit.get('targetType')))
    check('记录了来源 IP', bool(hit.get('ip')), str(hit.get('ip')))
    check('记录了操作详情', bool(hit.get('detail')), str(hit.get('detail'))[:60])

print('\n[6] 【审计日志】权限：非管理员不能看')
stu = 'm4stu_' + stamp
call('POST', '/api/auth/register', {'username': stu, 'password':'123456','nickname':'普通学生'})
c, b = call('POST', '/api/auth/login', {'username': stu, 'password':'123456'})
tk_stu = (b.get('data') or {}).get('token')
c, b = call('GET', '/api/admin/audit-logs', token=tk_stu)
print('  响应:', json.dumps(b, ensure_ascii=False)[:120])
check('学生被拦（40302 权限不足）', b.get('code') == 40302)

print('\n[7] 【审计日志】未登录访问管理接口')
c, b = call('GET', '/api/admin/audit-logs')
check('未登录返回 40101', b.get('code') == 40101)

print('\n[8] 【审计日志】按用户筛选')
c, b = call('GET', '/api/admin/audit-logs?userId=1', token=tk)
check('按 userId 筛选可用', b.get('code') == 0)

print('\n[9] 清理')
for x in [rid_good, rid_bad]:
    if x: call('DELETE', '/api/resources/%s' % x, token=tk)
if cid: call('DELETE', '/api/courses/%s' % cid, token=tk)
check('清理完成', True)

passed = sum(1 for _, c, _ in results if c)
total = len(results)
print('\n' + '=' * 66)
print('  测试结果: %d / %d 通过' % (passed, total))
print('=' * 66)
for n, c, _ in results:
    if not c: print('  失败: ' + n)
sys.exit(0 if passed == total else 1)
