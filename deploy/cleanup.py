import os, time, paramiko

HOST, USER, PASS = "112.124.55.97", "root", "@Flyemu891123"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username=USER, password=PASS, timeout=30)
print("Connected.")

# Clean up tmp
stdin, stdout, stderr = client.exec_command("rm -f /data/jxc/tmp/jxc-0.1.jar /data/jxc/tmp/jadmin-0.1.jar /data/jxc/tmp/*.tar.gz && echo 'cleaned'")
print(stdout.read().decode().strip())

# Check disk space
stdin, stdout, stderr = client.exec_command("df -h /data")
print(stdout.read().decode().strip())

# Check existing jars
stdin, stdout, stderr = client.exec_command("ls -lh /data/jxc/server/*.jar 2>/dev/null || echo 'no jars'")
print(stdout.read().decode().strip())

client.close()
