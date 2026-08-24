"""
Chunked deployment - split large files, upload in pieces, reassemble on server.
"""
import os, subprocess, tarfile, tempfile, time, hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SSH = r"C:\Windows\System32\OpenSSH\ssh.exe"
SCP = r"C:\Windows\System32\OpenSSH\scp.exe"

HOST = "root@112.124.55.97"
SSH_OPTS = "-o StrictHostKeyChecking=no -o PasswordAuthentication=no -o ServerAliveInterval=30 -o ServerAliveCountMax=3"
REMOTE_BASE = "/data/jxc"
REMOTE_TMP = f"{REMOTE_BASE}/tmp"
REMOTE_SERVER = f"{REMOTE_BASE}/server"
REMOTE_MERCHANT_WEB = "/data/contract/nginx/wwwroot/jxc.fenxi365.com"
REMOTE_ADMIN_WEB = "/data/contract/nginx/wwwroot/jadmin.fenxi365.com"

CHUNK_SIZE = 10 * 1024 * 1024  # 10 MB chunks
MAX_RETRIES = 5

def run_ssh(cmd, label="", timeout=300):
    full = f"{SSH} {SSH_OPTS} {HOST} \"{cmd}\""
    tag = f" [{label}]" if label else ""
    print(f"$ {full[:120]}...{tag}")
    result = subprocess.run(full, shell=True, capture_output=True, text=True, timeout=timeout)
    if result.stdout.strip():
        print(result.stdout.strip())
    if result.stderr.strip():
        print(result.stderr.strip()[:300])
    if result.returncode != 0:
        raise SystemExit(f"SSH failed ({result.returncode}): {result.stderr[:200]}")
    return result.stdout


def upload_chunked(local: Path, remote_name: str, remote_dir: str):
    """Upload a large file in chunks, reassemble on server."""
    size = os.path.getsize(local)
    mb = size / 1024 / 1024
    total_chunks = (size + CHUNK_SIZE - 1) // CHUNK_SIZE
    chunks_dir = f"{remote_dir}/._chunks_{remote_name}"
    final_path = f"{remote_dir}/{remote_name}"

    print(f"  {remote_name}: {mb:.1f} MB, {total_chunks} chunks of {CHUNK_SIZE / 1024 / 1024:.0f} MB")

    # Create chunks dir
    run_ssh(f"mkdir -p {chunks_dir}")

    # Calculate local hash for verification
    print("  computing hash...")
    sha256 = hashlib.sha256()
    with open(local, 'rb') as f:
        while True:
            data = f.read(8192)
            if not data:
                break
            sha256.update(data)
    local_hash = sha256.hexdigest()
    print(f"  local sha256: {local_hash[:16]}...")

    t0 = time.time()
    with open(local, 'rb') as f:
        for i in range(total_chunks):
            chunk_data = f.read(CHUNK_SIZE)
            chunk_name = f"chunk_{i:05d}"
            chunk_path = Path(str(local) + f".chunk_{i:05d}")

            # Write chunk to temp file
            with open(chunk_path, 'wb') as cf:
                cf.write(chunk_data)

            chunk_mb = len(chunk_data) / 1024 / 1024
            remote_chunk = f"{chunks_dir}/{chunk_name}"

            # Upload with retries
            success = False
            for attempt in range(MAX_RETRIES):
                try:
                    result = subprocess.run(
                        f"{SCP} -o StrictHostKeyChecking=no -o PasswordAuthentication=no -o ServerAliveInterval=30 "
                        f"\"{chunk_path}\" {HOST}:{remote_chunk}",
                        shell=True, capture_output=True, text=True, timeout=120
                    )
                    if result.returncode == 0:
                        success = True
                        break
                    print(f"    chunk {i+1}/{total_chunks} failed (attempt {attempt+1}): {result.stderr[:100]}")
                    time.sleep(3)
                except subprocess.TimeoutExpired:
                    print(f"    chunk {i+1}/{total_chunks} timeout (attempt {attempt+1})")
                    time.sleep(3)

            # Clean up temp file
            os.unlink(chunk_path)

            if not success:
                raise SystemExit(f"Failed to upload chunk {i+1}/{total_chunks} after {MAX_RETRIES} attempts")

            pct = (i + 1) * 100 / total_chunks
            elapsed = time.time() - t0
            speed = (i + 1) * CHUNK_SIZE / 1024 / 1024 / elapsed if elapsed > 0 else 0
            print(f"  [{pct:.0f}%] chunk {i+1}/{total_chunks} ({chunk_mb:.1f} MB) OK  speed:{speed:.1f} MB/s")

    # Reassemble on server
    print(f"  reassembling...")
    run_ssh(
        f"cat {chunks_dir}/chunk_* > {final_path} && "
        f"sha256sum {final_path} && "
        f"rm -rf {chunks_dir}",
        timeout=120
    )

    elapsed = time.time() - t0
    print(f"  {remote_name} done! {elapsed:.1f}s total")


