import Ajax from "@common/Request";

export default {
    list() {
        return Ajax.get('/financeItemMap')
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