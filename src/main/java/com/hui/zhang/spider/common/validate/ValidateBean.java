package com.hui.zhang.spider.common.validate;

import com.edb01.common.util.JsonEncoder;

import java.io.Serializable;

/**
 * ${DESCRIPTION}
 *
 * @author:jiangshun@centaur.cn
 * @create 2017-12-26 11:40
 **/
public class ValidateBean implements Serializable {
    private static final long serialVersionUID = -8505586483570518029L;
    public static ValidateBean INSTRANGE = new ValidateBean();
    private  String code;
    private  String msg;
    public  String build(String code,String msg){
        return JsonEncoder.DEFAULT.encode(new ValidateBean(code,msg));
    }
    public ValidateBean(String code,String mag){
        this.code = code;
        this.msg = mag;
    }
    public ValidateBean(){
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
