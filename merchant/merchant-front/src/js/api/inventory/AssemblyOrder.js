import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.assemblyOrder?.id ? 'put' : 'post']('/assemblyOrder', param)
    },
    list(param) {
        return Ajax.get('/assemblyOrder', param)
    },
    remove(id) {
        return Ajax.delete('/assemblyOrder/' + id);
    },
    load(id) {
        return Ajax.get("/assemblyOrder/load/" + id);
    },
    approved(state, ids) {
        return Ajax.post('/assemblyOrder/approved/' + state, ids);
    },
}
