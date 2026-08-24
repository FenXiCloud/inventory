import os
import sys
print('hello')
print('PATH:', os.environ.get('PATH', '')[:500])
print('python:', sys.executable)
print('cwd:', os.getcwd())
