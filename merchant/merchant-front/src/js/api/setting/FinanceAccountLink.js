import Ajax from "@common/Request";

export default {
    list() {
        return Ajax.get('/financeAccountLink')
    },
    save(param) {
        return Ajax.post('/financeAccountLink/save', param)
    },
    loadAccountSetsList(param) {
        return Ajax.post('/financeAccountLink/loadAccountSetsList', param)
    },
    loadByAccountBookId(param) {
        return Ajax.get(`/financeAccountLink/loadByAccountBookId/${param}`)
    },
    load(param) {
        return Ajax.get(`/financeAccountLink/load/${param}`)
    },
    loadVoucherWord() {
        return Ajax.get(`/financeAccountLink/loadVoucherWord`)
    },
    loadSubject() {
        return Ajax.get(`/financeAccountLink/loadSubject`)
    },
}