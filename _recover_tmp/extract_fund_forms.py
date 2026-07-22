# -*- coding: utf-8 -*-
from pathlib import Path
import json

base = Path(r"C:\Users\Administrator\.cursor\projects\d-Idea-inventory\agent-transcripts\b5acd35b-a7fa-40ea-87cb-1a822968b5b3\subagents")

targets = {
    "OrderPaymentForm.vue": None,
    "OrderReceiptForm.vue": None,
    "VerificationForm.vue": None,
    "AccountTransferForm.vue": None,
    "OtherExpenseForm.vue": None,
    "OtherReceiptForm.vue": None,
}

for p in sorted(base.glob("*.jsonl")):
    text = p.read_text(encoding="utf-8", errors="replace")
    for line in text.splitlines():
        if "Write" not in line or "contents" not in line:
            continue
        try:
            obj = json.loads(line)
        except Exception:
            continue
        msg = obj.get("message", {})
        content = msg.get("content")
        if not isinstance(content, list):
            continue
        for part in content:
            if not isinstance(part, dict):
                continue
            if part.get("type") != "tool_use" or part.get("name") != "Write":
                continue
            inp = part.get("input") or {}
            path = (inp.get("path") or "").replace("\\", "/")
            contents = inp.get("contents") or ""
            for name in list(targets):
                if path.endswith("fund/" + name) and contents and "t-table" in contents and "vxe-table" not in contents:
                    targets[name] = contents
                    print(f"FOUND {name} in {p.name} len={len(contents)}")

out = Path(r"D:\Idea\inventory\_recover_tmp\fund_tdesign")
out.mkdir(parents=True, exist_ok=True)
for name, contents in targets.items():
    if contents:
        (out / name).write_text(contents, encoding="utf-8")
        print(f"WROTE {name}")
    else:
        print(f"MISSING {name}")
