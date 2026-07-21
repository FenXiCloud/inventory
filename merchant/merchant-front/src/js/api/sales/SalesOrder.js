import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.salesOrder?.id ? 'put' : 'post']('/salesOrder', param)
    },
    list(param) {
        return Ajax.get('/salesOrder', param)
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
}
