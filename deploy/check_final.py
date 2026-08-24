#!/usr/bin/env python3
"""最终验证 - 确认线上 /api/init 返回菜单"""
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

    # 方式1: 容器内直接 curl localhost:8410/init
    stdin, stdout, stderr = ssh.exec_command("docker exec jxc_merchant curl -s http://localhost:8410/init 2>&1")
    resp = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    print(f"=== jxc_merchant:8410/init (容器内) ===")
    print(f"len={len(resp)} err={err[:100] if err else 'none'}")

    if resp:
        try:
            data = json.loads(resp)
            if 'data' in data and 'menus' in data['data']:
                menus = data['data']['menus']
                print(f"共 {len(menus)} 条菜单")
                for m in menus:
                    pid = m.get('parentId')
                    mid = m.get('id')
                    if pid in [1, 6, 10] or mid in [1, 6, 10, 79, 80]:
                        print(f"  id={mid:>3}  pid={str(pid):>4}  title={m.get('title', '')}")
            elif 'code' in data:
                print(f"code={data['code']} msg={data.get('msg', '')}")
            else:
                print("结构:", json.dumps(data, ensure_ascii=False)[:300])
        except json.JSONDecodeError:
            print("非JSON:", resp[:300])

    # 方式2: 通过 nginx proxy (正确的路径 /api/init)
    stdin, stdout, stderr = ssh.exec_command("curl -s -H 'Host: jxc.fenxi365.com' http://contract_nginx/api/init 2>&1")
    resp2 = stdout.read().decode().strip()
    err2 = stderr.read().decode().strip()
    print(f"\n=== nginx -> /api/init (Host: jxc.fenxi365.com) ===")
    print(f"len={len(resp2)} err={err2[:100] if err2 else 'none'}")
    if resp2:
        print(resp2[:500])

    ssh.close()

if __name__ == '__main__':
    main()
