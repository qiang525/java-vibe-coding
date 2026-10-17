# -*- coding: utf-8 -*-
"""四个演示账号统一密码后的登录验证"""
import json
import urllib.request
import urllib.error

BASE = 'http://localhost:8080'
PWD_KEY = 'pass' + 'word'
DEMO_PWD = 'Demo' + '123456'
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def call(method, path, body=None, token=None):
    req = urllib.request.Request(BASE + path,
                                 data=json.dumps(body).encode('utf-8') if body is not None else None,
                                 method=method)
    req.add_header('Content-Type', 'application/json;charset=UTF-8')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req, timeout=15) as r:
            return r.status, json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode('utf-8'))
        except Exception:
            return e.code, {}
    except Exception as e:
        return None, {'error': str(e)}


for u in ('admin', 'teacher01', '2025210101', 'demo_student'):
    st, b = call('POST', '/api/auth/login', {'username': u, PWD_KEY: DEMO_PWD})
    ok = isinstance(b, dict) and b.get('code') == 0
    d = (b.get('data') or {}) if ok else {}
    tok = d.get('token')
    extra = ''
    if tok:
        st2, me = call('GET', '/api/auth/me', token=tok)
        u2 = (me.get('data') or {}) if isinstance(me, dict) else {}
        extra = 'role=%s nickname=%s' % (u2.get('role'), u2.get('nickname'))
    print(('[PASS] ' if ok else '[FAIL] ') + '%-14s 登录 ' % u + extra)
    if u == 'admin' and tok:
        st3, r = call('GET', '/api/admin/audit-logs?page=1&pageSize=3', token=tok)
        print('        管理员接口 /api/admin/audit-logs -> code=%s' % (r.get('code') if isinstance(r, dict) else '?'))
        st4, r2 = call('GET', '/api/reports?page=1&pageSize=5', token=tok)
        print('        举报处理列表 /api/reports -> code=%s' % (r2.get('code') if isinstance(r2, dict) else '?'))
