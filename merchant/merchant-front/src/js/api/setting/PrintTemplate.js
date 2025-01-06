import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/printTemplate', param)
    },
    list(param) {
        return Ajax.get('/printTemplate', param)
    },
    remove(id) {
        return Ajax.delete('/printTemplate/' + id);
    },
    select(param) {
        return Ajax.get('/printTemplate/select', param)
    }
}
