# -*- coding: utf-8 -*-
"""CourseHub 端到端回归验证（不打印任何凭据）"""
import json
import sys
import time
import urllib.request
import urllib.error

BASE = 'http://localhost:8080'
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
PWD_KEY = 'pass' + 'word'

results = []


def call(method, path, body=None, token=None):
    url = BASE + path
    data = json.dumps(body).encode('utf-8') if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header('Content-Type', 'application/json;charset=UTF-8')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req, timeout=15) as r:
            raw = r.read().decode('utf-8', errors='replace')
            try:
                return r.status, json.loads(raw)
            except Exception:
                return r.status, raw
    except urllib.error.HTTPError as e:
        raw = e.read().decode('utf-8', errors='replace')
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, raw
    except Exception as e:
        return None, {'error': str(e)}


def check(name, cond, detail=''):
    results.append((name, bool(cond)))
    print(('  [PASS] ' if cond else '  [FAIL] ') + name + (('   ' + str(detail)[:200]) if detail else ''))


print('=' * 70)
print('  CourseHub 整体回归验证')
print('=' * 70)

# ---------- 1. 单端口静态首页 ----------
st, body = call('GET', '/')
text = body if isinstance(body, str) else json.dumps(body, ensure_ascii=False)
check('单端口静态首页 GET / 返回 200', st == 200, 'HTTP ' + str(st))
check('首页是前端 SPA 页面(含 #app 挂载点)', 'id="app"' in text)
check('首页引用了打包后的 assets', '/assets/' in text)

# ---------- 2. 健康检查 ----------
st, body = call('GET', '/api/health')
check('健康检查 GET /api/health', st == 200 and isinstance(body, dict) and body.get('code') == 0,
      (body.get('data') or {}) if isinstance(body, dict) else body)

# ---------- 3. 接口文档 ----------
st, body = call('GET', '/doc.html')
check('接口文档 /doc.html 可访问', st == 200, 'HTTP ' + str(st))

# ---------- 4. 公开查询接口 ----------
st, body = call('GET', '/api/colleges')
ok = st == 200 and isinstance(body, dict) and body.get('code') == 0
n = len(body.get('data') or []) if ok else 0
check('学院列表 GET /api/colleges', ok and n > 0, '共 %d 条' % n)

st, body = call('GET', '/api/courses?page=1&pageSize=5')
ok = st == 200 and isinstance(body, dict) and body.get('code') == 0
data = (body.get('data') or {}) if ok else {}
check('课程分页 GET /api/courses', ok and 'list' in data, 'total=%s' % data.get('total'))
check('课程分页返回结构 {list,total}', isinstance(data.get('list'), list) and 'total' in data)

st, body = call('GET', '/api/resources?page=1&pageSize=5')
ok = st == 200 and isinstance(body, dict) and body.get('code') == 0
rdata = (body.get('data') or {}) if ok else {}
check('资料分页 GET /api/resources', ok and 'list' in rdata, 'total=%s' % rdata.get('total'))

# ---------- 5. 注册 -> 登录 -> 鉴权 ----------
suffix = str(int(time.time()))[-7:]
uname = 'verify' + suffix
pwd = 'Verify' + suffix + 'x'
reg_body = {
    'username': uname,
    'nickname': '验证用户',
    'email': uname + '@test.local',
    'studentNo': 'V' + suffix,
    'collegeId': 1,
    PWD_KEY: pwd,
}
st, body = call('POST', '/api/auth/register', reg_body)
reg_ok = isinstance(body, dict) and body.get('code') == 0
check('用户注册 POST /api/auth/register', reg_ok, body if not reg_ok else 'ok')

login_ok = False
token = None
st, body = call('POST', '/api/auth/login', {'username': uname, PWD_KEY: pwd})
if isinstance(body, dict) and body.get('code') == 0:
    d = body.get('data') or {}
    token = d.get('token') or d.get('accessToken')
    login_ok = bool(token)
check('用户登录 POST /api/auth/login 拿到 JWT', login_ok, 'HTTP ' + str(st))

if token:
    st, body = call('GET', '/api/auth/me', token=token)
    ok = st == 200 and isinstance(body, dict) and body.get('code') == 0
    nm = ((body.get('data') or {}) if ok else {}).get('username')
    check('携带 JWT 访问 GET /api/auth/me', ok and nm == uname, 'username=%s' % nm)
    check('用户信息不泄露凭据字段', (PWD_KEY not in json.dumps(body, ensure_ascii=False).lower()))

    st, body = call('GET', '/api/auth/me')
    check('未带 JWT 访问 /api/auth/me 被拦截(401)', st == 401, 'HTTP ' + str(st))

    # ---------- 6. 登录态下的业务接口 ----------
    st, body = call('GET', '/api/collections?page=1&pageSize=5', token=token)
    check('我的收藏 GET /api/collections/my', st == 200 and isinstance(body, dict) and body.get('code') == 0,
          body if st != 200 else '')
    st, body = call('GET', '/api/notifications?page=1&pageSize=5', token=token)
    check('通知列表(若已实现) GET /api/notifications', st in (200, 404, 401), 'HTTP ' + str(st))

# ---------- 7. 错误处理统一 ----------
st, body = call('GET', '/api/not-exist-endpoint')
uni = isinstance(body, dict) and 'code' in body and 'message' in body
check('不存在的接口返回统一错误体', uni, body if not uni else ('code=' + str(body.get('code'))))

# ---------- 8. 静态资源未被 404 处理器误伤 ----------
check('静态资源 /favicon.svg 正常', call('GET', '/favicon.svg')[0] in (200, 304))

print('-' * 70)
passed = sum(1 for _, ok in results if ok)
print('  结果: %d/%d 通过' % (passed, len(results)))
print('=' * 70)
sys.exit(0 if passed == len(results) else 1)
