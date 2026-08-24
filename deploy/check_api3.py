#!/usr/bin/env python3
"""直接检查线上 /init API"""
import paramiko
import json

HOST = "112.124.55.97"
USER = "root"
KEY_PATH = r"C:\Users\Administrator\.ssh\id_ed25519"

def main():
    key = paramiko.Ed25519Key.from_private_key_file(KEY_PATH)
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, pkey=key, timeout=15)

    # localhost:8080 返回了什么
    stdin, stdout, stderr = ssh.exec_command("curl -s -o /tmp/api_resp.txt -w '%{http_code}' http://localhost:8080/init 2>&1")
    code = stdout.read().decode().strip()
    print("HTTP状态码:", code)

    stdin, stdout, stderr = ssh.exec_command("cat /tmp/api_resp.txt 2>&1")
    resp = stdout.read().decode().strip()
    print("响应内容:", resp[:500])

    # 试 jxc_merchant 内部
    # 先用 Docker exec 直接执行 Java 程序获取 /init
    # 注意：需要携带 Cookie/Token
    # 先试无认证
    stdin, stdout, stderr = ssh.exec_command("docker exec jxc_merchant curl -s -o /tmp/init_resp.txt -w '%{http_code}' http://localhost:8080/init 2>&1")
    code2 = stdout.read().decode().strip()
    print(f"\n容器内 HTTP状态码: {code2}")

    stdin, stdout, stderr = ssh.exec_command("docker exec jxc_merchant cat /tmp/init_resp.txt 2>&1")
    resp2 = stdout.read().decode().strip()
    print(f"容器内响应: {resp2[:800]}")

    if resp2:
        try:
            data = json.loads(resp2)
            if 'data' in data and 'menus' in data['data']:
                menus = data['data']['menus']
                print(f"\n=== /init 返回菜单 (基础资料相关) ===")
                for m in menus:
                    pid = m.get('parentId')
                    mid = m.get('id')
                    if pid in [1, 6, 10] or mid in [1, 6, 10, 79, 80]:
                        print(f"  id={mid:>3}  pid={str(pid):>4}  title={m.get('title', '')}  key={m.get('key','')}")
        except json.JSONDecodeError:
            pass

    ssh.close()

if __name__ == '__main__':
    main()
