import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.stockTake?.id ? 'put' : 'post']('/stockTake', param)
    },
    list(param) {
        return Ajax.get('/stockTake', param)
    },
    remove(stockTakeId) {
        return Ajax.delete('/stockTake/' + stockTakeId);
    },
    select(param) {
        return Ajax.get('/stockTake/select', param)
    },
    load(id) {
        return Ajax.get("/stockTake/load/" + id);
    },
    approved(state, ids) {
        return Ajax.post('/stockTake/approved/' + state, ids);
    },
    export(id) {
        return Ajax.get("/stockTake/export/" + id);
    },
}
