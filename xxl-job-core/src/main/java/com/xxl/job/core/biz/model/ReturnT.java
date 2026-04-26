package com.xxl.job.core.biz.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * common return
 * @author xuxueli 2015-12-4 16:32:31
 * @param <T>
 */
@Setter
@Getter
public class ReturnT<T> implements Serializable {
    private static final long serialVersionUID = 42L;

    public static final int SUCCESS_CODE = 200;
    public static final int FAIL_CODE = 500;

    public static final ReturnT<String> SUCCESS = new ReturnT<>(null);
    public static final ReturnT<String> FAIL = new ReturnT<>(FAIL_CODE, null);

    private int code;
    private String msg;
    private T content;

    public ReturnT() {}

    public ReturnT(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ReturnT(T content) {
        this.code = SUCCESS_CODE;
        this.content = content;
    }

    public ReturnT(int code, String msg, T content) {
        this.content = content;
        this.code = code;
        this.msg = msg;
    }

    public static <T> ReturnT<T> ofFail(String msg) {
        return new ReturnT<>(FAIL_CODE, msg, null);
    }

    @Override
    public String toString() {
        return "ReturnT [code=" + code + ", msg=" + msg + ", content=" + content + "]";
    }
}
