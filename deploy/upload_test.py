import os, sys, time, paramiko

HOST, USER, PASS = "112.124.55.97", "root", "@Flyemu891123"
LOCAL = r"d:\Works\inventory\merchant\merchant-server\build\libs\jxc-0.1.jar"
REMOTE = "/data/jxc/tmp/jxc-0.1.jar"

size = os.path.getsize(LOCAL)
print(f"File: {LOCAL}")
print(f"Size: {size / 1024 / 1024:.1f} MB")
print(f"Connecting to {USER}@{HOST}...")

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username=USER, password=PASS, timeout=30)
print("Connected.")

sftp = client.open_sftp()

print(f"Uploading...")
t0 = time.time()

# upload with progress callback
def cb(transferred, total):
    pct = transferred * 100.0 / total
    print(f"\r  {transferred / 1024 / 1024:.1f}/{total / 1024 / 1024:.1f} MB ({pct:.1f}%)", end="", flush=True)

sftp.put(LOCAL, REMOTE, callback=cb)
print()  
elapsed = time.time() - t0
print(f"Done! {elapsed:.1f}s, {size / 1024 / 1024 / elapsed:.2f} MB/s")

sftp.close()
client.close()
