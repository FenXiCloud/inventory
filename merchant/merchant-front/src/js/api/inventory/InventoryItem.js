/**
 * @功能描述: 库存明细/进销存报表
 */
import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/inventoryItem', param)
    },
    list(param) {
        return Ajax.get('/inventoryItem', param)
    },
    remove(inventoryItemId) {
        return Ajax.delete('/inventoryItem/' + inventoryItemId);
    },
    select(param) {
        return Ajax.get('/inventoryItem/select', param)
    },
    item(param) {
        return Ajax.get('/inventoryItem/item', param)
    },
    itemTotal(param) {
        return Ajax.get('/inventoryItem/itemTotal', param)
    },
    summary(param) {
        return Ajax.get('/inventoryItem/summary', param)
    },
    summaryByType(param) {
        return Ajax.get('/inventoryItem/summaryByType', param)
    },
    summaryInitial(param) {
        return Ajax.get('/inventoryItem/summaryInitial', param)
    },
    balance(param) {
        return Ajax.get('/inventoryItem/balance', param)
    },
};
