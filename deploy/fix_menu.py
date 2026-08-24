#!/usr/bin/env python3
"""修复线上菜单：将账户管理和计量单位移到辅助资料下"""
import paramiko

HOST = "112.124.55.97"
USER = "root"
KEY_PATH = r"C:\Users\Administrator\.ssh\id_ed25519"
PWD = "@Flyemu891123"
DB_CONTAINER = "contract_mysql8"
DB = "jxc"

def run(ssh, cmd):
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=15)
    o = stdout.read().decode('utf-8', errors='replace')
    e = stderr.read().decode('utf-8', errors='replace')
    # mysql warning 不算错误
    if 'ERROR' in e:
        print("ERR:", e.strip())
    elif 'Warning' in e:
        pass  # ignore mysql warnings
    elif e.strip():
        print("STDERR:", e.strip())
    return o.strip()

def main():
    print("=== 连接服务器 ===")
    key = paramiko.Ed25519Key.from_private_key_file(KEY_PATH)
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, pkey=key, timeout=15)
    print("OK\n")

    def mysql(sql):
        # 用 printf 传 SQL 避免 shell 转义问题
        cmd = f"docker exec {DB_CONTAINER} mysql -uroot -p'{PWD}' {DB} --default-character-set=utf8mb4 -e \"{sql}\""
        return run(ssh, cmd)

    # 修复前状态
    print("=== 修复前 (id=79,80,23,24) ===")
    print(mysql("SELECT id, parent_id, name, pos FROM jxc_menu WHERE id IN (79,80,23,24);"))
    
    # 执行 UPDATE
    print("\n=== 执行 UPDATE ===")
    print(mysql("UPDATE jxc_menu SET parent_id = 10, pos = 2 WHERE id = 79;"))
    print(mysql("UPDATE jxc_menu SET parent_id = 10, pos = 3 WHERE id = 80;"))
    print(mysql("UPDATE jxc_menu SET pos = 4 WHERE id = 23;"))
    print(mysql("UPDATE jxc_menu SET pos = 5 WHERE id = 24;"))
    print("UPDATE done")

    # 修复后状态
    print("\n=== 修复后 (id=79,80,23,24) ===")
    print(mysql("SELECT id, parent_id, name, pos FROM jxc_menu WHERE id IN (79,80,23,24);"))

    # 辅助资料完整子菜单
    print("\n=== 辅助资料(parent_id=10) 全部子菜单 ===")
    print(mysql("SELECT id, parent_id, name, pos FROM jxc_menu WHERE parent_id = 10 ORDER BY pos;"))

    ssh.close()
    print("\n=== done ===")

if __name__ == '__main__':
    main()
