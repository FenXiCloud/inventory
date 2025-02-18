import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/salesReport', param)
    },
    salesItem(param) {
        return Ajax.get('/salesReport/salesItem', param)
    },
    salesSummary(param) {
        return Ajax.get('/salesReport/salesSummary', param)
    },
    remove(id) {
        return Ajax.delete('/salesReport/' + id);
    },
    select(param) {
        return Ajax.get('/salesReport/select', param)
    },
    getInfo(id) {
        return Ajax.get('/salesReport/getInfo/' + id);
    },
    batchAudit(param) {
        return Ajax.put('/salesReport/batchAudit', param)
    },
    audit(param) {
        return Ajax.put('/salesReport/audit', param)
    },
}
