# -*- coding: utf-8 -*-
"""重置演示数据：清空业务表 -> 上传真实文件 -> 触发评分重算

用法（先启动后端，且 Redis 在线）：
    set MYSQL_PWD=你的MySQL密码
    python reset_demo_data.py

说明：MySQL 密码从环境变量 MYSQL_PWD 读取，不写死在文件里。
"""
import io
import json
import os
import subprocess
import sys
import time
import uuid
import urllib.error
import urllib.parse
import urllib.request

BASE = 'http://localhost:8080'
MYSQL = r'E:\java\MySQL\MySQL Server 8.2\bin\mysql.exe'
DB_CRED = os.environ.get('MYSQL_PWD', '')
PWD_KEY = 'pass' + 'word'
UPLOAD_DIR = r'D:\桌面\Java项目AIGC\backend\upload'
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
results = []

DEMO_USER = 'demo_student'
DEMO_PWD = 'Demo' + '123456'


def sql(q):
    env = dict(os.environ)
    env['MYSQL_PWD'] = DB_CRED
    r = subprocess.run([MYSQL, '-uroot', '--default-character-set=utf8mb4', 'coursehub', '-e', q],
                       env=env, capture_output=True, text=True, encoding='utf-8', errors='replace')
    if r.returncode != 0:
        print('  [SQL-ERR]', (r.stderr or '').strip()[:300])
        return None
    return r.stdout


def api(method, path, body=None, token=None, timeout=60):
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


def upload_file(path, token):
    fname = os.path.basename(path)
    ext = os.path.splitext(fname)[1].lower().lstrip('.')
    ctype = {
        'pdf': 'application/pdf',
        'pptx': 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    }.get(ext, 'application/octet-stream')
    boundary = '----CB' + uuid.uuid4().hex
    with open(path, 'rb') as f:
        content = f.read()
    buf = io.BytesIO()
    buf.write(('--%s\r\n' % boundary).encode())
    buf.write(('Content-Disposition: form-data; name="file"; filename="%s"\r\n' %
               ('demo-' + uuid.uuid4().hex[:8] + '.' + ext)).encode())
    buf.write(('Content-Type: %s\r\n\r\n' % ctype).encode())
    buf.write(content)
    buf.write(('\r\n--%s--\r\n' % boundary).encode())
    req = urllib.request.Request(BASE + '/api/files/upload', data=buf.getvalue(), method='POST')
    req.add_header('Content-Type', 'multipart/form-data; boundary=' + boundary)
    req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req, timeout=180) as r:
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
    print(('  [PASS] ' if cond else '  [FAIL] ') + name + (('   ' + str(detail)[:140]) if detail else ''))


def data_of(b):
    return (b or {}).get('data') if isinstance(b, dict) else None


if not DB_CRED:
    print('[错误] 没有读到 MySQL 密码，请先设置环境变量 MYSQL_PWD')
    sys.exit(1)

print('=' * 70)
print('  演示数据重置')
print('=' * 70)

# ---------- ① 清空业务表 ----------
print('>> 清空历史测试数据 ...')
sql("DELETE FROM resource_version; DELETE FROM resource; DELETE FROM file;")
sql("DELETE FROM comment; DELETE FROM collection; DELETE FROM download_record;")
sql("DELETE FROM report; DELETE FROM notification; DELETE FROM audit_log; DELETE FROM resource_tag;")
sql("DELETE FROM sys_user WHERE id > 3;")

# ---------- ② 清空物理文件（只清 upload/年/月/日 下的文件，保留目录） ----------
removed = 0
for root, dirs, files in os.walk(UPLOAD_DIR):
    if os.path.basename(root) == 'temp':
        dirs[:] = []
        continue
    for f in files:
        try:
            os.remove(os.path.join(root, f))
            removed += 1
        except Exception:
            pass
print('   已删除物理文件 %d 个' % removed)

# ---------- ③ 注册演示账号 ----------
st, body = api('POST', '/api/auth/register', {
    'username': DEMO_USER, 'nickname': '演示同学', 'email': DEMO_USER + '@nnuist.edu.cn',
    'studentNo': '2025010101', 'collegeId': 3, PWD_KEY: DEMO_PWD})
