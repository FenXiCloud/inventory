/**
 * AI助手API
 */
import Ajax from '@common/Request';

export default {
    /**
     * 发送消息给AI助手
     * @param {Object} params - { sessionId, message }
     */
    chat(params) {
        return Ajax.post('/ai/assistant/chat', params);
    },

    /**
     * 查询客户
     * @param {String} keyword - 搜索关键词
     * @param {Number} limit - 返回数量限制
     */
    searchCustomers(keyword, limit = 10) {
        return Ajax.get('/ai/assistant/customers', { keyword, limit });
    },

    /**
     * 查询商品
     * @param {String} keyword - 搜索关键词
     * @param {Number} limit - 返回数量限制
     */
    searchProducts(keyword, limit = 10) {
        return Ajax.get('/ai/assistant/products', { keyword, limit });
    },

    /**
     * 获取仓库列表
     */
    getWarehouses() {
        return Ajax.get('/ai/assistant/warehouses');
    },

    /**
     * 清除会话上下文
     * @param {String} sessionId - 会话ID
     */
    clearContext(sessionId) {
        return Ajax.post('/ai/assistant/clear-context', { sessionId });
    }
};
