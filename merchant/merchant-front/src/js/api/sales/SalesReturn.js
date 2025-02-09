import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/salesReturn', param)
    },
    list(param) {
        return Ajax.get('/salesReturn', param)
    },
    remove(id) {
        return Ajax.delete('/salesReturn/' + id);
    },
    select(param) {
        return Ajax.get('/salesReturn/select', param)
    },
    getInfo(id) {
        return Ajax.get('/salesReturn/getInfo/' + id);
    },
    batchAudit(param) {
        return Ajax.put('/salesReturn/batchAudit', param)
    },
    audit(param) {
        return Ajax.put('/salesReturn/audit', param)
    },
}
