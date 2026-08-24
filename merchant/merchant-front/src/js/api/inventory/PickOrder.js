import Ajax from "@common/Request";

export default {
    list(param) {
        return Ajax.get('/pickOrder', param);
    },
    getById(id) {
        return Ajax.get('/pickOrder/' + id);
    },
    generate(salesOutboundId) {
        return Ajax.post('/pickOrder/generate/' + salesOutboundId);
    },
    updateStatus(id, status) {
        return Ajax.put('/pickOrder/status/' + id, null, {params: {status}});
    },
    updateActualQuantity(itemId, actualQuantity) {
        return Ajax.put('/pickOrder/item/' + itemId, null, {params: {actualQuantity}});
    },
    delete(id) {
        return Ajax.delete('/pickOrder/' + id);
    }
};
