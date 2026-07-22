import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.otherOutbound?.id ? "put" : "post"]("/otherOutbound", param);
    },
    list(param) {
        return Ajax.get("/otherOutbound", param);
    },
    remove(otherOutboundId) {
        return Ajax.delete("/otherOutbound/" + otherOutboundId);
    },
    select(param) {
        return Ajax.get("/otherOutbound/select", param);
    },
    load(id) {
        return Ajax.get("/otherOutbound/load/" + id);
    },
    approved(state, ids) {
        return Ajax.post("/otherOutbound/approved/" + state, ids);
    },
};
