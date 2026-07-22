#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Safe mechanical cleanup: package blanks + move misplaced Tenant* imports."""
from __future__ import annotations

import re
from pathlib import Path

ROOTS = [
    Path("merchant/merchant-server/src/main/java/com/flyemu/share/controller"),
    Path("merchant/merchant-server/src/main/java/com/flyemu/share/service"),
    Path("admin/admin-server/src/main/java/com/flyemu/share/controller"),
    Path("admin/admin-server/src/main/java/com/flyemu/share/service"),
]

TENANT_RE = re.compile(
    r"^import com\.flyemu\.share\.common\.(TenantScope|TenantFilters|TenantAware);\r?\n",
    re.M,
)


def fix_package_blank(text: str) -> str:
    return re.sub(r"(package [^;]+;)\r?\n(?:\r?\n){2,}", r"\1\n\n", text)


def relocate_tenant_imports(text: str) -> str:
    found = TENANT_RE.findall(text)
    if not found:
        return text
    text2 = TENANT_RE.sub("", text)
    order = {"TenantAware": 0, "TenantFilters": 1, "TenantScope": 2}
    names = sorted(set(found), key=lambda n: order.get(n, 9))
    block = "".join(f"import com.flyemu.share.common.{n};\n" for n in names)

    m = list(re.finditer(r"^import com\.flyemu\.share\.annotation\.[^;]+;\r?\n", text2, re.M))
    if m:
        pos = m[-1].end()
        return text2[:pos] + block + text2[pos:]

    m2 = re.search(r"^import com\.flyemu\.share\.(?!common\.)", text2, re.M)
    if m2:
        return text2[: m2.start()] + block + text2[m2.start() :]

    m3 = list(re.finditer(r"^import (?:cn|jakarta|java)\.[^;]+;\r?\n", text2, re.M))
    if m3:
        pos = m3[-1].end()
        return text2[:pos] + block + text2[pos:]

    return re.sub(r"(package [^;]+;\r?\n\r?\n)", r"\1" + block, text2, count=1)


def process(path: Path) -> bool:
    orig = path.read_text(encoding="utf-8")
    text = fix_package_blank(orig)
    text = relocate_tenant_imports(text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    if text != orig:
        path.write_text(text, encoding="utf-8", newline="\n")
        return True
    return False


def main() -> None:
    n = 0
    for root in ROOTS:
        if not root.is_dir():
            continue
        for p in sorted(root.rglob("*.java")):
            if process(p):
                n += 1
    print(f"changed {n}")


if __name__ == "__main__":
    main()
