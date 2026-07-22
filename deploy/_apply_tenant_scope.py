#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Replace consecutive setMerchantId/setAccountBookId with TenantScope.bind."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "merchant/merchant-server/src/main/java/com/flyemu/share/controller"

# target.setMerchantId(account.xxx()); target.setAccountBookId(account.yyy());
# order either way
PAIR_RE = re.compile(
    r"(?P<indent>[ \t]*)(?P<target>\w+)\.setMerchantId\((?P<acc>\w+)\.getMerchantId\(\)\);\s*\n"
    r"(?P=indent)(?P=target)\.setAccountBookId\((?P=acc)\.getAccountBookId\(\)\);",
)
PAIR_RE_REV = re.compile(
    r"(?P<indent>[ \t]*)(?P<target>\w+)\.setAccountBookId\((?P<acc>\w+)\.getAccountBookId\(\)\);\s*\n"
    r"(?P=indent)(?P=target)\.setMerchantId\((?P=acc)\.getMerchantId\(\)\);",
)


def ensure_import(text: str) -> str:
    if "import com.flyemu.share.common.TenantScope;" in text:
        return text
    return re.sub(
        r"(package [^;]+;\s*)",
        r"\1\nimport com.flyemu.share.common.TenantScope;\n",
        text,
        count=1,
    )


def convert(text: str) -> str:
    def repl(m: re.Match) -> str:
        return (
            f"{m.group('indent')}TenantScope.bind({m.group('acc')}, "
            f"{m.group('target')}::setMerchantId, {m.group('target')}::setAccountBookId);"
        )

    new = PAIR_RE.sub(repl, text)
    new = PAIR_RE_REV.sub(repl, new)
    if new != text:
        new = ensure_import(new)
    return new


def main() -> None:
    changed = []
    for p in sorted(ROOT.rglob("*.java")):
        orig = p.read_text(encoding="utf-8")
        new = convert(orig)
        if new != orig:
            p.write_text(new, encoding="utf-8", newline="\n")
            changed.append(str(p.relative_to(ROOT)))
    print(f"changed {len(changed)}")
    for c in changed:
        print(c)


if __name__ == "__main__":
    main()
