# -*- coding: utf-8 -*-
from pathlib import Path

DST = Path(r"D:\Idea\inventory\merchant\merchant-front\src\views\fund")
lines = []
for name in [
    "AccountTransferForm.vue",
    "OrderPaymentForm.vue",
    "OrderReceiptForm.vue",
    "OtherExpenseForm.vue",
    "OtherReceiptForm.vue",
    "VerificationForm.vue",
]:
    t = (DST / name).read_text(encoding="utf-8")
    checks = {
        "no_vxe": "vxe-" not in t,
        "no_X_ROW_KEY": "_X_ROW_KEY" not in t,
        "has_rowKey": "_rowKey" in t,
        "has_newRow": "function newRow" in t,
        "has_t_table": "<t-table" in t,
        "has_form_toolbar": "form-toolbar" in t,
        "has_footData": "footData" in t,
        "has_chinese_date": "单据日期" in t,
        "has_chinese_save": "保存并新增" in t,
        "has_chinese_audit": "反审核" in t,
        "no_fffd": "\ufffd" not in t,
        "online_slot": ("#theOnlineTransactionNumber" in t)
        if name.startswith("Order")
        else True,
    }
    bad = [k for k, v in checks.items() if not v]
    status = "OK" if not bad else "FAIL " + ",".join(bad)
    lines.append(f"{name}: {status}")

report = "\n".join(lines)
Path(r"D:\Idea\inventory\_recover_tmp\fund_verify.txt").write_text(report, encoding="utf-8")
print(report)
