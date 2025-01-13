/**
 * @功能描述: 客户交易流水
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import Ajax from "@common/Request";

export default {
    save(param) {
        return Ajax[param.id ? 'put' : 'post']('/customerFlow', param)
    },
    list(param) {
        return Ajax.get('/customerFlow', param)
    },
    remove(customerFlowId) {
        return Ajax.delete('/customerFlow/' + customerFlowId);
    },
    select(param) {
        return Ajax.get('/customerFlow/select', param)
    },
}
