import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.salesOrder?.id ? 'put' : 'post']('/salesOrder', param)
    },
    list(param) {
        return Ajax.get('/salesOrder', param)
    },
    listToOutBound(param) {
        return Ajax.get('/salesOrder/toOutBound', param)
    },
    toOutbound(customerId, ids) {
        return Ajax.post('/salesOrder/toOutbound/' + customerId, ids);
    },
    total(param) {
        return Ajax.get('/salesOrder/total', param)
    },
    remove(id) {
        return Ajax.delete('/salesOrder/' + id);
    },
    select(param) {
        return Ajax.get('/salesOrder/select', param)
    },
    load(id) {
        return Ajax.get('/salesOrder/load/' + id);
    },
    approved(state, ids) {
        return Ajax.post('/salesOrder/approved/' + state, ids);
    },
    transferToPurchaseInbound(salesOrderId, data) {
        return Ajax.post('/salesOrder/transferToPurchaseInbound/' + salesOrderId, data);
    },
    /** 以销定购看板 - 待采购列表 */
    pendingPurchase(param) {
        return Ajax.get('/salesOrder/pendingPurchase', param);
    },
    /** 批量转采购入库单 */
    batchTransferToPurchaseInbound(data) {
        return Ajax.post('/salesOrder/batchTransferToPurchaseInbound', data);
    },
    /** 批量设置供应商 */
    batchSetSupplier(supplierId, productIds) {
        return Ajax.post('/salesOrder/batchSetSupplier?supplierId=' + supplierId, productIds);
    },
    /** 订单追踪 */
    track(salesOrderId) {
        return Ajax.get('/salesOrder/track/' + salesOrderId);
    },
    importData(formData) {
        return Ajax.post('/salesOrder/importData', formData, {'Content-Type': 'multipart/form-data', repeatable: true});
    },
    /* ========== 以销定购工作台（/purchase/to-order） ========== */
    /** 获取以销定购建议列表（订单级） */
    toOrderList(param) {
        return Ajax.post('/purchase/to-order/list', param);
    },
    /** 生成采购入库单（按供应商自动拆分） */
    toOrderCreatePurchaseIn(data) {
        return Ajax.post('/purchase/to-order/create-purchase-in', data);
    },
    /** 以销定购订单追踪 */
    toOrderTrack(saleOrderId) {
        return Ajax.get('/purchase/to-order/track/' + saleOrderId);
    },
    /** 批量设置供应商 */
    toOrderBatchSetSupplier(supplierId, productIds) {
        return Ajax.post('/purchase/to-order/batch-set-supplier?supplierId=' + supplierId, productIds);
    },
    /** 以销定购系统参数 */
    toOrderParams() {
        return Ajax.get('/purchase/to-order/params');
    },
}
