#!/usr/bin/env python3
"""最终检查 - 确认菜单系统和API"""
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

    # 1. Nginx配置位置
    print("=== Nginx 配置文件 ===")
    stdin, stdout, stderr = ssh.exec_command("docker exec contract_nginx ls /etc/nginx/conf.d/ 2>&1")
    out = stdout.read().decode().strip()
    print(out)
    
    stdin, stdout, stderr = ssh.exec_command("docker exec contract_nginx ls /etc/nginx/sites-enabled/ 2>&1")
    out2 = stdout.read().decode().strip()
    print(out2)

    # 2. 看docker-compose中jxc_merchant的完整定义
    print("\n=== jxc_merchant docker配置 ===")
    stdin, stdout, stderr = ssh.exec_command("docker inspect jxc_merchant --format '{{json .Config.Cmd}}' 2>&1")
    cmd = stdout.read().decode().strip()
    print("Cmd:", cmd[:500])
    
    stdin, stdout, stderr = ssh.exec_command("docker inspect jxc_merchant --format '{{json .Config.Entrypoint}}' 2>&1")
    ep = stdout.read().decode().strip()
    print("Entrypoint:", ep[:500])

    # 3. 检查容器内是否运行 Java 进程
    print("\n=== jxc_merchant 进程 ===")
    stdin, stdout, stderr = ssh.exec_command("docker exec jxc_merchant ps aux 2>&1 || docker exec jxc_merchant ls /proc/*/cmdline 2>&1")
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    print("out:", out[:500])
    if err:
        print("err:", err[:300])

    # 4. 检查 docker-compose 中的环境变量和端口映射
    print("\n=== docker-compose 中 jxc 相关 ===")
    stdin, stdout, stderr = ssh.exec_command("cat /data/jxc/docker-compose.yml 2>&1 | head -100")
    compose = stdout.read().decode()
    # 只打印 jxc 相关行
    in_jxc = False
    for line in compose.split('\n'):
        stripped = line.strip()
        if 'jxc_merchant' in stripped.lower() or 'jxc_admin' in stripped.lower() or 'jxc_db' in stripped.lower():
            in_jxc = True
            print(line)
        elif in_jxc:
            if stripped.startswith('#') or stripped == '':
                print(line)
            elif not stripped.startswith('-') and ':' in stripped and ' ' not in stripped.split(':')[0]:
                in_jxc = False
            else:
                print(line)

    ssh.close()

if __name__ == '__main__':
    main()
