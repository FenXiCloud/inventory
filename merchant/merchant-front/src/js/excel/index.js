import {export_json_to_excel} from "@js/excel/export2Excel";
import {MessagePlugin} from "tdesign-vue-next";

/**
 * 通用excel导出方法
 * @param val
 * @param headList
 * @param filename
 */
export function exportExcel(val, headList, filename) {
    try {
        message("附件下载中，请勿重复点击~");
        const tHeader = headList.map(item => item.label); // 表格头部
        const filterVal = headList.map(item => item.key); // 展示数据里哪些字
        const data = val.map(v => filterVal.map(j => v[j]));
        // 文件数据处理完成后自动下载
        export_json_to_excel({
            header: tHeader, // 表头
            data,
            filename, // 文件名称
            autoWidth: true,
            bookType: "xlsx" // 文件后缀名
        });
    } catch (error) {
    }
}

/**
 * 通用excel导出方法
 * @param val
 * @param tHeader
 * @param headList
 * @param merges
 * @param initList
 * @param filename
 */
export function exportExcelHeader(val, tHeader, headList, merges, initList, filename) {
    try {
        message("附件下载中，请勿重复点击~");
        const filterVal = headList.map(item => item.key); // 展示数据里哪些字
        const data = val.map(v => filterVal.map(j => v[j]));
        const exportData = initList.concat(data);
        // 文件数据处理完成后自动下载
        export_json_to_excel({
            header: tHeader, // 表头
            data: exportData,
            filename, // 文件名称
            autoWidth: true,
            merges,
            bookType: "xlsx" // 文件后缀名
        });
    } catch (error) {
    }
}