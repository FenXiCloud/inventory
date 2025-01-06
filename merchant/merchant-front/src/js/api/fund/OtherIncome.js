import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/otherIncome', param)
    },
    list(param) {
        return Ajax.get('/otherIncome', param)
    },
    remove(id) {
        return Ajax.delete('/otherIncome/' + id);
    },
    select(param) {
        return Ajax.get('/otherIncome/select', param)
    }
}
