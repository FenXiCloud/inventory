import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/inventoryInitial', param)
    },
    list(param) {
        return Ajax.get('/inventoryInitial', param)
    },
    remove(inventoryInitialId) {
        return Ajax.delete('/inventoryInitial/' + inventoryInitialId);
    },
    select(param) {
        return Ajax.get('/inventoryInitial/select', param)
    },
    batch(param) {
        return Ajax.post('/inventoryInitial/batch', param)
    },
    load(id) {
        return Ajax.get("/inventoryInitial/load/" + id);
    },
    batchDelete(param) {
        return Ajax.put('/inventoryInitial/batchDelete', param)
    },
}
