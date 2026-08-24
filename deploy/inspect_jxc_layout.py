# -*- coding: utf-8 -*-
import os
import paramiko

HOST = os.environ.get("JXC_DEPLOY_HOST", "112.124.55.97")
USER = os.environ.get("JXC_DEPLOY_USER", "root")
PASSWORD = os.environ.get("JXC_DEPLOY_PASSWORD", "@Flyemu891123")

cmds = [
    "ls -laR /data/jxc | head -200",
    "docker inspect jxc_merchant --format '{{json .Mounts}}' | python3 -m json.tool 2>/dev/null || docker inspect jxc_merchant --format '{{json .Mounts}}'",
    "docker inspect jxc_admin --format '{{json .Mounts}}' | python3 -m json.tool 2>/dev/null || docker inspect jxc_admin --format '{{json .Mounts}}'",
    "cat /data/jxc/server/docker-compose.yml 2>/dev/null || cat /data/jxc/docker-compose.yml 2>/dev/null",
    "ls -la /data/jxc/web /data/jxc/front /data/jxc/nginx /data/jxc/html 2>/dev/null; ls /data/jxc",
]


def main():
    c = paramiko.SSHClient()
    c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    c.connect(HOST, username=USER, password=PASSWORD, timeout=30)
    for cmd in cmds:
        print("=" * 60)
        print("$", cmd)
        _, o, e = c.exec_command(cmd, timeout=60)
        print(o.read().decode("utf-8", "replace"))
        err = e.read().decode("utf-8", "replace")
        if err:
            print("[stderr]", err[:500])
    c.close()


if __name__ == "__main__":
    main()
