import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/salesReturn', param)
    },
    list(param) {
        return Ajax.get('/salesReturn', param)
    },
    remove(id) {
        return Ajax.delete('/salesReturn/' + id);
    },
    select(param) {
        return Ajax.get('/salesReturn/select', param)
    }
}
