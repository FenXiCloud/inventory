import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/verification', param)
    },
    list(param) {
        return Ajax.get('/verification', param)
    },
    remove(id) {
        return Ajax.delete('/verification/' + id);
    },
    select(param) {
        return Ajax.get('/verification/select', param)
    }
}
