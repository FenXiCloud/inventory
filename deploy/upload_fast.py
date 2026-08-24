"""
Fast jar upload using SSH pipe (bypasses paramiko SFTP entirely).
"""
import subprocess, os, time, sys

SSH = r"C:\Windows\System32\OpenSSH\ssh.exe"
HOST = "root@112.124.55.97"
# Use sshpass equivalent - create a temp key-based approach or use the sshpass
# Since we don't have sshpass on Windows, use paramiko just for quick commands
# but use SSH stdin pipe for file transfer

REMOTE_TMP = "/data/jxc/tmp"
REMOTE_SERVER = "/data/jxc/server"

files = [
    (r"d:\Works\inventory\merchant\merchant-server\build\libs\jxc-0.1.jar", "jxc-0.1.jar"),
    (r"d:\Works\inventory\admin\admin-server\build\libs\jadmin-0.1.jar", "jadmin-0.1.jar"),
]

# Using paramiko for commands, but use a different transport for files
import paramiko

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect("112.124.55.97", username="root", password="@Flyemu891123", timeout=30)
print("Connected.")

# Ensure tmp dir exists
stdin, stdout, stderr = client.exec_command(f"mkdir -p {REMOTE_TMP}")
stdout.channel.recv_exit_status()

for local, name in files:
    size = os.path.getsize(local)
    size_mb = size / 1024 / 1024
    remote = f"{REMOTE_TMP}/{name}"
    print(f"\nUploading {name} ({size_mb:.1f} MB) to {remote}")
    t0 = time.time()
    
    # Use SFTPChannel with increased window size
    transport = client.get_transport()
    
    # Open SFTP session
    sftp = paramiko.SFTPClient.from_transport(transport)
    
    # Upload with file handle
    with open(local, 'rb') as f:
        print("  transferring...")
        # Set attributes on the SFTP file to use larger buffer
        sftp.putfo(f, remote)
    
    elapsed = time.time() - t0
    print(f"  Done! {elapsed:.1f}s, speed: {size_mb / elapsed:.2f} MB/s")
    sftp.close()

# Move to server dir
print("\nMoving jars...")
stdin, stdout, stderr = client.exec_command(
    f"mv -f {REMOTE_TMP}/jxc-0.1.jar {REMOTE_SERVER}/jxc-0.1.jar && "
    f"mv -f {REMOTE_TMP}/jadmin-0.1.jar {REMOTE_SERVER}/jadmin-0.1.jar && "
    f"ls -lh {REMOTE_SERVER}/*.jar"
)
code = stdout.channel.recv_exit_status()
print(stdout.read().decode().strip())
if code != 0:
    print(f"  exit={code}")

client.close()
print("\n=== JARS UPLOADED ===")
