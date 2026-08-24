import paramiko, sys

HOST, USER, PASS = "112.124.55.97", "root", "@Flyemu891123"
PUBKEY = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIPutmrvJ7Bg1rVhAQj9pgl5cvwJgXKuyG+KB/fjFP3UN administrator@LZ5"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username=USER, password=PASS, timeout=15)
print("Connected.")

cmd = f"mkdir -p ~/.ssh && grep -qxF '{PUBKEY}' ~/.ssh/authorized_keys 2>/dev/null || echo '{PUBKEY}' >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys && echo 'OK'"
stdin, stdout, stderr = client.exec_command(cmd)
print(stdout.read().decode().strip())
print(stderr.read().decode().strip())

client.close()
print("Done.")
