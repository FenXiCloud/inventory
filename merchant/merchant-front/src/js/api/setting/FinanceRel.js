import Ajax from "@common/Request";

export default {
    list() {
        return Ajax.get('/financeRel')
    },
    save(param) {
        return Ajax.post('/financeRel', param)
    }

}