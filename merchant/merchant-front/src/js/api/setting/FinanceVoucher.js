import Ajax from "@common/Request";

export default {
    list(param) {
        return Ajax.get('/financeVoucher', param)
    },
    save(param) {
        return Ajax.post('/financeVoucher/save', param)
    },
    delete(id) {
        return Ajax.delete("/financeVoucher/" + id);
    },
    load(id) {
        return Ajax.get("/financeVoucher/load/" + id);
    },
}