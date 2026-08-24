#!/usr/bin/env python3
"""完整检查线上菜单问题"""
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

    # 1. 检查 nginx 配置，看 /init 路由到哪
    print("=== Nginx 配置中 /init 路由 ===")
    stdin, stdout, stderr = ssh.exec_command("docker exec contract_nginx grep -r 'init' /etc/nginx/ 2>&1")
    out = stdout.read().decode().strip()
    if out:
        print(out)
    else:
        print("未找到 /init 相关配置")

    # 2. 检查 jxc_merchant 容器中服务端口
    stdin, stdout, stderr = ssh.exec_command("docker exec jxc_merchant netstat -tlnp 2>&1 || docker exec jxc_merchant ss -tlnp 2>&1")
    out = stdout.read().decode().strip()
    print(f"\n=== jxc_merchant 监听端口 ===\n{out}")

    # 3. 查看 nginx 的 jxc 相关配置
    print("\n=== Nginx jxc 相关配置 ===")
    stdin, stdout, stderr = ssh.exec_command("docker exec contract_nginx grep -l 'jxc' /etc/nginx/conf.d/*.conf 2>&1")
    conf_files = stdout.read().decode().strip()
    if conf_files:
        for f in conf_files.split('\n'):
            f = f.strip()
            if f:
                stdin, stdout, stderr = ssh.exec_command(f"docker exec contract_nginx cat {f} 2>&1")
                print(f"\n--- {f} ---")
                print(stdout.read().decode())

    # 4. 查看 docker-compose 中 jxc_merchant 的环境变量
    print("\n=== jxc_merchant 环境变量 ===")
    stdin, stdout, stderr = ssh.exec_command("docker inspect jxc_merchant --format '{{range .Config.Env}}{{println .}}{{end}}' 2>&1")
    envs = stdout.read().decode().strip()
    for line in envs.split('\n'):
        if any(k in line for k in ['PORT', 'HOST', 'HOST', 'URL', 'DB']):
            print(f"  {line.strip()}")

    ssh.close()

if __name__ == '__main__':
    main()
