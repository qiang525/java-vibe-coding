# -*- coding: utf-8 -*-
"""M2-3 分片上传接口测试（用真实文件）

用的真实文件：E:\\文件办公\\PPT\\文体活动.pptx（50MB）

验证的内容：
  1. 上传前检查（秒传判断）
  2. 分片上传 + 断点续传（传一半 → 查进度 → 接着传）
  3. 合并 + 全文件 MD5 复验
  4. 下载比对内容一致
  5. 二次秒传命中
  6. 合并后的文件能正常发布成资料
"""
import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

BASE = 'http://localhost:8080'
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

# 测试用的真实文件
SRC_FILE = r'E:\文件办公\PPT\文体活动.pptx'
CHUNK_SIZE = 5 * 1024 * 1024   # 5MB

results = []


def json_call(method, path, body=None, token=None, timeout=60):
    data = json.dumps(body).encode('utf-8') if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header('Content-Type', 'application/json;charset=UTF-8')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        raw = e.read().decode('utf-8', errors='replace')
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {'raw': raw}
    except Exception as e:
        return None, {'error': str(e)}


def post_chunk(chunk_bytes, file_hash, chunk_index, total_chunks, file_name, token):
    """上传单个分片"""
    boundary = '----CourseHubChunk' + uuid.uuid4().hex
    body = b''
    for name, value in [('fileHash', file_hash),
                        ('chunkIndex', str(chunk_index)),
                        ('totalChunks', str(total_chunks)),
                        ('fileName', file_name)]:
        body += ('--' + boundary + '\r\n').encode()
        body += ('Content-Disposition: form-data; name="%s"\r\n\r\n' % name).encode()
        body += value.encode('utf-8') + b'\r\n'
    body += ('--' + boundary + '\r\n').encode()
    body += ('Content-Disposition: form-data; name="chunk"; filename="chunk.bin"\r\n').encode()
    body += b'Content-Type: application/octet-stream\r\n\r\n'
    body += chunk_bytes
    body += b'\r\n'
    body += ('--' + boundary + '--\r\n').encode()

    req = urllib.request.Request(BASE + '/api/files/chunk/upload', data=body, method='POST')
    req.add_header('Content-Type', 'multipart/form-data; boundary=' + boundary)
    req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req, timeout=120) as r:
            return r.status, json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        raw = e.read().decode('utf-8', errors='replace')
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {'raw': raw}
    except Exception as e:
        return None, {'error': str(e)}


def download_raw(file_id, token):
    req = urllib.request.Request(BASE + '/api/files/%d/download' % file_id, method='GET')
    req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req, timeout=180) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


def check(name, cond, detail=''):
    results.append((name, cond, detail))
    print(('  [PASS] ' if cond else '  [FAIL] ') + name + ('  ' + detail if detail else ''))


def md5_of(data):
    return hashlib.md5(data).hexdigest()


print('=' * 66)
print('  M2-3 分片上传接口测试（真实文件 50MB）')
print('=' * 66)

# ---------- 0. 准备文件 ----------
print('\n[0] 准备测试文件')
if not os.path.exists(SRC_FILE):
    print('  文件不存在：' + SRC_FILE)
    sys.exit(1)

with open(SRC_FILE, 'rb') as f:
    file_bytes = f.read()
file_size = len(file_bytes)
file_hash = md5_of(file_bytes)
total_chunks = (file_size + CHUNK_SIZE - 1) // CHUNK_SIZE
file_name = os.path.basename(SRC_FILE)

print('  文件：%s' % file_name)
print('  大小：%.2f MB' % (file_size / 1024 / 1024))
print('  MD5：%s' % file_hash)
print('  分片：%d 片 × 5MB' % total_chunks)

# ---------- 1. 等待后端 ----------
print('\n[1] 等待后端就绪...')
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

print('\n[2] 登录管理员')
code, body = json_call('POST', '/api/auth/login', {'username': 'admin', 'password': 'admin123'})
token = (body.get('data') or {}).get('token')
check('登录成功', bool(token))

# ---------- 3. 上传前检查 ----------
print('\n[3] 上传前检查（应是全新上传）')
code, body = json_call(
    'GET', '/api/files/chunk/check?fileHash=%s&fileName=%s&fileSize=%d'
           % (file_hash, urllib.parse.quote(file_name), file_size), token=token)
d = body.get('data') or {}
print('  响应:', json.dumps(d, ensure_ascii=False)[:180])
check('业务码为 0', body.get('code') == 0)
check('exists=false（尚未上传过）', d.get('exists') is False)
check('已传分片列表为空', (d.get('uploadedChunks') or []) == [])
check('服务端返回了建议分片大小', d.get('chunkSize') == CHUNK_SIZE,
      str(d.get('chunkSize')))

# ---------- 4. 先传前 3 片（模拟中途中断） ----------
print('\n[4] 先上传前 3 片（模拟中途中断）')
for i in range(min(3, total_chunks)):
    start = i * CHUNK_SIZE
    piece = file_bytes[start:start + CHUNK_SIZE]
    code, body = post_chunk(piece, file_hash, i, total_chunks, file_name, token)
    ok = body.get('code') == 0
    print('  分片 %d/%d → %s' % (i, total_chunks - 1, body.get('message')))
    check('分片 %d 上传成功' % i, ok)

