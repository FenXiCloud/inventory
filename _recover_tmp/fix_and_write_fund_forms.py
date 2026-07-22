# -*- coding: utf-8 -*-
"""Fix recovered TDesign fund forms and write to merchant-front with UTF-8."""
from pathlib import Path
import re

SRC = Path(r"D:\Idea\inventory\_recover_tmp\fund_tdesign")
DST = Path(r"D:\Idea\inventory\merchant\merchant-front\src\views\fund")

ONLINE_SLOT = """
        <template #theOnlineTransactionNumber="{ row }">
          <t-input
            v-if="!isAudited"
            v-model="row.theOnlineTransactionNumber"
            placeholder="请输入"
          />
          <span v-else>{{ row.theOnlineTransactionNumber }}</span>
        </template>
"""

def fix_rowkey_strip(text: str) -> str:
    # save filterEmptyObjects: strip only _rowKey
    text = text.replace(
        ".map(({ _X_ROW_KEY, _rowKey, salesOrderId, salesOrderNo, ...rest }) => rest)",
        ".map(({ _rowKey, salesOrderId, salesOrderNo, ...rest }) => rest)",
    )
    text = text.replace(
        ".map(({ _X_ROW_KEY, _rowKey, ...rest }) => rest)",
        ".map(({ _rowKey, ...rest }) => rest)",
    )
    # source merge: drop _X_ROW_KEY delete lines; newRow already assigns fresh key
    text = re.sub(r"\n\s*delete item\._X_ROW_KEY;\n", "\n", text)
    # also remove leftover delete of _rowKey if any before newRow spread
    text = re.sub(r"\n\s*delete item\._rowKey;\n", "\n", text)
    return text


def ensure_online_slot(text: str) -> str:
    if "theOnlineTransactionNumber" not in text:
        return text
    if "#theOnlineTransactionNumber" in text:
        return text
    # Insert after #remarks slot in collection table (before closing </t-table> of first table)
    # Prefer inserting after the first remarks slot that is for collection rows
    marker = """        <template #remarks=\"{ row }\">
          <t-input v-if=\"!isAudited\" v-model=\"row.remarks\" />
          <span v-else>{{ row.remarks }}</span>
        </template>
      </t-table>"""
    if marker in text:
        return text.replace(
            marker,
            """        <template #remarks=\"{ row }\">
          <t-input v-if=\"!isAudited\" v-model=\"row.remarks\" />
          <span v-else>{{ row.remarks }}</span>
        </template>
""" + ONLINE_SLOT + "      </t-table>",
            1,
        )
    # Fallback: before first </t-table>
    return text.replace("</t-table>", ONLINE_SLOT + "      </t-table>", 1)


def ensure_foot_total_sync_account_transfer(text: str) -> str:
    """Ensure footData side-effect sync is not needed; updateFootEvent already used."""
    return text


def process(name: str) -> None:
    text = (SRC / name).read_text(encoding="utf-8")
    text = fix_rowkey_strip(text)
    if name in ("OrderPaymentForm.vue", "OrderReceiptForm.vue"):
        text = ensure_online_slot(text)
    # Final safety: no vxe, no _X_ROW_KEY
    if "vxe-" in text:
        raise SystemExit(f"{name}: still has vxe-")
    if "_X_ROW_KEY" in text:
        raise SystemExit(f"{name}: still mentions _X_ROW_KEY")
    if name in ("OrderPaymentForm.vue", "OrderReceiptForm.vue"):
        if "#theOnlineTransactionNumber" not in text:
            raise SystemExit(f"{name}: missing #theOnlineTransactionNumber slot")
    # Chinese sanity
    for label in ("保存", "取消", "备注"):
        if label not in text:
            raise SystemExit(f"{name}: missing Chinese label {label}")
    dest = DST / name
    dest.write_text(text, encoding="utf-8", newline="\n")
    print(f"OK {name} -> {dest} ({len(text)} bytes)")


for name in [
    "AccountTransferForm.vue",
    "OrderPaymentForm.vue",
    "OrderReceiptForm.vue",
    "OtherExpenseForm.vue",
    "OtherReceiptForm.vue",
    "VerificationForm.vue",
]:
    process(name)

print("DONE")
