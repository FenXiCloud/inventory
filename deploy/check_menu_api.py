#!/usr/bin/env python3
"""直接调用 /init API 检查菜单数据"""
import paramiko
import json

HOST = "112.124.55.97"
USER = "root"
KEY_PATH = r"C:\Users\Administrator\.ssh\id_ed25519"

def run(ssh, cmd):
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=15)
    return stdout.read().decode('utf-8', errors='replace'), stderr.read().decode('utf-8', errors='replace')

def main():
    print("=== 连接 ===")
    key = paramiko.Ed25519Key.from_private_key_file(KEY_PATH)
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, pkey=key, timeout=15)

    # 先找到 merchant 服务的端口
    o, _ = run(ssh, "docker port jxc_merchant")
    print("jxc_merchant ports:", o.strip())

    # 直接 curl
    curl = 'curl -s http://localhost:8080/init'
    o, e = run(ssh, f'docker exec jxc_merchant {curl}')
    print(f"\nAPI response length: {len(o)}")
    if o:
        try:
            data = json.loads(o)
            if 'data' in data and 'menus' in data['data']:
                menus = data['data']['menus']
                print(f"\n共 {len(menus)} 条菜单\n")
                # 找出基础资料相关的
                for m in menus:
                    pid = m.get('parentId')
                    mid = m.get('id')
                    if pid in [1, 6, 10] or mid in [1, 6, 10, 79, 80]:
                        print(f"  id={mid:>3}  pid={pid if pid else 'null':>4}  title={m.get('title', '')}")
            else:
                print("结构异常:", json.dumps(data, ensure_ascii=False)[:300])
        except json.JSONDecodeError:
            print("JSON解析失败, 前200字符:", o[:200])
    else:
        print("无返回, stderr:", e.strip())
    
    ssh.close()

if __name__ == '__main__':
    main()
