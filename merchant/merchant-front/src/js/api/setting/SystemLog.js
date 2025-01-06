import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/systemLog', param)
    },
    list(param) {
        return Ajax.get('/systemLog', param)
    },
    remove(id) {
        return Ajax.delete('/systemLog/' + id);
    },
    select(param) {
        return Ajax.get('/systemLog/select', param)
    }
}
