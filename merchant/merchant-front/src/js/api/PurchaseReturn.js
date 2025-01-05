import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/purchaseReturn', param)
    },
    list(param) {
        return Ajax.get('/purchaseReturn', param)
    },
    remove(id) {
        return Ajax.delete('/purchaseReturn/' + id);
    },
    select(param) {
        return Ajax.get('/purchaseReturn/select', param)
    }
}
