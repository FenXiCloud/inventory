import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/purchaseInbound', param)
    },
    list(param) {
        return Ajax.get('/purchaseInbound', param)
    },
    remove(id) {
        return Ajax.delete('/purchaseInbound/' + id);
    },
    select(param) {
        return Ajax.get('/purchaseInbound/select', param)
    }
}
