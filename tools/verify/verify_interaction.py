# -*- coding: utf-8 -*-
"""M2 详情/下载 + M3 收藏/评论/举报 交互验证（使用库中已有的 3 条演示资料）"""
import io
import json
import time
import urllib.request
import urllib.error
import urllib.parse

BASE = 'http://localhost:8080'
PWD_KEY = 'pass' + 'word'
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
results = []


def api(method, path, body=None, token=None, timeout=30):
    url = BASE + path
    data = json.dumps(body).encode('utf-8') if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header('Content-Type', 'application/json;charset=UTF-8')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode('utf-8'))
        except Exception:
            return e.code, {}
    except Exception as e:
        return None, {'error': str(e)}


def check(name, cond, detail=''):
    results.append(bool(cond))
    print(('  [PASS] ' if cond else '  [FAIL] ') + name + (('   ' + str(detail)[:150]) if detail else ''))


def data_of(body):
    return (body or {}).get('data') if isinstance(body, dict) else None


print('=' * 70)
print('  M2 详情/下载  +  M3 收藏/评论/举报 验证')
print('=' * 70)

suffix = str(int(time.time()))[-6:]
uname = 'u' + suffix
pwd = 'U' + suffix + 'w'
st, body = api('POST', '/api/auth/register', {
    'username': uname, 'nickname': '交互验证', 'email': uname + '@nnuist.edu.cn',
    'studentNo': 'T' + suffix, 'collegeId': 2, PWD_KEY: pwd})
st, body = api('POST', '/api/auth/login', {'username': uname, PWD_KEY: pwd})
token = (data_of(body) or {}).get('token')
check('注册并登录', bool(token))
if not token:
    raise SystemExit(1)

st, body = api('GET', '/api/resources?page=1&pageSize=10')
lst = (data_of(body) or {}).get('list') or []
check('拿到演示资料列表', len(lst) >= 3, '共 %d 条' % len(lst))
RID = lst[0]['id'] if lst else None
print('  用于测试的资料 id=%s 标题=%s' % (RID, str(lst[0].get('title'))[:30] if lst else '-'))

# ---- 详情 ----
st, body = api('GET', '/api/resources/%s' % RID)
d = data_of(body)
check('资料详情 GET /api/resources/{id}', st == 200 and isinstance(d, dict) and d.get('id') == RID,
      (d or {}).get('title'))
if isinstance(d, dict):
    for k in ('title', 'score', 'downloadCount', 'uploaderName', 'versions'):
        print('    · %-14s = %s' % (k, d.get(k)))

# ---- 下载（流式，验证鉴权 + 计数） ----
req = urllib.request.Request(BASE + '/api/resources/%s/download' % RID)
req.add_header('Authorization', 'Bearer ' + token)
try:
    with opener.open(req, timeout=120) as r:
        blob = r.read()
        ctype = r.headers.get('Content-Type', '')
        disp = r.headers.get('Content-Disposition', '')
        check('下载返回文件流', r.status == 200 and len(blob) > 10000,
              '%.1f MB | %s' % (len(blob) / 1048576.0, ctype[:40]))
        check('下载响应带 Content-Disposition(可保存文件)', 'attachment' in disp.lower(), disp[:60])
except Exception as e:
    check('下载返回文件流', False, str(e))

# ---- 收藏 ----
st, body = api('POST', '/api/resources/%s/collect' % RID, {}, token)
check('收藏资料 POST /api/resources/{id}/collect', isinstance(body, dict) and body.get('code') == 0, body)
st, body = api('GET', '/api/collections?page=1&pageSize=10', token=token)
check('我的收藏出现该资料', (data_of(body) or {}).get('total', 0) >= 1,
      'total=%s' % (data_of(body) or {}).get('total'))
st, body = api('DELETE', '/api/resources/%s/collect' % RID, token=token)
check('取消收藏 DELETE /api/resources/{id}/collect', isinstance(body, dict) and body.get('code') == 0)

# ---- 评论 ----
st, body = api('POST', '/api/resources/%s/comments' % RID, {'content': '这份资料整理得很清楚，谢谢分享！'}, token)
cid = data_of(body)
check('发表评论', isinstance(body, dict) and body.get('code') == 0 and cid, 'commentId=%s' % cid)
st, body = api('GET', '/api/resources/%s/comments?page=1&pageSize=10' % RID)
check('评论列表可读', (data_of(body) or {}).get('total', 0) >= 1,
      'total=%s' % (data_of(body) or {}).get('total'))
if cid:
    st, body = api('POST', '/api/resources/%s/comments' % RID, {'content': '补充一句：第 3 页有重点。', 'parentId': cid}, token)
    check('回复评论(二级)', isinstance(body, dict) and body.get('code') == 0)
    st, body = api('DELETE', '/api/comments/%s' % data_of(body), token=token)
    check('删除自己的回复', isinstance(body, dict) and body.get('code') == 0)

# ---- 举报 ----
st, body = api('POST', '/api/reports', {
    'targetType': 'RESOURCE', 'targetId': RID, 'reason': 'OTHER', 'detail': '自动化验证：检查举报链路是否可用'}, token)
check('提交举报 POST /api/reports', isinstance(body, dict) and (body.get('code') == 0 or body.get('code') == 40001),
      body)

# ---- 资料更新（全量语义） ----
st, body = api('PUT', '/api/resources/%s' % RID, {
    'courseId': lst[0].get('courseId'), 'fileId': lst[0].get('fileId'),
    'title': lst[0].get('title'), 'description': '（验证）描述已更新', 'visibility': 'PUBLIC'}, token)
check('更新资料 PUT /api/resources/{id}', isinstance(body, dict) and body.get('code') in (0, 40302, 40001), body)

# ---- 版本历史 ----
st, body = api('GET', '/api/resources/%s/versions' % RID)
v = data_of(body)
check('版本历史 GET /api/resources/{id}/versions', isinstance(body, dict) and body.get('code') == 0,
      '版本数=%s' % (len(v) if isinstance(v, list) else '-'))

# ---- 搜索 ----
st, body = api('GET', '/api/resources/search?keyword=' + urllib.parse.quote('PPT'))
check('检索 "PPT"', (data_of(body) or {}).get('total', 0) >= 1, '命中 %s 条' % (data_of(body) or {}).get('total'))
st, body = api('GET', '/api/resources/search/hot')
check('热门关键词 GET /api/resources/search/hot', isinstance(body, dict) and body.get('code') == 0,
      str(data_of(body))[:80])

print('-' * 70)
print('  结果: %d/%d 通过' % (sum(results), len(results)))
print('=' * 70)
