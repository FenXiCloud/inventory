#!/usr/bin/env python3
"""查看 jxc nginx 配置"""
import paramiko

HOST = "112.124.55.97"
USER = "root"
KEY_PATH = r"C:\Users\Administrator\.ssh\id_ed25519"

def main():
    key = paramiko.Ed25519Key.from_private_key_file(KEY_PATH)
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, pkey=key, timeout=15)

    # nginx jxc 配置
    stdin, stdout, stderr = ssh.exec_command("docker exec contract_nginx cat /etc/nginx/conf.d/jxc.fenxi365.com.conf 2>&1")
    print("=== jxc.fenxi365.com.conf ===")
    print(stdout.read().decode())

    stdin, stdout, stderr = ssh.exec_command("docker exec contract_nginx cat /etc/nginx/conf.d/jadmin.fenxi365.com.conf 2>&1")
    print("\n=== jadmin.fenxi365.com.conf ===")
    print(stdout.read().decode())

    # 通过 nginx 调用 init API (带正确的 Host header)
    stdin, stdout, stderr = ssh.exec_command("curl -s -H 'Host: jxc.fenxi365.com' http://contract_nginx/init 2>&1")
    print("=== curl contract_nginx /init (Host: jxc.fenxi365.com) ===")
    out = stdout.read().decode().strip()
    print(out[:1000])

    # 看 application-db.yml（服务器端口）
    stdin, stdout, stderr = ssh.exec_command("cat /data/jxc/server/application-db.yml 2>&1")
    print("\n=== application-db.yml ===")
    print(stdout.read().decode())

    ssh.close()

if __name__ == '__main__':
    main()
