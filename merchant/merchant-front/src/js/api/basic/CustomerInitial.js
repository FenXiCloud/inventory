import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/customerInitial', param)
    },
    list(param) {
        return Ajax.get('/customerInitial', param)
    },
    remove(customerInitialId) {
        return Ajax.delete('/customerInitial/' + customerInitialId);
    },
    select(param) {
        return Ajax.get('/customerInitial/select', param)
    },
    batch(param) {
        return Ajax.post('/customerInitial/batch', param)
    },
    load(id) {
        return Ajax.get("/customerInitial/load/" + id);
    },
    batchDelete(param) {
        return Ajax.put('/customerInitial/batchDelete', param)
    },
}
