# -*- coding: utf-8 -*-
"""M2-4 中文全文检索测试"""
import hashlib, json, sys, time, urllib.error, urllib.parse, urllib.request, uuid

BASE = 'http://localhost:8080'
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
results = []

def json_call(method, path, body=None, token=None, timeout=30):
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

def upload(filename, content, token):
    boundary = '----CH' + uuid.uuid4().hex
    body = b''
    body += ('--' + boundary + '\r\n').encode()
    body += ('Content-Disposition: form-data; name="file"; filename="%s"\r\n' % filename).encode('utf-8')
    body += b'Content-Type: application/octet-stream\r\n\r\n' + content + b'\r\n'
    body += ('--' + boundary + '--\r\n').encode()
    req = urllib.request.Request(BASE + '/api/files/upload', data=body, method='POST')
    req.add_header('Content-Type', 'multipart/form-data; boundary=' + boundary)
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
print('  M2-4 中文全文检索测试')
print('=' * 66)

print('\n[0] 等待后端就绪...')
for i in range(40):
    code, body = json_call('GET', '/api/health')
    if code == 200 and body.get('code') == 0:
        print('  后端就绪（%d 秒）' % (i*2)); break
    time.sleep(2)
else:
    print('  后端未就绪'); sys.exit(1)

print('\n[1] 登录管理员')
code, body = json_call('POST', '/api/auth/login', {'username':'admin','password':'admin123'})
token = (body.get('data') or {}).get('token')
check('登录成功', bool(token))

stamp = str(int(time.time()))
print('\n[2] 准备测试数据：创建课程 + 发布 3 份资料')
code, body = json_call('POST', '/api/courses', {'name': '检索测试课程-' + stamp}, token=token)
cid = body.get('data')
check('课程创建成功', body.get('code') == 0, 'courseId=%s' % cid)

titles = [
    ('数据结构与算法第二章 线性表', 'LECTURE', 'PUBLIC'),
    ('数据结构期末真题 2025', 'EXAM', 'PUBLIC'),
    ('计算机网络第一章 概述', 'LECTURE', 'PUBLIC'),
]
rids = []
for i, (t, typ, vis) in enumerate(titles):
    up = upload('doc%d.pdf' % i, pdf('%s-%d' % (stamp, i)), token)
    fid = (up.get('data') or {}).get('id')
    code, body = json_call('POST', '/api/resources', {
        'courseId': cid, 'fileId': fid, 'title': t, 'type': typ, 'visibility': vis
    }, token=token)
    rids.append(body.get('data'))
check('3 份资料发布成功', all(rids), str(rids))

print('\n[3] 【核心】中文全文检索：搜「数据结构」')
code, body = json_call('GET', '/api/resources/search?keyword=' + urllib.parse.quote('数据结构') + '&pageSize=20')
d = body.get('data') or {}
items = d.get('list') or []
print('  命中 %s 条' % d.get('total'))
for it in items:
    print('    · %s (related=%.3f)' % (it.get('title'), it.get('relevance') or 0))
check('业务码为 0', body.get('code') == 0)
check('搜到了 2 条（2 份资料标题含"数据结构"）', d.get('total') == 2, str(d.get('total')))
check('返回了相关度分数', all(it.get('relevance') is not None for it in items))

print('\n[4] 搜「线性表」（词在标题中间）')
code, body = json_call('GET', '/api/resources/search?keyword=' + urllib.parse.quote('线性表'))
d = body.get('data') or {}
print('  命中 %s 条' % d.get('total'))
check('命中 1 条', d.get('total') == 1, str(d.get('total')))

print('\n[5] 搜「网络」（跨字段匹配）')
code, body = json_call('GET', '/api/resources/search?keyword=' + urllib.parse.quote('网络'))
d = body.get('data') or {}
check('命中 1 条（计算机网络）', d.get('total') == 1, str(d.get('total')))

print('\n[6] 搜不存在的词')
code, body = json_call('GET', '/api/resources/search?keyword=' + urllib.parse.quote('区块链量子计算'))
d = body.get('data') or {}
check('命中 0 条', d.get('total') == 0)

print('\n[7] 【核心】搜索热词（Redis ZSet）')
code, body = json_call('GET', '/api/resources/search/hot?limit=10')
hot = body.get('data') or []
print('  热词:', json.dumps(hot, ensure_ascii=False)[:200])
check('业务码为 0', body.get('code') == 0)
check('返回了热词列表', len(hot) > 0, '%d 个' % len(hot))
kws = [h.get('keyword') for h in hot]
check('包含刚搜过的「数据结构」', '数据结构' in kws, str(kws[:5]))
check('热词带搜索次数', all(h.get('count') for h in hot))

print('\n[8] 【核心】搜索也遵守三级可见性')
# 用 admin 发一份 PRIVATE 资料，标题含特殊词
up = upload('secret.pdf', pdf('secret-' + stamp), token)
fid = (up.get('data') or {}).get('id')
code, body = json_call('POST', '/api/resources', {
    'courseId': cid, 'fileId': fid, 'title': '秘密笔记XYZ', 'visibility': 'PRIVATE'
}, token=token)
rid_secret = body.get('data')
check('PRIVATE 资料发布成功', body.get('code') == 0, 'rid=%s' % rid_secret)

# 管理员能搜到
code, body = json_call('GET', '/api/resources/search?keyword=XYZ', token=token)
d = body.get('data') or {}
check('管理员能搜到 PRIVATE 资料', d.get('total') == 1, str(d.get('total')))

# 学生搜不到
stu = 'searcher_' + stamp
json_call('POST', '/api/auth/register', {'username': stu, 'password': '123456', 'nickname': '搜索学生'})
code, body = json_call('POST', '/api/auth/login', {'username': stu, 'password': '123456'})
token_stu = (body.get('data') or {}).get('token')
code, body = json_call('GET', '/api/resources/search?keyword=XYZ', token=token_stu)
d = body.get('data') or {}
check('学生【搜不到】别人的 PRIVATE 资料', d.get('total') == 0, str(d.get('total')))

# 游客也搜不到
code, body = json_call('GET', '/api/resources/search?keyword=XYZ')
d = body.get('data') or {}
check('游客也搜不到 PRIVATE 资料', d.get('total') == 0, str(d.get('total')))

print('\n[9] 空关键词（应退化为普通列表，不报错）')
code, body = json_call('GET', '/api/resources/search?keyword=&pageSize=5')
check('业务码为 0（未报错）', body.get('code') == 0)

print('\n[10] 清理')
for rid in rids + [rid_secret]:
    if rid: json_call('DELETE', '/api/resources/%s' % rid, token=token)
if cid: json_call('DELETE', '/api/courses/%s' % cid, token=token)
check('清理完成', True)

passed = sum(1 for _, c, _ in results if c)
total = len(results)
print('\n' + '=' * 66)
print('  测试结果: %d / %d 通过' % (passed, total))
print('=' * 66)
for n, c, _ in results:
    if not c: print('  失败: ' + n)
sys.exit(0 if passed == total else 1)
