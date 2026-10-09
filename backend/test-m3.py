# -*- coding: utf-8 -*-
"""M3 协作与治理测试：版本树 / 评论 / 收藏 / 举报"""
import json, sys, time, urllib.error, urllib.parse, urllib.request, uuid

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
print('  M3 协作与治理测试（版本/评论/收藏/举报）')
print('=' * 66)

print('\n[0] 等待后端就绪...')
for i in range(40):
    c, b = call('GET', '/api/health')
    if c == 200 and b.get('code') == 0:
        print('  就绪（%d 秒）' % (i*2)); break
    time.sleep(2)
else:
    sys.exit(1)

print('\n[1] 准备：登录 admin + 建课程 + 发资料')
c, b = call('POST', '/api/auth/login', {'username':'admin','password':'admin123'})
tk = (b.get('data') or {}).get('token')
check('admin 登录', bool(tk))

stamp = str(int(time.time()))
c, b = call('POST', '/api/courses', {'name': 'M3测试课程-' + stamp}, token=tk)
cid = b.get('data')

b = upload('v1.pdf', pdf('v1-' + stamp), tk)
fid1 = (b.get('data') or {}).get('id')
c, b = call('POST', '/api/resources', {
    'courseId': cid, 'fileId': fid1, 'title': '版本测试资料', 'visibility': 'PUBLIC'
}, token=tk)
rid = b.get('data')
check('资料发布成功', b.get('code') == 0, 'rid=%s' % rid)

# ==================== 版本树 ====================
print('\n[2] 【版本树】查询初始版本列表')
c, b = call('GET', '/api/resources/%s/versions' % rid)
vers = b.get('data') or []
check('有 1 个版本（v1）', len(vers) == 1, str(len(vers)))
check('版本号为 1', vers[0].get('versionNo') == 1)
check('v1 是当前版本', vers[0].get('isCurrent') is True)
check('带出了文件名', bool(vers[0].get('fileName')), str(vers[0].get('fileName')))

print('\n[3] 【版本树】上传新文件 → 新增版本')
b = upload('v2.pdf', pdf('v2-' + stamp), tk)
fid2 = (b.get('data') or {}).get('id')
c, b = call('POST', '/api/resources/%s/versions' % rid,
            {'fileId': fid2, 'changeLog': '修正了第三章的公式错误'}, token=tk)
check('新增版本成功', b.get('code') == 0, str(b.get('data')))
v2_id = b.get('data')

c, b = call('GET', '/api/resources/%s/versions' % rid)
vers = b.get('data') or []
check('现在有 2 个版本', len(vers) == 2, str(len(vers)))
check('v2 是当前版本', any(v.get('versionNo') == 2 and v.get('isCurrent') for v in vers))
check('v1 的历史还在', any(v.get('versionNo') == 1 for v in vers))
check('记录了变更说明', any('公式错误' in (v.get('changeLog') or '') for v in vers))

print('\n[4] 【版本树】回滚到 v1（应生成 v3，而不是删掉 v2）')
v1_id = next(v['id'] for v in vers if v.get('versionNo') == 1)
c, b = call('POST', '/api/resources/%s/rollback/%s' % (rid, v1_id), token=tk)
print('  响应:', json.dumps(b, ensure_ascii=False)[:140])
check('回滚成功', b.get('code') == 0)

c, b = call('GET', '/api/resources/%s/versions' % rid)
vers = b.get('data') or []
check('版本变成 3 个（回滚也留痕）', len(vers) == 3, str(len(vers)))
v3 = next((v for v in vers if v.get('versionNo') == 3), None)
check('v3 标记为回滚产生的版本', v3 and v3.get('isRollback') is True)
check('v3 记录了回滚来源', v3 and v3.get('rollbackFromVersionId') == v1_id)
check('v3 是当前版本', v3 and v3.get('isCurrent') is True)

print('\n[5] 【评论】发表一级评论')
c, b = call('POST', '/api/resources/%s/comments' % rid,
            {'content': '这份讲义整理得很清楚', 'type': 'NORMAL'}, token=tk)
check('评论成功', b.get('code') == 0)
cm1 = b.get('data')

c, b = call('POST', '/api/resources/%s/comments' % rid,
            {'content': '第三章有个笔误，建议修正', 'type': 'ERROR'}, token=tk)
cm2 = b.get('data')
check('第二条评论成功', b.get('code') == 0)

print('\n[6] 【评论】回复某条评论')
c, b = call('POST', '/api/resources/%s/comments' % rid,
            {'content': '已经改好了，感谢指正', 'parentId': cm2}, token=tk)
check('回复成功', b.get('code') == 0)

print('\n[7] 【评论】查询列表（两级结构）')
c, b = call('GET', '/api/resources/%s/comments' % rid, token=tk)
d = b.get('data') or {}
items = d.get('list') or []
check('返回 2 条一级评论', d.get('total') == 2, str(d.get('total')))
err_cm = next((x for x in items if x.get('id') == cm2), None)
check('纠错评论带出了 1 条回复', err_cm and len(err_cm.get('replies') or []) == 1,
      str(len((err_cm or {}).get('replies') or [])))
