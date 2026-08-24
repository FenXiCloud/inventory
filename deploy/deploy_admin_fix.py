# -*- coding: utf-8 -*-
"""Deploy admin jar + admin-front only (merchant add-merchant fix)."""
from __future__ import annotations

import os
import tarfile
import tempfile
from pathlib import Path

import paramiko

ROOT = Path(__file__).resolve().parents[1]
HOST = os.environ.get("JXC_DEPLOY_HOST", "112.124.55.97")
USER = os.environ.get("JXC_DEPLOY_USER", "root")
PASSWORD = os.environ.get("JXC_DEPLOY_PASSWORD", "@Flyemu891123")

REMOTE_SERVER = "/data/jxc/server"
REMOTE_TMP = "/data/jxc/tmp"
REMOTE_ADMIN_WEB = "/data/contract/nginx/wwwroot/jadmin.fenxi365.com"
LOCAL_ADMIN_JAR = ROOT / "admin/admin-server/build/libs/jadmin-0.1.jar"
LOCAL_ADMIN_DIST = ROOT / "admin/admin-front/dist"


def make_tar_gz(src_dir: Path, out_path: Path) -> None:
    with tarfile.open(out_path, "w:gz") as tar:
        for p in src_dir.rglob("*"):
            if p.is_file():
                tar.add(p, arcname=str(p.relative_to(src_dir)).replace("\\", "/"))


def main() -> None:
    if not LOCAL_ADMIN_JAR.is_file():
        raise SystemExit(f"missing jar: {LOCAL_ADMIN_JAR}")
    if not LOCAL_ADMIN_DIST.is_dir():
        raise SystemExit(f"missing dist: {LOCAL_ADMIN_DIST}")

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, username=USER, password=PASSWORD, timeout=30)
    sftp = client.open_sftp()
    try:
        print("upload jar...")
        sftp.put(str(LOCAL_ADMIN_JAR), f"{REMOTE_TMP}/jadmin-0.1.jar")

        with tempfile.TemporaryDirectory() as td:
            tar_path = Path(td) / "admin-front.tar.gz"
            make_tar_gz(LOCAL_ADMIN_DIST, tar_path)
            print("upload front...")
            sftp.put(str(tar_path), f"{REMOTE_TMP}/admin-front.tar.gz")

        cmds = [
            f"mv -f {REMOTE_TMP}/jadmin-0.1.jar {REMOTE_SERVER}/jadmin-0.1.jar",
            f"mkdir -p {REMOTE_ADMIN_WEB}",
            f"rm -rf {REMOTE_ADMIN_WEB}/*",
            f"tar -xzf {REMOTE_TMP}/admin-front.tar.gz -C {REMOTE_ADMIN_WEB}",
            f"chmod -R a+rX {REMOTE_ADMIN_WEB}",
            "docker restart jxc_admin",
            "sleep 10",
            "docker logs jxc_admin 2>&1 | grep -E 'Started AdminApplication|APPLICATION FAILED|Started .*Application' | tail -5",
            "ls -lh /data/jxc/server/jadmin-0.1.jar",
        ]
        for cmd in cmds:
            print("$", cmd)
            _, o, e = client.exec_command(cmd, timeout=180)
            out = o.read().decode("utf-8", "replace")
            err = e.read().decode("utf-8", "replace")
            code = o.channel.recv_exit_status()
            if out.strip():
                print(out.strip())
            if err.strip():
                print("[stderr]", err.strip()[:500])
            if code != 0:
                raise SystemExit(f"failed ({code}): {cmd}")
    finally:
        sftp.close()
        client.close()
    print("admin deploy done")


if __name__ == "__main__":
    main()
