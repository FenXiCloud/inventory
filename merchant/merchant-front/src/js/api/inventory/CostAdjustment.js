import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.costAdjustment?.id ? 'put' : 'post']('/costAdjustment', param)
    },
    list(param) {
        return Ajax.get('/costAdjustment', param)
    },
    remove(costAdjustmentId) {
        return Ajax.delete('/costAdjustment/' + costAdjustmentId);
    },
    select(param) {
        return Ajax.get('/costAdjustment/select', param)
    },
    load(id) {
        return Ajax.get('/costAdjustment/load/' + id)
    },
    approved(state, ids) {
        return Ajax.post('/costAdjustment/approved/' + state, ids);
    }
}
