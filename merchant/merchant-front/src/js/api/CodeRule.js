import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/codeRule', param)
    },
    list(param) {
        return Ajax.get('/codeRule', param)
    },
    remove(id) {
        return Ajax.delete('/codeRule/' + id);
    },
    select(param) {
        return Ajax.get('/codeRule/select', param)
    }
}
