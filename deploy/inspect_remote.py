# -*- coding: utf-8 -*-
"""Inspect production layout before deploy. Credentials via env or defaults used last time."""
import os
import paramiko

HOST = os.environ.get("JXC_DEPLOY_HOST", "112.124.55.97")
USER = os.environ.get("JXC_DEPLOY_USER", "root")
PASSWORD = os.environ.get("JXC_DEPLOY_PASSWORD", "@Flyemu891123")

cmds = [
    "pwd; ls -la /root 2>/dev/null | head -40",
    "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'",
    "ls -la /opt 2>/dev/null; ls -la /data 2>/dev/null; ls -la /www 2>/dev/null; ls -la /home 2>/dev/null | head",
    "find / -maxdepth 4 -type d \\( -name 'jxc*' -o -name 'jadmin*' -o -name 'merchant*' \\) 2>/dev/null | head -80",
    "find / -maxdepth 5 -name 'jxc-0.1.jar' -o -name 'jadmin-0.1.jar' 2>/dev/null | head -20",
    "ls -la /etc/nginx/conf.d 2>/dev/null; ls -la /www/wwwroot 2>/dev/null | head -40",
]


def main():
    c = paramiko.SSHClient()
    c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    c.connect(HOST, username=USER, password=PASSWORD, timeout=30)
    for cmd in cmds:
        print("=" * 60)
        print("$", cmd)
        _, o, e = c.exec_command(cmd, timeout=120)
        out = o.read().decode("utf-8", "replace")
        err = e.read().decode("utf-8", "replace")
        if out:
            print(out)
        if err:
            print("[stderr]", err[:800])
    c.close()


if __name__ == "__main__":
    main()
