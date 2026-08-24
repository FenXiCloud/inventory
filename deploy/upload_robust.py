"""
Upload jars with max socket timeout and optimized params.
"""
import os, sys, time, socket
import paramiko

HOST, USER, PASS = "112.124.55.97", "root", "@Flyemu891123"
REMOTE_TMP = "/data/jxc/tmp"

LOCAL = r"d:\Works\inventory\merchant\merchant-server\build\libs\jxc-0.1.jar"
REMOTE = f"{REMOTE_TMP}/jxc-0.1.jar"

size = os.path.getsize(LOCAL)
print(f"Uploading jxc-0.1.jar ({size / 1024 / 1024:.1f} MB)")

transport = paramiko.Transport((HOST, 22))
transport.connect(username=USER, password=PASS)
transport.sock.settimeout(600)  # 10 min socket timeout
# Increase window size
transport.window_size = 2147483647  # max window
transport.packetizer.REKEY_BYTES = pow(2, 40)  # avoid rekey during transfer
transport.packetizer.REKEY_PACKETS = pow(2, 40)

sftp = paramiko.SFTPClient.from_transport(transport)
# Set max packet size
sftp.MAX_REQUEST_SIZE = 1024 * 1024  # 1MB chunks

print("Starting upload...")
t0 = time.time()

def progress(t, total):
    pct = t * 100.0 / total
    mb = t / 1024 / 1024
    total_mb = total / 1024 / 1024
    print(f"\r  {mb:.1f}/{total_mb:.1f} MB ({pct:.1f}%)  ", end="", flush=True)

sftp.put(LOCAL, REMOTE, callback=progress)
print()

elapsed = time.time() - t0
print(f"Done! {elapsed:.1f}s, {size / 1024 / 1024 / elapsed:.2f} MB/s")

sftp.close()
transport.close()