check('回复内容正确', err_cm and (err_cm['replies'][0].get('content') or '').startswith('已经改好了'))
check('canDelete 为 true（本人）', err_cm and err_cm.get('canDelete') is True)

print('\n[8] 【评论】资料评论数是否 +1 统计')
c, b = call('GET', '/api/resources/%s' % rid)
check('资料评论数为 3', (b.get('data') or {}).get('commentCount') == 3,
      str((b.get('data') or {}).get('commentCount')))

print('\n[9] 【收藏】收藏 / 重复收藏 / 我的收藏')
c, b = call('POST', '/api/resources/%s/collect' % rid, token=tk)
check('收藏成功', b.get('code') == 0)

c, b = call('POST', '/api/resources/%s/collect' % rid, token=tk)
print('  重复收藏响应:', json.dumps(b, ensure_ascii=False)[:120])
check('重复收藏返回 40905', b.get('code') == 40905)

c, b = call('GET', '/api/resources/%s/collected' % rid, token=tk)
check('已收藏状态为 true', b.get('data') is True)

c, b = call('GET', '/api/collections')
check('未登录查收藏 → 40101', b.get('code') == 40101)

c, b = call('GET', '/api/collections', token=tk)
d = b.get('data') or {}
check('我的收藏有 1 条', d.get('total') == 1, str(d.get('total')))
check('收藏列表带出资料标题', (d.get('list') or [{}])[0].get('title') == '版本测试资料',
      str((d.get('list') or [{}])[0].get('title')))

c, b = call('GET', '/api/resources/%s' % rid)
check('资料收藏数为 1', (b.get('data') or {}).get('favoriteCount') == 1,
      str((b.get('data') or {}).get('favoriteCount')))

print('\n[10] 【收藏】取消收藏')
c, b = call('DELETE', '/api/resources/%s/collect' % rid, token=tk)
check('取消收藏成功', b.get('code') == 0)
c, b = call('GET', '/api/resources/%s/collected' % rid, token=tk)
check('状态变回 false', b.get('data') is False)

print('\n[11] 【举报】普通用户提交举报')
stu = 'm3stu_' + stamp
call('POST', '/api/auth/register', {'username': stu, 'password':'123456','nickname':'举报学生'})
c, b = call('POST', '/api/auth/login', {'username': stu, 'password':'123456'})
tk_stu = (b.get('data') or {}).get('token')

c, b = call('POST', '/api/reports', {
    'targetType': 'RESOURCE', 'targetId': rid,
    'reason': 'COPYRIGHT', 'description': '疑似盗版扫描件'
}, token=tk_stu)
print('  响应:', json.dumps(b, ensure_ascii=False)[:140])
check('举报提交成功', b.get('code') == 0)

print('\n[12] 【举报】防重复举报')
c, b = call('POST', '/api/reports', {
    'targetType': 'RESOURCE', 'targetId': rid, 'reason': 'AD'
}, token=tk_stu)
print('  响应:', json.dumps(b, ensure_ascii=False)[:140])
check('重复举报返回 40902', b.get('code') == 40902)

print('\n[13] 【举报】非法对象类型')
c, b = call('POST', '/api/reports',
            {'targetType': 'HACK', 'targetId': 1, 'reason': 'AD'}, token=tk_stu)
check('非法类型返回 40001', b.get('code') == 40001)

print('\n[14] 【举报】权限：学生不能看举报列表')
c, b = call('GET', '/api/reports', token=tk_stu)
print('  响应:', json.dumps(b, ensure_ascii=False)[:120])
check('学生被拦（40302 权限不足）', b.get('code') == 40302)

print('\n[15] 【举报】管理员查看列表')
c, b = call('GET', '/api/reports?status=PENDING', token=tk)
d = b.get('data') or {}
check('管理员能看到举报', d.get('total') >= 1, str(d.get('total')))
rep = (d.get('list') or [{}])[0]
check('带出举报人昵称', bool(rep.get('reporterName')), str(rep.get('reporterName')))
check('带出被举报对象标题', rep.get('targetTitle') == '版本测试资料', str(rep.get('targetTitle')))

print('\n[16] 【举报】管理员处理举报')
rep_id = rep.get('id')
c, b = call('PUT', '/api/reports/%s/handle' % rep_id,
            {'status': 'HANDLED', 'handleNote': '已核实并下架'}, token=tk)
check('处理成功', b.get('code') == 0)

c, b = call('GET', '/api/reports?status=HANDLED', token=tk)
check('已处理的举报可查到', (b.get('data') or {}).get('total') >= 1)

print('\n[17] 【举报】重复处理同一条')
c, b = call('PUT', '/api/reports/%s/handle' % rep_id,
            {'status': 'REJECTED'}, token=tk)
print('  响应:', json.dumps(b, ensure_ascii=False)[:140])
check('返回 40904（状态不允许）', b.get('code') == 40904)

print('\n[18] 清理')
for x in [cm1, cm2]:
    if x: call('DELETE', '/api/comments/%s' % x, token=tk)
if rid: call('DELETE', '/api/resources/%s' % rid, token=tk)
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
