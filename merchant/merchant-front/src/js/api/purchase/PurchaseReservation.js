import Ajax from '@common/Request'

export default {

    list(data) {
        return Ajax.get('/purchaseReservation', data)
    },

    save(data) {
        return Ajax.post('/purchaseReservation', data)
    },

    update(data) {
        return Ajax.put('/purchaseReservation', data)
    },

    delete(purchaseReservationId) {
        return Ajax.delete(`/purchaseReservation/${purchaseReservationId}`)
    },

    load(orderId) {
        return Ajax.get(`/purchaseReservation/load/${orderId}`)
    },

    queryTotal(data) {
        return Ajax.get('/purchaseReservation/total', data)
    },

    listToPurchaseOrder(data) {
        return Ajax.get('/purchaseReservation/toPurchaseOrder', data)
    },

    toPurchaseOrder(data) {
        return Ajax.post('/purchaseReservation/toPurchaseOrder', data)
    },

    toPurchaseInbound(data) {
        return Ajax.post('/purchaseReservation/toPurchaseInbound', data)
    },

    transferToPurchaseInbound(id, warehouseId) {
        return Ajax.post(`/purchaseReservation/transferToPurchaseInbound/${id}`, null, {params: {warehouseId}})
    },

    approved(data, state) {
        return Ajax.post(`/purchaseReservation/approved/${state}`, data)
    }

}
