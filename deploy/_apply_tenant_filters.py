#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Rewrite standard Query setMerchantId/setAccountBookId to use TenantFilters."""
from __future__ import annotations

import re
from pathlib import Path

ROOTS = [
    Path(__file__).resolve().parents[1] / "merchant/merchant-server/src/main/java/com/flyemu/share/service",
    Path(__file__).resolve().parents[1] / "admin/admin-server/src/main/java/com/flyemu/share/service",
]

# Matches the common null-check + builder.and(qXxx.merchantId.eq(merchantId)) pattern
MERCHANT_SETTER = re.compile(
    r"(?P<indent>[ \t]*)public void setMerchantId\(Long merchantId\) \{\s*\n"
    r"(?P=indent)    if \(merchantId != null\) \{\s*\n"
    r"(?P=indent)        builder\.and\((?P<path>q\w+\.merchantId)\.eq\(merchantId\)\);\s*\n"
    r"(?P=indent)    \}\s*\n"
    r"(?P=indent)\}",
)
BOOK_SETTER = re.compile(
    r"(?P<indent>[ \t]*)public void setAccountBookId\(Long accountBookId\) \{\s*\n"
    r"(?P=indent)    if \(accountBookId != null\) \{\s*\n"
    r"(?P=indent)        builder\.and\((?P<path>q\w+\.accountBookId)\.eq\(accountBookId\)\);\s*\n"
    r"(?P=indent)    \}\s*\n"
    r"(?P=indent)\}",
)


def ensure_import(text: str) -> str:
    if "import com.flyemu.share.common.TenantFilters;" in text:
        return text
    return re.sub(
        r"(package [^;]+;\s*)",
        r"\1\nimport com.flyemu.share.common.TenantFilters;\n",
        text,
        count=1,
    )


def convert(text: str) -> tuple[str, int]:
    count = 0

    def repl_m(m: re.Match) -> str:
        nonlocal count
        count += 1
        return (
            f"{m.group('indent')}public void setMerchantId(Long merchantId) {{\n"
            f"{m.group('indent')}    TenantFilters.merchant(builder, {m.group('path')}, merchantId);\n"
            f"{m.group('indent')}}}"
        )

    def repl_b(m: re.Match) -> str:
        nonlocal count
        count += 1
        return (
            f"{m.group('indent')}public void setAccountBookId(Long accountBookId) {{\n"
            f"{m.group('indent')}    TenantFilters.accountBook(builder, {m.group('path')}, accountBookId);\n"
            f"{m.group('indent')}}}"
        )

    new = MERCHANT_SETTER.sub(repl_m, text)
    new = BOOK_SETTER.sub(repl_b, new)
    if count:
        new = ensure_import(new)
    return new, count


def main() -> None:
    files = 0
    setters = 0
    for root in ROOTS:
        if not root.is_dir():
            continue
        for p in sorted(root.rglob("*.java")):
            orig = p.read_text(encoding="utf-8")
            # skip complex SalesReport-style that also write whereClause
            if "whereClause" in orig and "setMerchantId" in orig:
                # still try — regex only matches simple builder-only bodies
                pass
            new, n = convert(orig)
            if n:
                p.write_text(new, encoding="utf-8", newline="\n")
                files += 1
                setters += n
                print(f"{p.relative_to(root.parents[5] if False else p.parents[6])}: {n}" if False else f"{p.name}: {n}")
    print(f"files={files} setters={setters}")


if __name__ == "__main__":
    main()
