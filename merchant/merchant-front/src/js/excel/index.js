import {export_json_to_excel} from "@js/excel/export2Excel";
import {MessagePlugin} from "tdesign-vue-next";

/**
 * 带表头合并的通用 excel 导出
 */
export function exportExcelHeader(val, tHeader, headList, merges, initList, filename) {
    try {
        MessagePlugin.info("附件下载中，请勿重复点击~");
        const filterVal = headList.map(item => item.key);
        const data = val.map(v => filterVal.map(j => v[j]));
        const exportData = initList.concat(data);
        export_json_to_excel({
            header: tHeader,
            data: exportData,
            filename,
            autoWidth: true,
            merges,
            bookType: "xlsx"
        });
    } catch (error) {
        console.error(error);
        MessagePlugin.error("导出失败，请稍后重试");
    }
}
