import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.otherInbound?.id ? 'put' : 'post']('/otherInbound', param)
    },
    list(param) {
        return Ajax.get('/otherInbound', param)
    },
    remove(otherInboundId) {
        return Ajax.delete('/otherInbound/' + otherInboundId);
    },
    select(param) {
        return Ajax.get('/otherInbound/select', param)
    },
    load(id) {
        return Ajax.get("/otherInbound/load/" + id);
    },
    approved(state, ids) {
        return Ajax.post('/otherInbound/approved/' + state, ids);
    },
}
