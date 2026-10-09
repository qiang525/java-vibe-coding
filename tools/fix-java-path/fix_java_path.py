# -*- coding: utf-8 -*-
r"""
修复 Windows 系统 PATH 中失效的 Oracle javapath 条目
====================================================

【问题说明】
系统 PATH 的第一位是：
    C:\Program Files\Common Files\Oracle\Java\javapath
这是以前安装 Oracle JDK/JRE 时留下的「转发器」目录。由于
C:\Program Files\Java 已被删除，该目录下的 java.exe 已失效
（执行时崩溃，返回码 0xC0000409）。

因为它在 PATH 中排第一位，任何在命令行输入 `java` 的操作都会命中
这个坏文件，导致 java 命令不可用。

【本脚本做什么】
只从系统 PATH 中删除这 1 个失效条目，其余条目不碰。
删除后，PATH 中后面的 %JAVA_HOME%\bin（即 E:\java\jdk11\bin）
会正常接管 java 命令。

【安全措施】
1. 修改前自动备份完整 PATH 到同目录的 PATH_backup_时间戳.txt
2. 精确匹配，只删这一个条目，不会误删其他
3. 写入时保持 REG_EXPAND_SZ 类型（否则 %JAVA_HOME% 这类变量会失效）
4. 写入后立即读回校验

用法：
    一键修复.bat            # 正常修复（会自动请求管理员权限）
    python fix_java_path.py --dry-run   # 只预览，不修改
"""

import os
import sys
import ctypes
import winreg
import datetime

try:
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')
except Exception:
    pass

# ============ 配置 ============
BAD_ENTRY = r'C:\Program Files\Common Files\Oracle\Java\javapath'
SYS_ENV_KEY = r'SYSTEM\CurrentControlSet\Control\Session Manager\Environment'

WM_SETTINGCHANGE = 0x001A
HWND_BROADCAST = 0xFFFF
SMTO_ABORTIFHUNG = 0x0002


def is_admin():
    try:
        return bool(ctypes.windll.shell32.IsUserAnAdmin())
    except Exception:
        return False


def line(msg=''):
    print(msg)


def broadcast_change():
    """通知系统环境变量已变更，新开的程序能立即感知"""
    try:
        ctypes.windll.user32.SendMessageTimeoutW(
            HWND_BROADCAST, WM_SETTINGCHANGE, 0, 'Environment',
            SMTO_ABORTIFHUNG, 5000, None)
        return True
    except Exception:
        return False


def read_sys_path():
    k = winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, SYS_ENV_KEY)
    try:
        path, vtype = winreg.QueryValueEx(k, 'Path')
    finally:
        winreg.CloseKey(k)
    return path, vtype


def main():
    dry_run = '--dry-run' in sys.argv

    line('=' * 60)
    line('  修复失效的 Oracle javapath 条目')
    line('=' * 60)
    line()

    if dry_run:
        line('[模式] 预览模式（--dry-run），不会做任何修改')
    else:
        if not is_admin():
            line('[错误] 需要管理员权限。')
            line('       请通过「一键修复.bat」运行，或右键以管理员身份运行。')
            line()
            return 1

    # ---------- 1. 读取当前 PATH ----------
    try:
        path, vtype = read_sys_path()
    except PermissionError:
        line('[错误] 无法读取系统 PATH（权限不足）。')
        return 1

    type_names = {0: 'REG_NONE', 1: 'REG_SZ', 2: 'REG_EXPAND_SZ',
                  3: 'REG_BINARY', 4: 'REG_DWORD'}
    line('【当前状态】')
    line('  系统 PATH 长度 : %d 字符' % len(path))
    line('  数据类型       : %s (%d)' % (type_names.get(vtype, '?'), vtype))
    line()

    items = path.split(';')
    non_empty = [x for x in items if x.strip()]

    # ---------- 2. 定位目标 ----------
    hits = [i for i, x in enumerate(items)
            if x.strip().lower() == BAD_ENTRY.lower()]

    line('【目标条目】')
    line('  %s' % BAD_ENTRY)
    line('  匹配数量 : %d' % len(hits))
    line()

    if not hits:
        line('[结果] 未找到该条目，说明PATH 中已经不存在，无需修复。')
        line()
        return 0

    # ---------- 3. 组装新 PATH ----------
    new_items = [x for x in items
                 if x.strip().lower() != BAD_ENTRY.lower()]
    new_path = ';'.join(new_items)

    line('【修改预览】')
    line('  条目数 : %d → %d' % (len(non_empty), len([x for x in new_items if x.strip()])))
    line()
    line('  删除后，PATH 中剩余的 Java 相关项：')
    for x in new_items:
        if 'java' in x.lower() and x.strip():
            line('    · %s' % x.strip())
    line()

    if dry_run:
        line('[预览结束] 未做任何修改。去掉 --dry-run 即可真正执行。')
        line()
        return 0

    # ---------- 4. 备份 ----------
    script_dir = os.path.dirname(os.path.abspath(sys.argv[0])) or os.getcwd()
    ts = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
    bak = os.path.join(script_dir, 'PATH_backup_%s.txt' % ts)
    try:
        with open(bak, 'w', encoding='utf-8') as f:
            f.write(path)
        line('【备份】')
        line('  已保存到 : %s' % bak)
        line()
    except Exception as e:
        line('[错误] 备份失败，已中止操作以保护你的系统：%s' % e)
        return 1

    # ---------- 5. 写入 ----------
    line('【执行修改】')
    try:
        k = winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, SYS_ENV_KEY, 0,
                           winreg.KEY_SET_VALUE | winreg.KEY_QUERY_VALUE)
        # 关键：明确指定 REG_EXPAND_SZ，保证 %JAVA_HOME% 等变量仍能展开
        winreg.SetValueEx(k, 'Path', 0, winreg.REG_EXPAND_SZ, new_path)
        winreg.CloseKey(k)
        line('  写入成功')
    except PermissionError:
        line('  [错误] 写入被拒绝，请确认以管理员身份运行。')
        return 1
    except Exception as e:
        line('  [错误] 写入失败：%s' % e)
        return 1

    # ---------- 6. 校验 ----------
    line()
    line('【校验结果】')
    try:
        vpath, vtype2 = read_sys_path()
        ok_content = vpath == new_path
        ok_type = (vtype2 == winreg.REG_EXPAND_SZ)
        ok_gone = not any(x.strip().lower() == BAD_ENTRY.lower()
                          for x in vpath.split(';'))
        line('  内容一致      : %s' % ('是' if ok_content else '否'))
        line('  数据类型保持  : %s' % ('是 (REG_EXPAND_SZ)' if ok_type else '否'))
        line('  失效条目已移除: %s' % ('是' if ok_gone else '否'))
        line()
        if ok_content and ok_type and ok_gone:
            line('=' * 60)
            line('  修复成功！')
            line('=' * 60)
            line()
            line('接下来请：')
            line('  1. 关闭所有已打开的 CMD / PowerShell 窗口')
            line('  2. 关闭并重启 IntelliJ IDEA')
            line('  3. 重新打开命令行，执行：java -version')
            line('     应显示 java version "11.0.5"')
            line()
            broadcast_change()
            line('（已通知系统刷新环境变量）')
            line()
            return 0
        else:
            line('[警告] 校验未完全通过，但修改可能已生效。')
            line('       如需回退，用备份文件 %s' % bak)
            return 1
    except Exception as e:
        line('  校验异常：%s' % e)
        return 1


if __name__ == '__main__':
    sys.exit(main())
