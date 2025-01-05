import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/salesOrder', param)
    },
    list(param) {
        return Ajax.get('/salesOrder', param)
    },
    remove(id) {
        return Ajax.delete('/salesOrder/' + id);
    },
    select(param) {
        return Ajax.get('/salesOrder/select', param)
    }
}
