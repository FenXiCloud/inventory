import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.inventoryTransfer?.id ? 'put' : 'post']('/inventoryTransfer', param)
    },
    list(param) {
        return Ajax.get('/inventoryTransfer', param)
    },
    remove(inventoryTransferId) {
        return Ajax.delete('/inventoryTransfer/' + inventoryTransferId);
    },
    select(param) {
        return Ajax.get('/inventoryTransfer/select', param)
    },
    load(id) {
        return Ajax.get("/inventoryTransfer/load/" + id);
    },
    approved(state, ids) {
        return Ajax.post('/inventoryTransfer/approved/' + state, ids);
    },
}