def deploy_frontend(dist: Path, remote_name: str, web_root: str):
    """Package frontend dist as tar.gz and deploy."""
    print(f"\n  Packaging {dist}...")
    with tempfile.TemporaryDirectory() as td:
        tar_path = Path(td) / remote_name
        with tarfile.open(tar_path, "w:gz") as tar:
            for p in dist.rglob("*"):
                if p.is_file():
                    tar.add(p, arcname=str(p.relative_to(dist)).replace("\\", "/"))
        # Upload as tar.gz
        upload_chunked(tar_path, remote_name, REMOTE_TMP)

    # Deploy
    print(f"  deploying frontend...")
    run_ssh(
        f"mkdir -p {web_root} && rm -rf {web_root}/* && "
        f"tar -xzf {REMOTE_TMP}/{remote_name} -C {web_root} && "
        f"chmod -R a+rX {web_root} && ls {web_root} | head -8",
        label="web"
    )


def main():
    print("=" * 60)
    print("CHUNKED DEPLOY")
    print("=" * 60)

    # 1. Prepare dirs
    print("\n[1/5] Preparing remote dirs...")
    run_ssh(f"mkdir -p {REMOTE_TMP} {REMOTE_SERVER} && rm -f {REMOTE_TMP}/*.jar {REMOTE_TMP}/*.tar.gz")

    # 2. Upload jars
    print("\n[2/5] Uploading JARs...")
    upload_chunked(
        ROOT / "merchant/merchant-server/build/libs/jxc-0.1.jar",
        "jxc-0.1.jar", REMOTE_TMP
    )
    upload_chunked(
        ROOT / "admin/admin-server/build/libs/jadmin-0.1.jar",
        "jadmin-0.1.jar", REMOTE_TMP
    )

    # 3. Move jars
    print("\n[3/5] Moving JARs to server dir...")
    run_ssh(
        f"mv -f {REMOTE_TMP}/jxc-0.1.jar {REMOTE_SERVER}/jxc-0.1.jar && "
        f"mv -f {REMOTE_TMP}/jadmin-0.1.jar {REMOTE_SERVER}/jadmin-0.1.jar && "
        f"ls -lh {REMOTE_SERVER}/*.jar"
    )

    # 4. Deploy frontends
    print("\n[4/5] Deploying frontends...")
    deploy_frontend(
        ROOT / "merchant/merchant-front/dist",
        "merchant-front.tar.gz", REMOTE_MERCHANT_WEB
    )
    deploy_frontend(
        ROOT / "admin/admin-front/dist",
        "admin-front.tar.gz", REMOTE_ADMIN_WEB
    )

    # 5. Restart services
    print("\n[5/5] Restarting services...")
    run_ssh(f"cd {REMOTE_BASE} && docker compose up -d && docker restart jxc_merchant jxc_admin", timeout=300)

    time.sleep(10)
    print("\n--- Service Status ---")
    run_ssh(
        "docker ps --filter name=jxc_ --format 'table {{.Names}}\\t{{.Status}}'",
        timeout=60
    )
    print("\n--- jxc_merchant logs ---")
    run_ssh("docker logs jxc_merchant --tail 10 2>&1", timeout=60)
    print("\n--- jxc_admin logs ---")
    run_ssh("docker logs jxc_admin --tail 10 2>&1", timeout=60)

    print("\n" + "=" * 60)
    print("DEPLOY COMPLETE!")
    print("=" * 60)


if __name__ == "__main__":
    main()
