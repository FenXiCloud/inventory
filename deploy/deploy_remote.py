# -*- coding: utf-8 -*-
"""
Publish local build artifacts to production (/data/jxc + nginx wwwroot).

Env (optional):
  JXC_DEPLOY_HOST / JXC_DEPLOY_USER / JXC_DEPLOY_PASSWORD
"""
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

REMOTE_BASE = "/data/jxc"
REMOTE_SERVER = f"{REMOTE_BASE}/server"
REMOTE_TMP = f"{REMOTE_BASE}/tmp"
REMOTE_MERCHANT_WEB = "/data/contract/nginx/wwwroot/jxc.fenxi365.com"
REMOTE_ADMIN_WEB = "/data/contract/nginx/wwwroot/jadmin.fenxi365.com"

LOCAL_MERCHANT_JAR = ROOT / "merchant/merchant-server/build/libs/jxc-0.1.jar"
LOCAL_ADMIN_JAR = ROOT / "admin/admin-server/build/libs/jadmin-0.1.jar"
LOCAL_MERCHANT_DIST = ROOT / "merchant/merchant-front/dist"
LOCAL_ADMIN_DIST = ROOT / "admin/admin-front/dist"


def require_file(path: Path, label: str) -> None:
    if not path.is_file():
        raise SystemExit(f"missing {label}: {path}")


def require_dir(path: Path, label: str) -> None:
    if not path.is_dir():
        raise SystemExit(f"missing {label}: {path}")


def make_tar_gz(src_dir: Path, out_path: Path) -> None:
    with tarfile.open(out_path, "w:gz") as tar:
        for p in src_dir.rglob("*"):
            if p.is_file():
                tar.add(p, arcname=str(p.relative_to(src_dir)).replace("\\", "/"))


def sftp_put(sftp: paramiko.SFTPClient, local: Path, remote: str) -> None:
    print(f"upload {local} -> {remote}")
    sftp.put(str(local), remote)


def ssh_run(client: paramiko.SSHClient, cmd: str, timeout: int = 180) -> str:
    print(f"$ {cmd}")
    _, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", "replace")
    err = stderr.read().decode("utf-8", "replace")
    code = stdout.channel.recv_exit_status()
    if out.strip():
        print(out.strip())
    if err.strip():
        print("[stderr]", err.strip()[:1000])
    if code != 0:
        raise SystemExit(f"remote command failed ({code}): {cmd}")
    return out


def deploy_frontend(client: paramiko.SSHClient, sftp: paramiko.SFTPClient, dist: Path, tar_name: str, web_root: str) -> None:
    with tempfile.TemporaryDirectory() as td:
        tar_path = Path(td) / tar_name
        make_tar_gz(dist, tar_path)
        remote_tar = f"{REMOTE_TMP}/{tar_name}"
        sftp_put(sftp, tar_path, remote_tar)
        ssh_run(
            client,
            " && ".join(
                [
                    f"mkdir -p {web_root}",
                    f"rm -rf {web_root}/*",
                    f"tar -xzf {remote_tar} -C {web_root}",
                    f"chmod -R a+rX {web_root}",
                    f"ls -la {web_root} | head -20",
                ]
            ),
            timeout=300,
        )


def main() -> None:
    require_file(LOCAL_MERCHANT_JAR, "merchant jar")
    require_file(LOCAL_ADMIN_JAR, "admin jar")
    require_dir(LOCAL_MERCHANT_DIST, "merchant dist")
    require_dir(LOCAL_ADMIN_DIST, "admin dist")

    print(f"deploy -> {USER}@{HOST}")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, username=USER, password=PASSWORD, timeout=30)
    sftp = client.open_sftp()
    try:
        ssh_run(client, f"mkdir -p {REMOTE_SERVER} {REMOTE_TMP}")

        # jars (tmp then atomic move)
        sftp_put(sftp, LOCAL_MERCHANT_JAR, f"{REMOTE_TMP}/jxc-0.1.jar")
        sftp_put(sftp, LOCAL_ADMIN_JAR, f"{REMOTE_TMP}/jadmin-0.1.jar")
        ssh_run(
            client,
            " && ".join(
                [
                    f"mv -f {REMOTE_TMP}/jxc-0.1.jar {REMOTE_SERVER}/jxc-0.1.jar",
                    f"mv -f {REMOTE_TMP}/jadmin-0.1.jar {REMOTE_SERVER}/jadmin-0.1.jar",
                    f"ls -lh {REMOTE_SERVER}/*.jar",
                ]
            ),
        )

        deploy_frontend(client, sftp, LOCAL_MERCHANT_DIST, "merchant-front.tar.gz", REMOTE_MERCHANT_WEB)
        deploy_frontend(client, sftp, LOCAL_ADMIN_DIST, "admin-front.tar.gz", REMOTE_ADMIN_WEB)

        # restart backends（线上 docker compose 插件不可用，直接 restart）
        ssh_run(client, "docker restart jxc_merchant jxc_admin", timeout=300)
        ssh_run(
            client,
            "sleep 25; docker ps --filter name=jxc_ --format 'table {{.Names}}\t{{.Status}}'; "
            "docker logs jxc_merchant 2>&1 | grep -E 'Started MerchantApplication|APPLICATION FAILED' | tail -2; "
            "docker logs jxc_admin 2>&1 | grep -E 'Started AdminApplication|APPLICATION FAILED' | tail -2",
            timeout=180,
        )
    finally:
        sftp.close()
        client.close()
    print("deploy done")


if __name__ == "__main__":
    main()
