package com.flyemu.share.exception;
/**
 * @功能描述: 共用serviceExceptin
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
public class ServiceException extends RuntimeException {

    public ServiceException() {
        super("服务错误~");
    }

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Exception e) {
        super(message, e);
    }
}
