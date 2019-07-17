package com.hui.zhang.spider.common.exceptions;

/**
 * ${DESCRIPTION}
 *
 * @author:jiangshun@centaur.cn
 * @create 2017-12-26 13:55
 **/
public class RpcException extends RuntimeException {
    private final String errorCode;

    public RpcException() {
        super();
        this.errorCode = "10000"; // 服务器异常
    }


    public RpcException(String message) {
        this("10000", message);
    }

    public RpcException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RpcException(Throwable cause) {
        super(cause);
        this.errorCode = "10000";
    }

    public RpcException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
