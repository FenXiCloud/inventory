import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/purchaseReturn', param)
    },
    list(param) {
        return Ajax.get('/purchaseReturn', param)
    },
    total(param) {
        return Ajax.get('/purchaseReturn/total', param)
    },
    remove(id) {
        return Ajax.delete('/purchaseReturn/' + id);
    },
    select(param) {
        return Ajax.get('/purchaseReturn/select', param)
    },
    approved(state, ids) {
        return Ajax.post('/purchaseReturn/approved/' + state, ids);
    },
    load(id) {
        return Ajax.get('/purchaseReturn/load/' + id);
    },
}
