import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/purchaseOrder', param)
    },
    list(param) {
        return Ajax.get('/purchaseOrder', param)
    },
    remove(id) {
        return Ajax.delete('/purchaseOrder/' + id);
    },
    select(param) {
        return Ajax.get('/purchaseOrder/select', param)
    },
    load(id) {
        return Ajax.get('/purchaseOrder/load/' + id);
    },
}
