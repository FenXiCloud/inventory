import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.salesOutbound?.id ? 'put' : 'post']('/salesOutbound', param)
    },
    list(param) {
        return Ajax.get('/salesOutbound', param)
    },
    total(param) {
        return Ajax.get('/salesOutbound/total', param)
    },
    remove(id) {
        return Ajax.delete('/salesOutbound/' + id);
    },
    select(param) {
        return Ajax.get('/salesOutbound/select', param)
    },
    load(id) {
        return Ajax.get('/salesOutbound/load/' + id);
    },
    prefillInvoice(id) {
        return Ajax.get('/salesOutbound/prefillInvoice/' + id);
    },
    approved(state, ids) {
        return Ajax.post('/salesOutbound/approved/' + state, ids);
    },
    importData(formData) {
        return Ajax.post('/salesOutbound/importData', formData, {'Content-Type': 'multipart/form-data', repeatable: true});
    },
    exportToExcel(params) {
        return Ajax.get('/salesOutbound/exportToExcel', params, {responseType: 'blob'})
    },
}
