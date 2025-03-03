import Ajax from "@common/Request";

export default {
    loadAccountSets(id, param) {
        return Ajax.get('/relationCw/loadAccountSets/' + id, param)
    },
    save(param) {
        return Ajax.post('/relationCw', param)
    },
    load() {
        return Ajax.get('/relationCw')
    }
}