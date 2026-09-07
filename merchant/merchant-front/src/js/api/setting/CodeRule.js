import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/codeRule', param)
    },
    list(param) {
        return Ajax.get('/codeRule', param)
    },
    remove(id) {
        return Ajax.delete('/codeRule/' + id);
    },
    select(param) {
        return Ajax.get('/codeRule/select', param)
    },
    /* 编码效果预览（服务端只读计算，不消耗流水号） */
    preview(param) {
        return Ajax.post('/codeRule/preview', param)
    }
}