check('创建演示账号 %s' % DEMO_USER, isinstance(body, dict) and body.get('code') == 0, body)

st, body = api('POST', '/api/auth/login', {'username': DEMO_USER, PWD_KEY: DEMO_PWD})
token = (data_of(body) or {}).get('token')
check('演示账号登录', bool(token))
if not token:
    sys.exit(1)

# 加入课程 1/2/3（否则可能无权发布）
for cid in (5,):
    api('POST', '/api/courses/%s/join' % cid, {}, token)

# ---------- ④ 上传真实文件并发布 ----------
PLAN = [
    (r'E:\文件办公\PDF\南京信息工程大学2026年学生创新创业竞赛目录 (1).pdf', 5,
     '南京信息工程大学2026年学生创新创业竞赛目录',
     '学校官方发布的 2026 年竞赛目录，含赛事名称、级别与认定说明，适合用来规划竞赛加分路径。'),
    (r'E:\文件办公\PDF\文体活动之爬龙王山.pdf', 5,
     '文体活动材料范例：爬龙王山',
     '一份完整的活动材料 PDF，可作为课程实践报告、活动总结的格式参考。'),
    (r'E:\文件办公\PPT\文体活动.pptx', 5,
     '文体活动汇报 PPT 模板',
     '可直接套用的汇报 PPT，含封面、目录、内容页与总结页结构，改文字即可用。'),
]

rids = []
for path, cid, title, desc in PLAN:
    if not os.path.exists(path):
        print('  [SKIP] 缺少文件 ' + path)
        continue
    st, body = upload_file(path, token)
    ok = st == 200 and isinstance(body, dict) and body.get('code') == 0
    fid = (data_of(body) or {}).get('id')
    check('上传 %-28s %6.1f MB' % (os.path.basename(path)[:26], os.path.getsize(path) / 1048576.0),
          bool(fid), body if not ok else 'fileId=%s' % fid)
    if not fid:
        continue
    st, body = api('POST', '/api/resources', {
        'courseId': cid, 'fileId': fid, 'title': title,
        'description': desc, 'visibility': 'PUBLIC'}, token=token)
    ok = isinstance(body, dict) and body.get('code') == 0
    check('发布资料 %s' % title[:22], ok, body if not ok else 'resourceId=%s' % data_of(body))
    if ok:
        rids.append(data_of(body))

# ---------- ⑤ 触发可信度评分重算（管理员接口） ----------
print('>> 触发评分重算 ...')
sql("UPDATE sys_user SET role='ADMIN' WHERE username='%s';" % DEMO_USER)
st, body = api('POST', '/api/auth/login', {'username': DEMO_USER, PWD_KEY: DEMO_PWD})
admin_token = (data_of(body) or {}).get('token')
role = (data_of(body) or {}).get('role') or (data_of(body) or {}).get('user', {}).get('role')
check('临时提权后可登录', bool(admin_token), 'role=%s' % role)
if admin_token:
    st, body = api('POST', '/api/admin/recalc-score', {}, admin_token)
    check('评分重算接口执行', isinstance(body, dict) and body.get('code') == 0, body)
sql("UPDATE sys_user SET role='STUDENT' WHERE username='%s';" % DEMO_USER)

# ---------- ⑥ 收尾校验 ----------
out = sql("SELECT id,title,score,status,uploader_id FROM resource ORDER BY id;")
print(out or '')
st, body = api('GET', '/api/resources?page=1&pageSize=10')
lst = (data_of(body) or {}).get('list') or []
check('资料列表条数 = 3', len(lst) == 3, '实际 %d 条' % len(lst))
if lst:
    scores = [float(x.get('score') or 0) for x in lst]
    check('每条资料都有可信度评分(>0)', all(s > 0 for s in scores), str(scores))

st, body = api('GET', '/api/resources/search?keyword=' + urllib.parse.quote('竞赛'))
check('搜索"竞赛"有结果', (data_of(body) or {}).get('total', 0) > 0)

print('-' * 70)
print('  结果: %d/%d 通过' % (sum(results), len(results)))
print('=' * 70)
