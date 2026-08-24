import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.salesReservation?.id ? 'put' : 'post']('/salesReservation', param)
    },
    list(param) {
        return Ajax.get('/salesReservation', param)
    },
    queryTotal(param) {
        return Ajax.get('/salesReservation/total', param)
    },
    delete(id) {
        return Ajax.delete('/salesReservation/' + id);
    },
    load(id) {
        return Ajax.get('/salesReservation/load/' + id);
    },
    approved(ids, state) {
        return Ajax.post('/salesReservation/approved/' + state, ids);
    },
    toPurchase(data) {
        return Ajax.post('/salesReservation/toPurchase', data)
    },
    toSalesOrder(data) {
        return Ajax.post('/salesReservation/toSalesOrder', data)
    },
    transferToPurchase(id, supplierId) {
        return Ajax.post(`/salesReservation/transferToPurchase/${id}`, null, {params: {supplierId}})
    },
    transferToSalesOrder(id) {
        return Ajax.post(`/salesReservation/transferToSalesOrder/${id}`)
    },
    exportToExcel(params) {
        return Ajax.get('/salesReservation/exportToExcel', params, {responseType: 'blob'})
    },
}
