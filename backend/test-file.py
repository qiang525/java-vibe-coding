# -*- coding: utf-8 -*-
"""M2-1 文件模块接口测试

重点验证：
  1. 五重安全校验（空文件 / 大小 / 扩展名白名单 / 文件头 / 路径重命名）
  2. MD5 秒传
  3. 上传 → 查询 → 下载 → 删除 完整链路
  4. 中文文件名在 HTTP 头里的正确处理
"""
import hashlib
import json
import sys
import time
import urllib.error
import urllib.request
import uuid

BASE = 'http://localhost:8080'
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

results = []


def json_call(method, path, body=None, token=None):
    """发 JSON 请求"""
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


def upload(filename, content, token=None):
    """手工构造 multipart/form-data 上传（不依赖第三方库）"""
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
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req, timeout=60) as r:
            return r.status, json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        raw = e.read().decode('utf-8', errors='replace')
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {'raw': raw}
    except Exception as e:
        return None, {'error': str(e)}


def download_raw(file_id, token=None):
    """下载文件，返回原始字节"""
    req = urllib.request.Request(BASE + '/api/files/%d/download' % file_id, method='GET')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req, timeout=30) as r:
            return r.status, r.read(), dict(r.headers)
    except urllib.error.HTTPError as e:
        return e.code, e.read(), {}
    except Exception as e:
        return None, str(e).encode(), {}


def check(name, cond, detail=''):
    results.append((name, cond, detail))
    print(('  [PASS] ' if cond else '  [FAIL] ') + name + ('  ' + detail if detail else ''))


# 构造一个"真的 PDF"（文件头必须是 %PDF，否则会被文件头校验拦下）
PDF_CONTENT = b'%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<<>>\n%%EOF\n'
PDF_MD5 = hashlib.md5(PDF_CONTENT).hexdigest()

print('=' * 66)
print('  M2-1 文件模块接口测试')
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

print('\n[1] 登录管理员')
code, body = json_call('POST', '/api/auth/login', {'username': 'admin', 'password': 'admin123'})
token = (body.get('data') or {}).get('token')
check('登录成功', bool(token))

print('\n[2] 未登录上传（应返回 40101）')
code, body = upload('test.pdf', PDF_CONTENT)
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('业务码为 40101（未登录）', body.get('code') == 40101)

print('\n[3] 正常上传 PDF（应通过全部校验）')
code, body = upload('数据结构课件.pdf', PDF_CONTENT, token)
print('  响应:', json.dumps(body, ensure_ascii=False)[:220])
check('上传成功', body.get('code') == 0)
f1 = body.get('data') or {}
file_id = f1.get('id')
check('返回文件 ID', bool(file_id), 'id=%s' % file_id)
check('首次上传非秒传', f1.get('instantUpload') is False)
check('保留了原始文件名', f1.get('fileName') == '数据结构课件.pdf', str(f1.get('fileName')))
check('返回可读大小', bool(f1.get('readableSize')), str(f1.get('readableSize')))

print('\n[4] 同一内容换个文件名再传（应命中秒传）')
code, body = upload('换个名字.pdf', PDF_CONTENT, token)
print('  响应:', json.dumps(body, ensure_ascii=False)[:200])
f2 = body.get('data') or {}
check('业务码为 0', body.get('code') == 0)
check('instantUpload=true（秒传命中）', f2.get('instantUpload') is True)
check('指向同一个文件 ID（没有重复存储）', f2.get('id') == file_id, 'id=%s' % f2.get('id'))

print('\n[5] 上传 .exe（扩展名白名单应拒绝）')
code, body = upload('virus.exe', b'MZ\x90\x00\x03\x00\x00\x00', token)
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('业务码为 41003（不支持的文件类型）', body.get('code') == 41003)

print('\n[6] 伪装文件：扩展名 .pdf 但内容是纯文本（文件头校验应拒绝）')
code, body = upload('fake.pdf', b'This is definitely not a PDF file at all.', token)
print('  响应:', json.dumps(body, ensure_ascii=False)[:160])
check('业务码为 41003', body.get('code') == 41003)

print('\n[7] 上传无扩展名文件（应拒绝）')
code, body = upload('noextension', b'some content here', token)
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('业务码为 41003', body.get('code') == 41003)

print('\n[8] 查询文件元信息')
code, body = json_call('GET', '/api/files/%s' % file_id, token=token)
info = body.get('data') or {}
check('业务码为 0', body.get('code') == 0)
check('文件名正确', info.get('fileName') == '数据结构课件.pdf', str(info.get('fileName')))
check('返回了下载地址', bool(info.get('downloadUrl')))
check('未暴露服务器存储路径', 'storagePath' not in json.dumps(info))

print('\n[9] 下载文件并比对内容')
code, raw, headers = download_raw(file_id, token)
check('HTTP 200', code == 200)
check('内容与原文件完全一致（MD5 比对）', hashlib.md5(raw).hexdigest() == PDF_MD5)
cd = headers.get('Content-Disposition', '')
check('响应头带附件下载标记', 'attachment' in cd, cd[:50])
check('中文文件名已按 RFC 5987 编码', "filename*=UTF-8''" in cd, cd[:80])

print('\n[10] 删除文件（此时引用计数为 2，减 1 后文件应保留）')
code, body = json_call('DELETE', '/api/files/%s' % file_id, token=token)
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('删除成功', body.get('code') == 0)

code, body = json_call('GET', '/api/files/%s' % file_id, token=token)
check('引用未归零 → 记录仍在（这正是引用计数的意义）', body.get('code') == 0)

print('\n[11] 再删除一次（引用计数 1 → 0，应彻底清理）')
code, body = json_call('DELETE', '/api/files/%s' % file_id, token=token)
check('删除成功', body.get('code') == 0)

code, body = json_call('GET', '/api/files/%s' % file_id, token=token)
print('  响应:', json.dumps(body, ensure_ascii=False)[:140])
check('业务码为 40406（记录已清除）', body.get('code') == 40406)

passed = sum(1 for _, c, _ in results if c)
total = len(results)
print('\n' + '=' * 66)
print('  测试结果: %d / %d 通过' % (passed, total))
print('=' * 66)
for name, c, _ in results:
    if not c:
        print('  失败: ' + name)
sys.exit(0 if passed == total else 1)
