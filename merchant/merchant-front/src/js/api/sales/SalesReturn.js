import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.salesReturn?.id ? 'put' : 'post']('/salesReturn', param)
    },
    list(param) {
        return Ajax.get('/salesReturn', param)
    },
    total(param) {
        return Ajax.get('/salesReturn/total', param)
    },
    remove(id) {
        return Ajax.delete('/salesReturn/' + id);
    },
    select(param) {
        return Ajax.get('/salesReturn/select', param)
    },
    load(id) {
        return Ajax.get('/salesReturn/load/' + id);
    },
    approved(state, ids) {
        return Ajax.post('/salesReturn/approved/' + state, ids);
    },
}
