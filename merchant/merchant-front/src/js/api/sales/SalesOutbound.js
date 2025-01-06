import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/salesOutbound', param)
    },
    list(param) {
        return Ajax.get('/salesOutbound', param)
    },
    remove(id) {
        return Ajax.delete('/salesOutbound/' + id);
    },
    select(param) {
        return Ajax.get('/salesOutbound/select', param)
    }
}
