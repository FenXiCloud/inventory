import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/systemConfig', param)
    },
    list(param) {
        return Ajax.get('/systemConfig', param)
    },
    remove(id) {
        return Ajax.delete('/systemConfig/' + id);
    },
    select(param) {
        return Ajax.get('/systemConfig/select', param)
    }
}
