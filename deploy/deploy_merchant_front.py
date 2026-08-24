# -*- coding: utf-8 -*-
"""Deploy merchant-front only."""
from __future__ import annotations

import paramiko

from deploy_remote import (
    HOST,
    LOCAL_MERCHANT_DIST,
    PASSWORD,
    REMOTE_MERCHANT_WEB,
    REMOTE_TMP,
    USER,
    deploy_frontend,
    require_dir,
    ssh_run,
)


def main() -> None:
    require_dir(LOCAL_MERCHANT_DIST, "merchant dist")
    print(f"deploy merchant-front -> {USER}@{HOST}")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, username=USER, password=PASSWORD, timeout=30)
    sftp = client.open_sftp()
    try:
        ssh_run(client, f"mkdir -p {REMOTE_TMP}")
        deploy_frontend(client, sftp, LOCAL_MERCHANT_DIST, "merchant-front.tar.gz", REMOTE_MERCHANT_WEB)
    finally:
        sftp.close()
        client.close()
    print("merchant-front deploy done")


if __name__ == "__main__":
    main()
