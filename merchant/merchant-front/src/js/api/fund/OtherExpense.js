import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/otherExpense', param)
    },
    list(param) {
        return Ajax.get('/otherExpense', param)
    },
    remove(id) {
        return Ajax.delete('/otherExpense/' + id);
    },
    select(param) {
        return Ajax.get('/otherExpense/select', param)
    }
}
