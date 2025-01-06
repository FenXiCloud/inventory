import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/orderPayment', param)
    },
    list(param) {
        return Ajax.get('/orderPayment', param)
    },
    remove(id) {
        return Ajax.delete('/orderPayment/' + id);
    },
    select(param) {
        return Ajax.get('/orderPayment/select', param)
    }
}
