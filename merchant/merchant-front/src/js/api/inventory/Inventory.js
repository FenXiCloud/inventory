
/** 库存余额表 */
import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/inventory', param)
    },
    list(param) {
        return Ajax.get('/inventory', param)
    },
    remove(inventoryId) {
        return Ajax.delete('/inventory/' + inventoryId);
    },
    select(param) {
        return Ajax.get('/inventory/select', param)
    },
    products(param) {
        return Ajax.get("/inventory/products", param);
    },
    balance(param) {
        return Ajax.get("/inventory/balance", param);
    },
    balanceTotal(param) {
        return Ajax.get("/inventory/balanceTotal", param);
    },
    totalCost(productId, warehouseId) {
        return Ajax.get(`/inventory/totalCost/${productId}/${warehouseId}`);
    },
}
