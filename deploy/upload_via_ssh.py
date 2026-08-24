"""
Upload jars via SSH pipe (more reliable than SFTP for large files).
"""
import os, sys, time, paramiko, base64

HOST, USER, PASS = "112.124.55.97", "root", "@Flyemu891123"
REMOTE_SERVER = "/data/jxc/server"
REMOTE_TMP = "/data/jxc/tmp"

FILES = [
    (r"d:\Works\inventory\merchant\merchant-server\build\libs\jxc-0.1.jar", "jxc-0.1.jar"),
    (r"d:\Works\inventory\admin\admin-server\build\libs\jadmin-0.1.jar", "jadmin-0.1.jar"),
]

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username=USER, password=PASS, timeout=30)
print("Connected.")

# Ensure tmp dir
stdin, stdout, stderr = client.exec_command(f"mkdir -p {REMOTE_TMP}")
stdout.channel.recv_exit_status()

for local_path, name in FILES:
    size = os.path.getsize(local_path)
    size_mb = size / 1024 / 1024
    remote_path = f"{REMOTE_TMP}/{name}"  # Using -- apparently _ is special in exec_command? No it's not
    
    print(f"\nUploading {name} ({size_mb:.1f} MB)...")
    t0 = time.time()
    
    # Read and upload via SFTP with a small trick - use putfo with file object
    # but paramiko automatically handles large files
    sftp = client.open_sftp()
    
    # Use put with a buffer  
    with open(local_path, 'rb') as f:
        sftp.putfo(f, remote_path.replace("-", "-"), callback=None)
    
    elapsed = time.time() - t0
    print(f"  Done! {elapsed:.1f}s ({size_mb / elapsed:.2f} MB/s)")
    sftp.close()

# Move to server dir
print("\nMoving jars...")
stdin, stdout, stderr = client.exec_command(
    f"mv -f {REMOTE_TMP}/jxc-0.1.jar {REMOTE_SERVER}/jxc-0.1.jar && "
    f"mv -f {REMOTE_TMP}/jadmin-0.1.jar {REMOTE_SERVER}/jadmin-0.1.jar && "
    f"ls -lh {REMOTE_SERVER}/*.jar"
)
print(stdout.read().decode().strip())

client.close()
print("\nJAR UPLOAD COMPLETE")
