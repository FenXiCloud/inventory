import Ajax from "@common/Request";

export default {
    list(param) {
        return Ajax.get('/locationTransfer', param);
    },
    getById(id) {
        return Ajax.get('/locationTransfer/' + id);
    },
    save(param) {
        return Ajax.post('/locationTransfer', param);
    },
    approve(id) {
        return Ajax.put('/locationTransfer/approve/' + id);
    },
    delete(id) {
        return Ajax.delete('/locationTransfer/' + id);
    }
};
