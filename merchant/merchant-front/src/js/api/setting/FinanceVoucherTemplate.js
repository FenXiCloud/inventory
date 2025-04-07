import Ajax from "@common/Request";

export default {
    list() {
        return Ajax.get('/financeVoucherTemplate')
    },
    save(param) {
        return Ajax.post('/financeVoucherTemplate/save', param)
    },
    delete(id) {
        return Ajax.delete("/financeVoucherTemplate/" + id);
    },
    load(id) {
        return Ajax.get("/financeVoucherTemplate/load/" + id);
    },
}