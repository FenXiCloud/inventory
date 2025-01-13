import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/accountFlow', param)
    },
    list(param) {
        return Ajax.get('/accountFlow', param)
    },
    remove(id) {
        return Ajax.delete('/accountFlow/' + id);
    },
    select(param) {
        return Ajax.get('/accountFlow/select', param)
    }
}
