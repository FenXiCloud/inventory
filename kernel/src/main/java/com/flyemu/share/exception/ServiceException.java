package com.flyemu.share.exception;
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
