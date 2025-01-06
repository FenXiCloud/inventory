import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/pricingPolicy', param)
    },
    list(param) {
        return Ajax.get('/pricingPolicy', param)
    },
    remove(id) {
        return Ajax.delete('/pricingPolicy/' + id);
    },
    select(param) {
        return Ajax.get('/pricingPolicy/select', param)
    }
}
