"""
分步部署脚本，每步都有日志输出
"""
from __future__ import annotations

import os
import sys
import tarfile
import tempfile
import time
from pathlib import Path

sys.path.insert(0, os.path.dirname(__file__))

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

def make_tar_gz(src_dir: Path, out_path: Path) -> None:
    with tarfile.open(out_path, "w:gz") as tar:
        for p in sorted(src_dir.rglob("*")):
            if p.is_file():
                tar.add(p, arcname=str(p.relative_to(src_dir)).replace("\\", "/"))
    print(f"  tar created: {out_path} ({os.path.getsize(out_path) / 1024:.0f} KB)")


def main() -> None:
    # validate
    for f, label in [
        (LOCAL_MERCHANT_JAR, "merchant jar"),
        (LOCAL_ADMIN_JAR, "admin jar"),
        (LOCAL_MERCHANT_DIST, "merchant dist"),
        (LOCAL_ADMIN_DIST, "admin dist"),
    ]:
        if isinstance(f, Path):
            check = f.is_file() if f.suffix else f.is_dir()
        else:
            check = f.is_dir()
        if not check:
            raise SystemExit(f"missing {label}: {f}")
        else:
            print(f"[OK] {label}: {f}")

    print(f"\n=== Connecting to {USER}@{HOST} ===")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, username=USER, password=PASSWORD, timeout=30)
    sftp = client.open_sftp()
    print("  connected.")

    def run(cmd, timeout=180, label=""):
        tag = f" [{label}]" if label else ""
        print(f"$ {cmd}{tag}")
        stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
        out = stdout.read().decode("utf-8", "replace").strip()
        err = stderr.read().decode("utf-8", "replace").strip()
        code = stdout.channel.recv_exit_status()
        if out:
            print(out)
        if err:
            print("[E]", err[:600])
        if code != 0:
            raise SystemExit(f"remote failed ({code}): {cmd}")
        return out

    def upload(local: Path, remote: str) -> None:
        size = os.path.getsize(local) / 1024 / 1024
        print(f"  uploading {local.name} ({size:.1f} MB) -> {remote} ...", end="", flush=True)
        t0 = time.time()
        sftp.put(str(local), remote)
        elapsed = time.time() - t0
        print(f" done ({elapsed:.1f}s, {size / elapsed:.2f} MB/s)")

    try:
        # prepare remote dirs
        run(f"mkdir -p {REMOTE_SERVER} {REMOTE_TMP}")

        # upload jars
        print("\n=== Uploading jars ===")
        upload(LOCAL_MERCHANT_JAR, f"{REMOTE_TMP}/jxc-0.1.jar")
        upload(LOCAL_ADMIN_JAR, f"{REMOTE_TMP}/jadmin-0.1.jar")

        # atomic move
        print("\n=== Moving jars ===")
        run(f"mv -f {REMOTE_TMP}/jxc-0.1.jar {REMOTE_SERVER}/jxc-0.1.jar && mv -f {REMOTE_TMP}/jadmin-0.1.jar {REMOTE_SERVER}/jadmin-0.1.jar && ls -lh {REMOTE_SERVER}/*.jar")

        # merchant frontend
        print("\n=== Deploying merchant frontend ===")
        with tempfile.TemporaryDirectory() as td:
            tar_path = Path(td) / "merchant-front.tar.gz"
            print(f"  packaging {LOCAL_MERCHANT_DIST} ...")
            make_tar_gz(LOCAL_MERCHANT_DIST, tar_path)
            remote_tar = f"{REMOTE_TMP}/merchant-front.tar.gz"
            upload(tar_path, remote_tar)
            run(
                f"mkdir -p {REMOTE_MERCHANT_WEB} && rm -rf {REMOTE_MERCHANT_WEB}/* && tar -xzf {remote_tar} -C {REMOTE_MERCHANT_WEB} && chmod -R a+rX {REMOTE_MERCHANT_WEB}",
                timeout=300,
                label="merchant-web"
            )

        # admin frontend
        print("\n=== Deploying admin frontend ===")
        with tempfile.TemporaryDirectory() as td:
            tar_path = Path(td) / "admin-front.tar.gz"
            print(f"  packaging {LOCAL_ADMIN_DIST} ...")
            make_tar_gz(LOCAL_ADMIN_DIST, tar_path)
            remote_tar = f"{REMOTE_TMP}/admin-front.tar.gz"
            upload(tar_path, remote_tar)
            run(
                f"mkdir -p {REMOTE_ADMIN_WEB} && rm -rf {REMOTE_ADMIN_WEB}/* && tar -xzf {remote_tar} -C {REMOTE_ADMIN_WEB} && chmod -R a+rX {REMOTE_ADMIN_WEB}",
                timeout=300,
                label="admin-web"
            )

        # restart
        print("\n=== Restarting services ===")
        run(f"cd {REMOTE_BASE} && docker compose up -d && docker restart jxc_merchant jxc_admin", timeout=300)
        time.sleep(10)
        run(
            "docker ps --filter name=jxc_ --format 'table {{.Names}}\t{{.Status}}' \n"
            "docker logs jxc_merchant --tail 5 2>&1\n"
            "docker logs jxc_admin --tail 5 2>&1",
            timeout=60,
            label="status"
        )

    finally:
        sftp.close()
        client.close()

    print("\n=== DEPLOY COMPLETE ===")


if __name__ == "__main__":
    main()
