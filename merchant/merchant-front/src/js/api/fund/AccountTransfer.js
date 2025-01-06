import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/accountTransfer', param)
    },
    list(param) {
        return Ajax.get('/accountTransfer', param)
    },
    remove(id) {
        return Ajax.delete('/accountTransfer/' + id);
    },
    select(param) {
        return Ajax.get('/accountTransfer/select', param)
    }
}
