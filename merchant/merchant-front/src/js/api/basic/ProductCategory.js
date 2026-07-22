import Ajax from "@common/Request";

export default {
	save(param) {
        return Ajax[param.id ? 'put' : 'post']('/productCategory', param)
	},
	list(param) {
        return Ajax.get('/productCategory', param)
	},
	remove(id) {
        return Ajax.delete('/productCategory/' + id);
	},
	select(param) {
        return Ajax.get('/productCategory/select', param)
	},
}
