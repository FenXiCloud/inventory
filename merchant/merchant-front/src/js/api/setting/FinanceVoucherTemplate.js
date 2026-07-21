import Ajax from "@common/Request";

export default {
    list() {
        return Ajax.get('/financeVoucherTemplate')
    },
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/financeVoucherTemplate', param)
    },
    delete(id) {
        return Ajax.delete("/financeVoucherTemplate/" + id);
    },
    load(id) {
        return Ajax.get("/financeVoucherTemplate/load/" + id);
    },
}
