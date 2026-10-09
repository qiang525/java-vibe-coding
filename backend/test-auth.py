# -*- coding: utf-8 -*-
"""M1 认证模块接口测试"""
import json
import sys
import time
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
print('  M1 认证模块接口测试')
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

# ---------- 1. 正确账号登录 ----------
print('\n[1] 登录：正确账号密码')
code, body = call('POST', '/api/auth/login',
                  {'username': 'admin', 'password': 'admin123'})
print('  响应:', json.dumps(body, ensure_ascii=False)[:180])
check('HTTP 200', code == 200)
check('业务码为 0', body.get('code') == 0)
data = body.get('data') or {}
token = data.get('token')
check('返回了 Token', bool(token), (token[:30] + '...') if token else '')
check('返回了用户信息', bool(data.get('user')))
check('用户信息中【不含】密码字段', 'password' not in json.dumps(data.get('user') or {}))
check('角色为 ADMIN', (data.get('user') or {}).get('role') == 'ADMIN')

# ---------- 2. 密码错误 ----------
print('\n[2] 登录：密码错误（应被拒绝）')
code, body = call('POST', '/api/auth/login',
                  {'username': 'admin', 'password': 'wrongpassword'})
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('业务码为 40104（账号或密码错误）', body.get('code') == 40104)

# ---------- 3. 不存在的账号 ----------
print('\n[3] 登录：账号不存在（提示应与密码错误一致，防枚举）')
code, body = call('POST', '/api/auth/login',
                  {'username': 'nobody9999', 'password': 'whatever'})
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('业务码同样为 40104', body.get('code') == 40104)

# ---------- 4. 参数校验 ----------
print('\n[4] 登录：参数校验（密码过短）')
code, body = call('POST', '/api/auth/login',
                  {'username': 'admin', 'password': '123'})
print('  响应:', json.dumps(body, ensure_ascii=False)[:160])
check('业务码为 40001（参数校验失败）', body.get('code') == 40001)

# ---------- 5. 未带 Token 访问受保护接口 ----------
print('\n[5] 鉴权：未带 Token 访问 /api/auth/me（应返回 401）')
code, body = call('GET', '/api/auth/me')
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('业务码为 40101（未登录）', body.get('code') == 40101)

# ---------- 6. 带 Token 访问 ----------
print('\n[6] 鉴权：带 Token 访问 /api/auth/me（应成功）')
code, body = call('GET', '/api/auth/me', token=token)
print('  响应:', json.dumps(body, ensure_ascii=False)[:200])
check('业务码为 0', body.get('code') == 0)
check('返回的是 admin 本人信息',
      (body.get('data') or {}).get('username') == 'admin')

# ---------- 7. 伪造 Token ----------
print('\n[7] 鉴权：伪造 Token（应被拒绝）')
code, body = call('GET', '/api/auth/me', token='fake.token.here')
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('业务码为 40101（Token 无效）', body.get('code') == 40101)

# ---------- 8. 退出登录 ----------
print('\n[8] 退出登录')
code, body = call('POST', '/api/auth/logout', token=token)
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('业务码为 0', body.get('code') == 0)

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
