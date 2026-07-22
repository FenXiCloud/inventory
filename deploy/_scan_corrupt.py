# -*- coding: utf-8 -*-
import re
from pathlib import Path

root = Path(r"D:/Idea/inventory/merchant/merchant-front/src/views")
files = [
    "fund/AccountTransferForm.vue",
    "fund/OrderPaymentForm.vue",
    "fund/OrderReceiptForm.vue",
    "fund/OtherExpenseForm.vue",
    "fund/OtherReceiptForm.vue",
    "fund/VerificationForm.vue",
    "purchase/PurchaseInboundForm.vue",
    "purchase/PurchaseOrderForm.vue",
    "purchase/PurchaseReturnForm.vue",
    "sales/SalesOrderForm.vue",
    "sales/SalesOutboundForm.vue",
    "sales/SalesReturnForm.vue",
]
out = Path(r"D:/Idea/inventory/deploy/_encoding_corrupt.txt")
lines = []
for f in files:
    p = root / f
    t = p.read_text(encoding="utf-8", errors="replace")
    for i, l in enumerate(t.splitlines(), 1):
        if "\ufffd" in l or "?/" in l or "?\"" in l or "?'" in l:
            lines.append(f"{f}:{i}:{l}")
out.write_text("\n".join(lines), encoding="utf-8")
print(f"wrote {len(lines)} lines")