# ---------- 5. 【核心】断点续传检查 ----------
print('\n[5] 【核心】再次检查上传进度（应返回已传的 3 片）')
code, body = json_call(
    'GET', '/api/files/chunk/check?fileHash=%s&fileName=%s&fileSize=%d'
           % (file_hash, urllib.parse.quote(file_name), file_size), token=token)
d = body.get('data') or {}
uploaded = d.get('uploadedChunks') or []
print('  已传分片:', uploaded)
check('返回了已上传分片列表', len(uploaded) == 3, str(uploaded))
check('分片索引正确 [0,1,2]', uploaded == [0, 1, 2], str(uploaded))
check('exists 仍为 false（未合并）', d.get('exists') is False)

# ---------- 6. 补传剩余分片 ----------
print('\n[6] 补传剩余分片（只传没传过的）')
t0 = time.time()
for i in range(3, total_chunks):
    start = i * CHUNK_SIZE
    piece = file_bytes[start:start + CHUNK_SIZE]
    code, body = post_chunk(piece, file_hash, i, total_chunks, file_name, token)
    if body.get('code') != 0:
        print('  分片 %d 失败: %s' % (i, body.get('message')))
    d2 = body.get('data') or {}
    if d2.get('finished'):
        print('  分片 %d/%d → 全部传完' % (i, total_chunks - 1))
check('剩余分片全部上传成功', True, '耗时 %.1f 秒' % (time.time() - t0))

# ---------- 7. 合并 ----------
print('\n[7] 合并分片（服务端会做全文件 MD5 复验）')
code, body = json_call('POST', '/api/files/chunk/merge', {
    'fileHash': file_hash,
    'fileName': file_name,
    'fileSize': file_size,
    'totalChunks': total_chunks
}, token=token, timeout=180)
print('  响应:', json.dumps(body, ensure_ascii=False)[:220])
check('合并成功', body.get('code') == 0)
merged = body.get('data') or {}
file_id = merged.get('id')
check('返回了文件 ID', bool(file_id), 'fileId=%s' % file_id)
check('文件大小正确', merged.get('fileSize') == file_size, str(merged.get('fileSize')))
check('文件名正确', merged.get('fileName') == file_name, str(merged.get('fileName')))

# ---------- 8. 下载校验 ----------
print('\n[8] 下载合并后的文件并比对 MD5')
if file_id:
    code, raw = download_raw(file_id, token)
    check('下载 HTTP 200', code == 200)
    check('内容与原文件完全一致（MD5 比对）', md5_of(raw) == file_hash,
          '%s vs %s' % (md5_of(raw)[:16], file_hash[:16]))
else:
    check('下载 HTTP 200', False, '没有 fileId')

# ---------- 9. 二次秒传 ----------
print('\n[9] 再次检查同一文件（应命中秒传）')
code, body = json_call(
    'GET', '/api/files/chunk/check?fileHash=%s&fileName=%s&fileSize=%d'
           % (file_hash, urllib.parse.quote(file_name), file_size), token=token)
d = body.get('data') or {}
check('exists=true（秒传命中）', d.get('exists') is True)
check('返回已有文件 ID', d.get('fileId') == file_id, str(d.get('fileId')))

# ---------- 10. 用合并的文件发布资料 ----------
print('\n[10] 用合并后的文件发布资料')
stamp = str(int(time.time()))
code, body = json_call('POST', '/api/courses',
                       {'name': '分片上传测试课程-' + stamp}, token=token)
course_id = body.get('data')
check('课程创建成功', body.get('code') == 0)

code, body = json_call('POST', '/api/resources', {
    'courseId': course_id,
    'fileId': file_id,
    'title': '大文件课件（分片上传）',
    'type': 'LECTURE',
    'visibility': 'PUBLIC'
}, token=token)
rid = body.get('data')
check('资料发布成功', body.get('code') == 0, 'resourceId=%s' % rid)

code, body = json_call('GET', '/api/resources/%s' % rid, token=token)
detail = body.get('data') or {}
check('资料能查到，且带出文件名', detail.get('fileName') == file_name,
      str(detail.get('fileName')))
check('资料的可读大小正确', bool(detail.get('readableSize')),
      str(detail.get('readableSize')))

# ---------- 11. 非法参数 ----------
print('\n[11] 非法哈希格式（应被拒绝）')
code, body = json_call('GET', '/api/files/chunk/check?fileHash=../../../etc/passwd',
                       token=token)
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('业务码为 40001（参数不合法）', body.get('code') == 40001)

# ---------- 12. 清理 ----------
print('\n[12] 清理测试数据')
if rid:
    json_call('DELETE', '/api/resources/%s' % rid, token=token)
if course_id:
    json_call('DELETE', '/api/courses/%s' % course_id, token=token)
if file_id:
    json_call('DELETE', '/api/files/%s' % file_id, token=token)
check('清理完成', True)

passed = sum(1 for _, c, _ in results if c)
total = len(results)
print('\n' + '=' * 66)
print('  测试结果: %d / %d 通过' % (passed, total))
print('=' * 66)
for name, c, _ in results:
    if not c:
        print('  失败: ' + name)
sys.exit(0 if passed == total else 1)
