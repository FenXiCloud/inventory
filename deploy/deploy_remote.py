#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Deploy JXC to 112.124.55.97 without affecting contract_* stack."""
import os
import sys
import time
import tarfile
import tempfile
from pathlib import Path

import paramiko

HOST = "112.124.55.97"
USER = "root"
PASSWORD = "@Flyemu891123"

ROOT = Path(__file__).resolve().parent.parent
DEPLOY = Path(__file__).resolve().parent
REMOTE_BASE = "/data/jxc"
NGINX_VHOST = "/data/contract/nginx/vhost"
NGINX_WWW = "/data/contract/nginx/wwwroot"
NGINX_SSL = "/data/contract/nginx/ssl"


def ssh_connect():
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, username=USER, password=PASSWORD, timeout=30)
    return client


def run(client, cmd, timeout=180, check=True):
    print(f"\n$ {cmd}")
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    code = stdout.channel.recv_exit_status()
    if out:
        sys.stdout.write(out[:12000])
    if err:
        sys.stdout.write("[stderr] " + err[:4000] + "\n")
    if check and code != 0:
        raise RuntimeError(f"cmd failed ({code}): {cmd}")
    return code, out, err


def sftp_put_file(sftp, local_path, remote_path):
    print(f"  put {local_path} -> {remote_path}")
    sftp.put(str(local_path), remote_path)


