import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/priceRecord', param)
    },
    list(param) {
        return Ajax.get('/priceRecord', param)
    },
    remove(id) {
        return Ajax.delete('/priceRecord/' + id);
    },
    select(param) {
        return Ajax.get('/priceRecord/select', param)
    }
}
