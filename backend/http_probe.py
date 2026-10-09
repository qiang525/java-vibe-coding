"""HTTP 探测工具：绕过代理请求本机接口并输出响应
用法: python http_probe.py <url>
"""
import sys
import urllib.request
import urllib.error

# 关键：显式禁用代理，否则会打到系统代理上
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def probe(url, timeout=3):
    try:
        with opener.open(url, timeout=timeout) as resp:
            body = resp.read().decode('utf-8', errors='replace')
            return resp.status, body
    except urllib.error.HTTPError as e:
        body = e.read().decode('utf-8', errors='replace')
        return e.code, body
    except Exception as e:
        return None, str(e)


if __name__ == '__main__':
    url = sys.argv[1]
    code, body = probe(url)
    if code is None:
        print('CONNECT_FAILED: %s' % body)
    else:
        print('HTTP %d' % code)
        print(body)
