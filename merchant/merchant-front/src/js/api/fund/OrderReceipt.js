import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/orderReceipt', param)
    },
    list(param) {
        return Ajax.get('/orderReceipt', param)
    },
    remove(id) {
        return Ajax.delete('/orderReceipt/' + id);
    },
    select(param) {
        return Ajax.get('/orderReceipt/select', param)
    }
}
