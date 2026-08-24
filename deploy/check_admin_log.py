#!/usr/bin/env python3
import paramiko

HOST = "112.124.55.97"
USER = "root"
KEY_PATH = r"C:\Users\Administrator\.ssh\id_ed25519"

def run(ssh, cmd):
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=30)
    o = stdout.read().decode('utf-8', errors='replace')
    e = stderr.read().decode('utf-8', errors='replace')
    return o.strip(), e.strip()

def main():
    key = paramiko.Ed25519Key.from_private_key_file(KEY_PATH)
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, pkey=key, timeout=15)

    # 列出容器
    print("=== docker containers ===")
    o, e = run(ssh, "docker ps --format '{{.Names}} {{.Image}} {{.Ports}}'")
    print(o)
    if e:
        print("ERR:", e)

    # 查看 admin 日志
    print("\n=== admin logs (last 200 lines) ===")
    o, e = run(ssh, "docker logs --tail 200 jxc_admin 2>&1")
    print(o[-8000:] if len(o) > 8000 else o)
    if e:
        print("ERR:", e)

    ssh.close()

if __name__ == '__main__':
    main()
