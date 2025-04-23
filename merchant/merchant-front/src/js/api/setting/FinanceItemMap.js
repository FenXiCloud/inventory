import Ajax from "@common/Request";

export default {
    list(param) {
        return Ajax.get('/financeItemMap', param)
    },
    save(param) {
        return Ajax.post('/financeItemMap/save', param)
    },
    delete(id) {
        return Ajax.delete("/financeItemMap/" + id);
    },
    load(id) {
        return Ajax.get("/financeItemMap/load/" + id);
    },
}