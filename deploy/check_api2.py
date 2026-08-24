#!/usr/bin/env python3
"""检查线上 /init API 返回的菜单数据"""
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

    # 查看 docker-compose 中的端口映射
    print("=== jxc_merchant 端口 ===")
    stdin, stdout, stderr = ssh.exec_command("docker inspect jxc_merchant --format '{{json .HostConfig.PortBindings}}' 2>&1")
    print("PortBindings:", stdout.read().decode().strip())
    stdin, stdout, stderr = ssh.exec_command("docker inspect jxc_merchant --format '{{json .Config.ExposedPorts}}' 2>&1")
    print("ExposedPorts:", stdout.read().decode().strip())

    # 检查容器内是否有 curl
    stdin, stdout, stderr = ssh.exec_command("docker exec jxc_merchant which curl 2>&1; docker exec jxc_merchant which wget 2>&1")
    print("\n容器内工具:", stdout.read().decode().strip())
    err = stderr.read().decode().strip()
    if err:
        print("stderr:", err)

    # 尝试通过 docker network 调用
    # 检查容器 IP
    stdin, stdout, stderr = ssh.exec_command("docker inspect jxc_merchant --format '{{range .NetworkSettings.Networks}}{{.IPAddress}} {{end}}' 2>&1")
    ip = stdout.read().decode().strip()
    print("\njxc_merchant IP:", ip)

    # 从 nginx 容器调用
    stdin, stdout, stderr = ssh.exec_command(f"docker exec contract_nginx curl -s http://jxc_merchant:8080/init 2>&1")
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    print("\n=== nginx -> jxc_merchant:8080/init ===")
    print("len:", len(out))
    if err:
        print("stderr:", err)

    if out:
        try:
            data = json.loads(out)
            if 'data' in data and 'menus' in data['data']:
                menus = data['data']['menus']
                print(f"\n共 {len(menus)} 条菜单 (基础资料相关):")
                for m in menus:
                    pid = m.get('parentId')
                    mid = m.get('id')
                    if pid in [1, 6, 10] or mid in [1, 6, 10, 79, 80]:
                        print(f"  id={mid:>3}  pid={str(pid):>4}  title={m.get('title', '')}")
            else:
                print("结构:", json.dumps(data, ensure_ascii=False)[:500])
        except json.JSONDecodeError:
            print("非JSON:", out[:300])

    # 也试直接宿主机调用(如果端口映射了)
    stdin, stdout, stderr = ssh.exec_command("curl -s http://localhost:8080/init 2>&1")
    out2 = stdout.read().decode().strip()
    err2 = stderr.read().decode().strip()
    print(f"\n=== localhost:8080/init ===")
    print("len:", len(out2))
    if err2:
        print("stderr:", err2)

    ssh.close()

if __name__ == '__main__':
    main()
