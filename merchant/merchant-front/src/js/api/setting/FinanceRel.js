import Ajax from "@common/Request";

export default {
    list() {
        return Ajax.get('/financeRel')
    },
    save(param) {
        return Ajax.post('/financeRel', param)
    },
    voucherList(param) {
        return Ajax.get('/relationVoucher', param)
    },
    loadVoucher(voucherId) {
        return Ajax.get('/relationVoucher/' + voucherId)
    },
    loadWord() {
        return Ajax.get('/relationVoucher/word')
    },
    loadCode(param) {
        return Ajax.get('/relationVoucher/code', param)
    },
    toVoucher(id) {
        return Ajax.put('/relationVoucher/toVoucher/' + id)
    },
    saveVoucher(id, param) {
        return Ajax.put('/relationVoucher/save/' + id, param)
    },

}