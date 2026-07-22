import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/supplierInitial', param)
    },
    list(param) {
        return Ajax.get('/supplierInitial', param)
    },
    remove(supplierInitialId) {
        return Ajax.delete('/supplierInitial/' + supplierInitialId);
    },
    select(param) {
        return Ajax.get('/supplierInitial/select', param)
    },
    batch(param) {
        return Ajax.post('/supplierInitial/batch', param)
    },
    load(id) {
        return Ajax.get("/supplierInitial/load/" + id);
    },
    batchDelete(param) {
        return Ajax.put('/supplierInitial/batchDelete', param)
    },
}
