#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unify controller tenant params to @SaAccountVal AccountDto."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "merchant/merchant-server/src/main/java/com/flyemu/share/controller"

PARAM_PAIR = re.compile(
    r"(?:,\s*)?@SaMerchantId\s+Long\s+merchantId\s*,\s*@SaAccountBookId\s+Long\s+accountBookId"
    r"|(?:,\s*)?@SaAccountBookId\s+Long\s+accountBookId\s*,\s*@SaMerchantId\s+Long\s+merchantId"
)
ACCOUNT_PARAM = re.compile(r"@SaAccountVal\s+AccountDto\s+(\w+)")
METHOD_RE = re.compile(
    r"((?:@\w+(?:\([^;]*?\))?\s+)*public\s+(?:JsonResult|ResponseEntity(?:\s*<[^>]+>)?)\s+\w+\s*\()([^{]*)(\)\s*\{)",
    re.MULTILINE | re.DOTALL,
)


def ensure_imports(text: str) -> str:
    if "import com.flyemu.share.annotation.SaAccountVal;" not in text:
        text = re.sub(
            r"(package [^;]+;\s*)",
            r"\1\nimport com.flyemu.share.annotation.SaAccountVal;\n",
            text,
            count=1,
        )
    if "import com.flyemu.share.dto.AccountDto;" not in text:
        text = re.sub(
            r"(package [^;]+;\s*)",
            r"\1\nimport com.flyemu.share.dto.AccountDto;\n",
            text,
            count=1,
        )
    if "@SaMerchantId" not in text:
        text = re.sub(r"\nimport com\.flyemu\.share\.annotation\.SaMerchantId;\r?\n", "\n", text)
    if "@SaAccountBookId" not in text:
        text = re.sub(r"\nimport com\.flyemu\.share\.annotation\.SaAccountBookId;\r?\n", "\n", text)
    return text


def replace_body_ids(body: str, account_name: str, preserve: set[str] | None = None) -> str:
    preserve = preserve or set()

    def repl_m(m: re.Match) -> str:
        if "merchantId" in preserve:
            return m.group(0)
        return f"{account_name}.getMerchantId()"

    def repl_b(m: re.Match) -> str:
        if "accountBookId" in preserve:
            return m.group(0)
        return f"{account_name}.getAccountBookId()"

    body = re.sub(r"(?<![.\w])merchantId(?!\s*\()", repl_m, body)
    body = re.sub(r"(?<![.\w])accountBookId(?!\s*\()", repl_b, body)
    return body


def convert_file(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    orig = text
    if "@SaMerchantId" not in text and "@SaAccountBookId" not in text:
        return False

    def process_match(m: re.Match) -> str:
        prefix, params, suffix = m.group(1), m.group(2), m.group(3)
        if "@SaMerchantId" not in params and "@SaAccountBookId" not in params:
            return m.group(0)

        existing = ACCOUNT_PARAM.search(params)
        account_name = existing.group(1) if existing else "accountDto"

        new_params = PARAM_PAIR.sub("", params)
        new_params = re.sub(r"(?:,\s*)?@SaMerchantId\s+Long\s+merchantId", "", new_params)
        new_params = re.sub(r"(?:,\s*)?@SaAccountBookId\s+Long\s+accountBookId", "", new_params)
        new_params = re.sub(r",\s*,", ",", new_params)
        new_params = re.sub(r",\s*$", "", new_params.strip())
        new_params = re.sub(r"^\s*,\s*", "", new_params)

        if not existing:
            if new_params.strip():
                new_params = new_params.rstrip() + ", @SaAccountVal AccountDto accountDto"
            else:
                new_params = "@SaAccountVal AccountDto accountDto"

        preserve = set()
        if re.search(r"@PathVariable\s+(?:Long|Integer)\s+merchantId", new_params):
            preserve.add("merchantId")
        if re.search(r"@PathVariable\s+(?:Long|Integer)\s+accountBookId", new_params):
            preserve.add("accountBookId")
        preserve_flag = ",".join(sorted(preserve)) if preserve else ""
        return prefix + new_params + suffix + f"/*__TENANT_BIND__:{account_name}:{preserve_flag}*/"

    text2 = METHOD_RE.sub(process_match, text)

    result: list[str] = []
    pos = 0
    marker_re = re.compile(r"/\*__TENANT_BIND__:(\w+):([^\*]*)\*/")
    while True:
        m = marker_re.search(text2, pos)
        if not m:
            result.append(text2[pos:])
            break
        account_name = m.group(1)
        preserve = {x for x in m.group(2).split(",") if x}
        result.append(text2[pos : m.start()])
        body_start = m.end()
        depth = 1
        j = body_start
        while j < len(text2) and depth > 0:
            ch = text2[j]
            if ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
            j += 1
        body = replace_body_ids(text2[body_start : j - 1], account_name, preserve)
        result.append(body)
        result.append("}")
        pos = j

    text2 = "".join(result)
    text2 = ensure_imports(text2)
    text2 = re.sub(r"\n{3,}", "\n\n", text2)

    if text2 != orig:
        path.write_text(text2, encoding="utf-8", newline="\n")
        return True
    return False


def main(packages: list[str]) -> None:
    changed: list[str] = []
    for pkg in packages:
        d = ROOT / pkg
        if not d.is_dir():
            print(f"skip missing {d}")
            continue
        for p in sorted(d.glob("*.java")):
            if convert_file(p):
                changed.append(str(p.relative_to(ROOT)))
    # also AppController at controller root if requested
    if "root" in packages:
        p = ROOT / "AppController.java"
        if p.exists() and convert_file(p):
            changed.append("AppController.java")
    print(f"changed {len(changed)}")
    for c in changed:
        print(c)


if __name__ == "__main__":
    pkgs = sys.argv[1:] or ["basic"]
    main(pkgs)
