# -*- coding: utf-8 -*-
"""Deploy jars + restart + run product name ALTER on production."""
from __future__ import annotations

import paramiko

from deploy_remote import (
    HOST,
    LOCAL_ADMIN_JAR,
    LOCAL_MERCHANT_JAR,
    PASSWORD,
    REMOTE_SERVER,
    REMOTE_TMP,
    USER,
    require_file,
    sftp_put,
    ssh_run,
)

DB_CONTAINER = "contract_mysql8"
DB = "jxc"
ALTER_SQL = "ALTER TABLE jxc_product MODIFY COLUMN name VARCHAR(256) NOT NULL COMMENT '名称';"
SHOW_SQL = "SHOW COLUMNS FROM jxc_product LIKE 'name';"


def main() -> None:
    require_file(LOCAL_MERCHANT_JAR, "merchant jar")
    require_file(LOCAL_ADMIN_JAR, "admin jar")

    print(f"deploy backend -> {USER}@{HOST}")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, username=USER, password=PASSWORD, timeout=30)
    sftp = client.open_sftp()
    try:
        ssh_run(client, f"mkdir -p {REMOTE_SERVER} {REMOTE_TMP}")

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

        print("=== run ALTER product name ===")
        sql_cmd = (
            f"docker exec {DB_CONTAINER} mysql -uroot -p'{PASSWORD}' {DB} "
            f"--default-character-set=utf8mb4 -e \"{ALTER_SQL} {SHOW_SQL}\""
        )
        ssh_run(client, sql_cmd, timeout=60)

        print("=== restart backends ===")
        ssh_run(client, "docker restart jxc_merchant jxc_admin", timeout=300)
        ssh_run(
            client,
            "sleep 25; docker ps --filter name=jxc_ --format 'table {{.Names}}\\t{{.Status}}'; "
            "docker logs jxc_merchant 2>&1 | grep -E 'Started MerchantApplication|APPLICATION FAILED' | tail -2; "
            "docker logs jxc_admin 2>&1 | grep -E 'Started AdminApplication|APPLICATION FAILED' | tail -2",
            timeout=180,
        )
    finally:
        sftp.close()
        client.close()
    print("backend deploy done")


if __name__ == "__main__":
    main()