def main():
    merchant_jar = ROOT / "merchant" / "merchant-server" / "build" / "libs" / "jxc-0.1.jar"
    admin_jar = ROOT / "admin" / "admin-server" / "build" / "libs" / "jadmin-0.1.jar"
    merchant_dist = ROOT / "merchant" / "merchant-front" / "dist"
    admin_dist = ROOT / "admin" / "admin-front" / "dist"

    missing = [p for p in (merchant_jar, admin_jar) if not p.exists()]
    if missing:
        print("Missing jars:", missing)
        sys.exit(1)
    for p in (merchant_dist, admin_dist):
        if not p.exists() or not any(p.iterdir()):
            print("Missing dist:", p)
            sys.exit(1)

    print("jar sizes:", merchant_jar.stat().st_size, admin_jar.stat().st_size)

    client = ssh_connect()
    sftp = client.open_sftp()

    run(client, f"mkdir -p {REMOTE_BASE}/server {REMOTE_BASE}/upload {REMOTE_BASE}/logs {REMOTE_BASE}/tmp")
    run(client, f"mkdir -p {NGINX_WWW}/jxc.fenxi365.com {NGINX_WWW}/jadmin.fenxi365.com")

    # isolated MySQL schema on shared instance
    sql = (
        "CREATE DATABASE IF NOT EXISTS jxc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; "
        "CREATE USER IF NOT EXISTS 'jxc'@'%' IDENTIFIED BY '@Flyemu891123'; "
        "GRANT ALL PRIVILEGES ON jxc.* TO 'jxc'@'%'; "
        "FLUSH PRIVILEGES;"
    )
    run(client, f"docker exec contract_mysql8 mysql -uroot -p'@Flyemu891123' -e \"{sql}\"", check=False)

    sftp_put_file(sftp, DEPLOY / "server" / "docker-compose.yml", f"{REMOTE_BASE}/docker-compose.yml")
    sftp_put_file(sftp, DEPLOY / "server" / "application-db.yml", f"{REMOTE_BASE}/server/application-db.yml")
    sftp_put_file(sftp, DEPLOY / "server" / "start.sh", f"{REMOTE_BASE}/start.sh")
    sftp_put_file(sftp, merchant_jar, f"{REMOTE_BASE}/server/jxc-0.1.jar")
    sftp_put_file(sftp, admin_jar, f"{REMOTE_BASE}/server/jadmin-0.1.jar")

    with tempfile.TemporaryDirectory() as td:
        td = Path(td)
        merchant_tar = td / "merchant-front.tar.gz"
        admin_tar = td / "admin-front.tar.gz"
        with tarfile.open(merchant_tar, "w:gz") as tar:
            tar.add(str(merchant_dist), arcname=".")
        with tarfile.open(admin_tar, "w:gz") as tar:
            tar.add(str(admin_dist), arcname=".")
        sftp_put_file(sftp, merchant_tar, f"{REMOTE_BASE}/tmp/merchant-front.tar.gz")
        sftp_put_file(sftp, admin_tar, f"{REMOTE_BASE}/tmp/admin-front.tar.gz")

    run(
        client,
        f"rm -rf {NGINX_WWW}/jxc.fenxi365.com/* && "
        f"tar -xzf {REMOTE_BASE}/tmp/merchant-front.tar.gz -C {NGINX_WWW}/jxc.fenxi365.com",
    )
    run(
        client,
        f"rm -rf {NGINX_WWW}/jadmin.fenxi365.com/* && "
        f"tar -xzf {REMOTE_BASE}/tmp/admin-front.tar.gz -C {NGINX_WWW}/jadmin.fenxi365.com",
    )

    # Start app containers first so nginx upstream hostnames resolve
    run(client, f"chmod +x {REMOTE_BASE}/start.sh")
    run(client, f"cd {REMOTE_BASE} && (docker compose down || docker-compose down || true)", check=False)
    code, out, err = run(client, f"cd {REMOTE_BASE} && docker compose up -d", timeout=300, check=False)
    if code != 0:
        run(client, f"cd {REMOTE_BASE} && docker-compose up -d", timeout=300)

    # HTTP nginx after containers exist (upstream jxc_merchant / jxc_admin)
    sftp_put_file(sftp, DEPLOY / "nginx" / "jxc.fenxi365.com.conf", f"{NGINX_VHOST}/jxc.fenxi365.com.conf")
    sftp_put_file(sftp, DEPLOY / "nginx" / "jadmin.fenxi365.com.conf", f"{NGINX_VHOST}/jadmin.fenxi365.com.conf")
    run(client, "docker exec contract_nginx nginx -t")
    run(client, "docker exec contract_nginx nginx -s reload")

    print("waiting for spring boot...")
    time.sleep(35)
    run(client, "docker ps --format 'table {{.Names}}\t{{.Status}}'", check=False)
    run(client, "docker logs --tail 50 jxc_merchant 2>&1", check=False)
    run(client, "docker logs --tail 50 jxc_admin 2>&1", check=False)

    # SSL
    for domain in ("jxc.fenxi365.com", "jadmin.fenxi365.com"):
        www = f"{NGINX_WWW}/{domain}"
        run(
            client,
            f"bash -lc '/root/.acme.sh/acme.sh --issue -d {domain} -w {www} --server letsencrypt --force'",
            timeout=240,
            check=False,
        )
        run(
            client,
            f"bash -lc \"/root/.acme.sh/acme.sh --install-cert -d {domain} "
            f"--key-file {NGINX_SSL}/{domain}.key "
            f"--fullchain-file {NGINX_SSL}/{domain}.crt "
            f"--reloadcmd 'docker exec contract_nginx nginx -s reload'\"",
            timeout=60,
            check=False,
        )

    # enable SSL conf only if certs exist
    code1, _, _ = run(client, f"test -f {NGINX_SSL}/jxc.fenxi365.com.crt", check=False)
    code2, _, _ = run(client, f"test -f {NGINX_SSL}/jadmin.fenxi365.com.crt", check=False)
    if code1 == 0 and code2 == 0:
        sftp_put_file(sftp, DEPLOY / "nginx" / "jxc.fenxi365.com.ssl.conf", f"{NGINX_VHOST}/jxc.fenxi365.com.conf")
        sftp_put_file(sftp, DEPLOY / "nginx" / "jadmin.fenxi365.com.ssl.conf", f"{NGINX_VHOST}/jadmin.fenxi365.com.conf")
        code, out, err = run(client, "docker exec contract_nginx nginx -t", check=False)
        if code == 0:
            run(client, "docker exec contract_nginx nginx -s reload")
        else:
            print("SSL nginx test failed, reverting to HTTP")
            sftp_put_file(sftp, DEPLOY / "nginx" / "jxc.fenxi365.com.conf", f"{NGINX_VHOST}/jxc.fenxi365.com.conf")
            sftp_put_file(sftp, DEPLOY / "nginx" / "jadmin.fenxi365.com.conf", f"{NGINX_VHOST}/jadmin.fenxi365.com.conf")
            run(client, "docker exec contract_nginx nginx -s reload")
    else:
        print("SSL certs not ready; HTTP-only for now")

    run(client, "docker ps --format 'table {{.Names}}\t{{.Status}}'", check=False)
    run(client, "curl -sI -H 'Host: admin.hangxinqian.com' http://127.0.0.1/ | head -8", check=False)
    run(client, "curl -sI -H 'Host: sign.hangxinqian.com' http://127.0.0.1/ | head -8", check=False)
    run(client, "curl -sI -H 'Host: jxc.fenxi365.com' http://127.0.0.1/ | head -12", check=False)
    run(client, "curl -sI -H 'Host: jadmin.fenxi365.com' http://127.0.0.1/ | head -12", check=False)
    run(client, "docker exec contract_nginx curl -s -o /dev/null -w '%{http_code}' http://jxc_merchant:8410/ ; echo", check=False)
    run(client, "docker exec contract_nginx curl -s -o /dev/null -w '%{http_code}' http://jxc_admin:8500/ ; echo", check=False)

    sftp.close()
    client.close()
    print("\nDONE")


if __name__ == "__main__":
    main()
