import Ajax from "@common/Request";

export default {
    list(param) {
        return Ajax.get('/warehouseLocation', param);
    },
    listAll(param) {
        return Ajax.get('/warehouseLocation/list', param);
    },
    listByWarehouse(warehouseId) {
        return Ajax.get('/warehouseLocation/listByWarehouse', {warehouseId});
    },
    listByType(type) {
        return Ajax.get('/warehouseLocation/listByType', {type});
    },
    listByWarehouseAndType(warehouseId, type) {
        return Ajax.get('/warehouseLocation/listByWarehouseAndType', {warehouseId, type});
    },
    getById(id) {
        return Ajax.get('/warehouseLocation/' + id);
    },
    save(param) {
        return Ajax.post('/warehouseLocation', param);
    },
    update(param) {
        return Ajax.put('/warehouseLocation', param);
    },
    delete(id) {
        return Ajax.delete('/warehouseLocation/' + id);
    },
    batchDelete(ids) {
        return Ajax.put('/warehouseLocation/batchDelete', ids);
    }
};
