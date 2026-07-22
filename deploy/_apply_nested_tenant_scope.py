#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Apply TenantScope.bind to nested getX().setMerchantId/setAccountBookId pairs."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "merchant/merchant-server/src/main/java/com/flyemu/share/controller"

# expr.getFoo().setMerchantId(acc.getMerchantId());
# expr.getFoo().setAccountBookId(acc.getAccountBookId());
PAIR = re.compile(
    r"(?P<indent>[ \t]*)(?P<expr>\w+(?:\.\w+\(\))*)\.setMerchantId\((?P<acc>\w+)\.getMerchantId\(\)\);\s*\n"
    r"(?P=indent)(?P=expr)\.setAccountBookId\((?P=acc)\.getAccountBookId\(\)\);",
)
PAIR_REV = re.compile(
    r"(?P<indent>[ \t]*)(?P<expr>\w+(?:\.\w+\(\))*)\.setAccountBookId\((?P<acc>\w+)\.getAccountBookId\(\)\);\s*\n"
    r"(?P=indent)(?P=expr)\.setMerchantId\((?P=acc)\.getMerchantId\(\)\);",
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
        expr, acc, indent = m.group("expr"), m.group("acc"), m.group("indent")
        # simple identifier -> method ref; nested getX() -> extract local via double call (acceptable)
        if "(" not in expr:
            return f"{indent}TenantScope.bind({acc}, {expr}::setMerchantId, {expr}::setAccountBookId);"
        return (
            f"{indent}TenantScope.bind({acc}, {expr}::setMerchantId, {expr}::setAccountBookId);"
        )

    new = PAIR.sub(repl, text)
    new = PAIR_REV.sub(repl, new)
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
