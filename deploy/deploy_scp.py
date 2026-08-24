"""
Deploy using OpenSSH scp + ssh (key-based auth, fast).
"""
import os, subprocess, tarfile, tempfile, time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SSH = r"C:\Windows\System32\OpenSSH\ssh.exe"
SCP = r"C:\Windows\System32\OpenSSH\scp.exe"

HOST = "root@112.124.55.97"
SSH_OPTS = "-o StrictHostKeyChecking=no -o PasswordAuthentication=no"
REMOTE_BASE = "/data/jxc"
REMOTE_TMP = f"{REMOTE_BASE}/tmp"
REMOTE_SERVER = f"{REMOTE_BASE}/server"

LOCAL_JXC = ROOT / "merchant/merchant-server/build/libs/jxc-0.1.jar"
LOCAL_JADMIN = ROOT / "admin/admin-server/build/libs/jadmin-0.1.jar"
LOCAL_MERCHANT_DIST = ROOT / "merchant/merchant-front/dist"
LOCAL_ADMIN_DIST = ROOT / "admin/admin-front/dist"
REMOTE_MERCHANT_WEB = "/data/contract/nginx/wwwroot/jxc.fenxi365.com"
REMOTE_ADMIN_WEB = "/data/contract/nginx/wwwroot/jadmin.fenxi365.com"

def run_ssh(cmd, label=""):
    full = f"{SSH} {SSH_OPTS} {HOST} \"{cmd}\""
    tag = f" [{label}]" if label else ""
    print(f"$ {full[:120]}...{tag}")
    result = subprocess.run(full, shell=True, capture_output=True, text=True, timeout=300)
    if result.stdout.strip():
        print(result.stdout.strip())
    if result.stderr.strip():
        print(result.stderr.strip()[:500])
    if result.returncode != 0:
        raise SystemExit(f"SSH failed ({result.returncode})")
    return result.stdout

def run_scp(local: Path, remote: str):
    size = os.path.getsize(local)
    mb = size / 1024 / 1024
    print(f"  uploading {local.name} ({mb:.1f} MB) -> {remote}")
    t0 = time.time()
    result = subprocess.run(
        f"{SCP} -o StrictHostKeyChecking=no -o PasswordAuthentication=no \"{local}\" {HOST}:{remote}",
        shell=True, capture_output=True, text=True, timeout=600
    )
    elapsed = time.time() - t0
    if result.returncode != 0:
        print(f"  ERROR: {result.stderr[:300]}")
        raise SystemExit("scp failed")
    print(f"  done ({elapsed:.1f}s, {mb / elapsed:.2f} MB/s)")

def main():
    # 1. Prepare remote dirs
    print("=== Preparing remote dirs ===")
    run_ssh(f"mkdir -p {REMOTE_TMP} {REMOTE_SERVER}")

    # 2. Upload jars via scp
    print("\n=== Uploading JARs via scp ===")
    run_scp(LOCAL_JXC, f"{REMOTE_TMP}/jxc-0.1.jar")
    run_scp(LOCAL_JADMIN, f"{REMOTE_TMP}/jadmin-0.1.jar")

    # 3. Move jars to server dir
    print("\n=== Moving JARs ===")
    run_ssh(f"mv -f {REMOTE_TMP}/jxc-0.1.jar {REMOTE_SERVER}/jxc-0.1.jar && mv -f {REMOTE_TMP}/jadmin-0.1.jar {REMOTE_SERVER}/jadmin-0.1.jar && ls -lh {REMOTE_SERVER}/*.jar")

    # 4. Package & upload frontend
    print("\n=== Packaging merchant frontend ===")
    with tempfile.TemporaryDirectory() as td:
        mer_tar = Path(td) / "merchant-front.tar.gz"
        with tarfile.open(mer_tar, "w:gz") as tar:
            for p in LOCAL_MERCHANT_DIST.rglob("*"):
                if p.is_file():
                    tar.add(p, arcname=str(p.relative_to(LOCAL_MERCHANT_DIST)).replace("\\", "/"))
        ts = os.path.getsize(mer_tar) / 1024
        print(f"  tar: {ts:.0f} KB")
        run_scp(mer_tar, f"{REMOTE_TMP}/merchant-front.tar.gz")

    print("\n=== Deploying merchant frontend ===")
    run_ssh(
        f"rm -rf {REMOTE_MERCHANT_WEB}/* && tar -xzf {REMOTE_TMP}/merchant-front.tar.gz -C {REMOTE_MERCHANT_WEB} && chmod -R a+rX {REMOTE_MERCHANT_WEB} && ls {REMOTE_MERCHANT_WEB} | head -8",
        label="merchant-web"
    )

    print("\n=== Packaging admin frontend ===")
    with tempfile.TemporaryDirectory() as td:
        adm_tar = Path(td) / "admin-front.tar.gz"
        with tarfile.open(adm_tar, "w:gz") as tar:
            for p in LOCAL_ADMIN_DIST.rglob("*"):
                if p.is_file():
                    tar.add(p, arcname=str(p.relative_to(LOCAL_ADMIN_DIST)).replace("\\", "/"))
        ts = os.path.getsize(adm_tar) / 1024
        print(f"  tar: {ts:.0f} KB")
        run_scp(adm_tar, f"{REMOTE_TMP}/admin-front.tar.gz")

    print("\n=== Deploying admin frontend ===")
    run_ssh(
        f"rm -rf {REMOTE_ADMIN_WEB}/* && tar -xzf {REMOTE_TMP}/admin-front.tar.gz -C {REMOTE_ADMIN_WEB} && chmod -R a+rX {REMOTE_ADMIN_WEB} && ls {REMOTE_ADMIN_WEB} | head -8",
        label="admin-web"
    )

    # 5. Restart services
    print("\n=== Restarting services ===")
    run_ssh(f"cd {REMOTE_BASE} && docker compose up -d && docker restart jxc_merchant jxc_admin", label="restart")

    time.sleep(8)

    # 6. Check status
    print("\n=== Service status ===")
    run_ssh(
        "docker ps --filter name=jxc_ --format 'table {{.Names}}\\t{{.Status}}' && echo --- && "
        "docker logs jxc_merchant --tail 5 2>&1 && echo --- && "
        "docker logs jxc_admin --tail 5 2>&1",
        label="status"
    )

    print("\n=== DEPLOY COMPLETE ===")


if __name__ == "__main__":
    main()
