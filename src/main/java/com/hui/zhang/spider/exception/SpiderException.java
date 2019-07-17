package com.hui.zhang.spider.exception;

/**
 * Created by zuti on 2018/3/1.
 * email zuti@centaur.cn
 */
public class SpiderException extends RuntimeException{

	private static final String MSG = "spider rpc exception ";

	public SpiderException() {

	}

	public SpiderException(String message) {
		super(MSG + message);
	}

	public SpiderException(String message, Throwable cause) {
		super(MSG + message, cause);
	}

	public SpiderException(Throwable cause) {
		super(cause);
	}

	public SpiderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(MSG + message, cause, enableSuppression, writableStackTrace);
	}
}
