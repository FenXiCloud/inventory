# -*- coding: utf-8 -*-
import os
import paramiko

HOST = os.environ.get("JXC_DEPLOY_HOST", "112.124.55.97")
USER = os.environ.get("JXC_DEPLOY_USER", "root")
PASSWORD = os.environ.get("JXC_DEPLOY_PASSWORD", "@Flyemu891123")

cmds = [
    "cat /data/jxc/start.sh",
    "grep -R \"jxc.fenxi365\\|jadmin.fenxi365\\|merchant-front\\|admin-front\" /data/contract/nginx /etc/nginx /data/contract 2>/dev/null | head -80",
    "find /data/contract -maxdepth 5 -type d \\( -name 'jxc*' -o -name 'jadmin*' -o -name 'dist' \\) 2>/dev/null | head -60",
    "ls -la /data/contract 2>/dev/null | head -40",
    "docker exec contract_nginx sh -c 'ls -la /etc/nginx/conf.d; ls -la /usr/share/nginx/html; find / -name \"jxc.fenxi365*\" 2>/dev/null | head' 2>/dev/null | head -80",
]


def main():
    c = paramiko.SSHClient()
    c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    c.connect(HOST, username=USER, password=PASSWORD, timeout=30)
    for cmd in cmds:
        print("=" * 60)
        print("$", cmd)
        _, o, e = c.exec_command(cmd, timeout=90)
        print(o.read().decode("utf-8", "replace"))
        err = e.read().decode("utf-8", "replace")
        if err:
            print("[stderr]", err[:400])
    c.close()


if __name__ == "__main__":
    main()
